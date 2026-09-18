/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a material becomes when enough mechanical work is beaten into it.
 *
 * <h2>This is a material property, not a recipe</h2>
 * The distinction matters and the philosophy is strict about it. A machine never consults this.
 * The Mechanical Hammer knows only how to deliver {@code Fu} at a given {@code St} to whatever is
 * in front of it; it has no list of things it can make and cannot be asked what it produces.
 * <p>
 * What this table describes is how a <em>material</em> responds to being hit -- the same way a
 * melting point belongs to the metal rather than to the furnace. Entries chain: the output of one
 * is the input of the next, which is the whole of the overrun mechanic. A plate that keeps being
 * struck becomes foil because foil is what a struck plate <em>is</em>, not because the machine
 * decided to make foil.
 *
 * <h2>Hardness does both jobs</h2>
 * There is deliberately no "force required" separate from "work required". Force and work were
 * two numbers describing the same event, and nothing is strong enough to dent steel yet fails to
 * slam copper -- so what a blow accomplishes is not a property of the machine at all. It is what
 * the <em>material</em> does with the force it is handed:
 * <pre>
 *     Fu per blow  =  blow force (St)  /  hardness
 * </pre>
 * One number, doing both jobs. It is the floor below which nothing happens, and the divisor for
 * how much lands above it. Copper's hardness of 1 means a 12 St blow dumps 12 Fu into it; steel's
 * 15 means the same blow does nothing whatsoever.
 * <p>
 * The consequence is the interesting part. A hard blow on a soft material <em>overshoots</em> --
 * three blows take copper to a plate and two more ruin it into foil, while a gentle blow gives
 * ten and seven. So a short crank throw is the power option and a long one is the precision
 * option, and neither is strictly better. A forging press really does wreck copper.
 *
 * @param input     what is being worked. An Ingredient, so tags work and identity need not be
 *                  named -- "any copper ingot" is a property, "this exact item" is not.
 * @param work      cumulative Fu required to complete the change.
 * @param hardness  how stubborn the material is, in St -- the same unit a blow is delivered in,
 *                  since a blow lands or fails by direct comparison against this figure and what
 *                  it delivers is that comparison's quotient (see {@link #workFrom}). Both the
 *                  minimum force that does anything -- philosophy 7's hard gate, a real
 *                  impossibility rather than a slow version -- and how little of a bigger blow
 *                  actually lands.
 * @param result    what it becomes.
 * @param minTemperature  how hot the workpiece must be for a blow to do anything, in Tu. Zero for
 *                  everything cold-workable, which is most things.
 * @param maxTemperature  how hot it may be. Unbounded unless a material says otherwise.
 * @param operation how force must be delivered for this entry to apply -- see {@link Operation}.
 *                  Optional and defaults to {@code BLOW} so every existing hammer entry needed no
 *                  change. This is the second key {@link DeformationTable#find} matches on, and
 *                  the reason it exists at all: the same input item can now have two entries (one
 *                  a hammer reaches, one a draw plate reaches) without either shadowing the other.
 */
public record Deformation(Ingredient input, int work, St hardness, ItemStack result,
                          Tu minTemperature, Tu maxTemperature, Operation operation) {

    public static final Codec<Deformation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(Deformation::input),
            Codec.INT.fieldOf("work").forGetter(Deformation::work),
            Units.codec(St::new).optionalFieldOf("hardness", new St(1f)).forGetter(Deformation::hardness),
            ItemStack.CODEC.fieldOf("result").forGetter(Deformation::result),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(0f)).forGetter(Deformation::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Deformation::maxTemperature),
            Codec.STRING.xmap(s -> Operation.valueOf(s.toUpperCase()), Operation::name)
                    .optionalFieldOf("operation", Operation.BLOW).forGetter(Deformation::operation)
    ).apply(instance, Deformation::new));

    /**
     * For sending the table to the client, which needs it only to <em>print</em> it.
     *
     * <h3>Why the client is told at all</h3>
     * Philosophy 8: what a process requires is published data and costs nothing. The recipe
     * browser is allowed to state {@code 14 Fu} exactly, on day one, with no instrument owned --
     * so the figures have to get across. Nothing on the client ever computes with them.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Deformation> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, Deformation::input,
            ByteBufCodecs.VAR_INT, Deformation::work,
            Units.streamCodec(St::new), Deformation::hardness,
            ItemStack.STREAM_CODEC, Deformation::result,
            Units.streamCodec(Tu::new), Deformation::minTemperature,
            Units.streamCodec(Tu::new), Deformation::maxTemperature,
            ByteBufCodecs.idMapper(i -> Operation.values()[i], Operation::ordinal), Deformation::operation,
            Deformation::new);

    /**
     * Fu this material absorbs from one blow of the given force, or 0 if the blow is too weak.
     * Always at least 1 once the threshold is met, so a barely-sufficient blow still progresses
     * rather than hammering forever at zero.
     */
    /**
     * Whether a workpiece at this temperature can be worked at all.
     *
     * <h3>Why hot working is a window and not a bonus</h3>
     * Philosophy 7 is strict that a hard gate must be a genuine physical impossibility rather than
     * a slow version of the process, and this is the second one in the mod after hardness. Steel
     * below 900 Tu does not deform more slowly; it does not deform. The consequence is the whole
     * of the slice's convergence: the workpiece carries its own heat, the heat is running out, and
     * so the distance between the crucible and the anvil became a decision without anybody
     * installing one.
     */
    public boolean worksAt(Tu tu) {
        return tu.value() >= minTemperature.value() && tu.value() <= maxTemperature.value();
    }

    /** Whether this material cares about temperature at all. Most do not. */
    public boolean isHotWorking() {
        return minTemperature.value() > 0 || maxTemperature.value() < Float.MAX_VALUE;
    }

    public int workFrom(St blowForce) {
        if (blowForce.value() < hardness.value())
            return 0;
        return Math.max(1, Math.round(blowForce.value() / hardness.value()));
    }
}
