// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;


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
 * @param byproduct what finishing this change drives out of the piece (slag from a bloom); empty for most entries. The caller that
 *                  holds the workpiece decides where it goes: a machine throws it clear, a hand at the anvil is handed it.
 * @param operation how force must be delivered for this entry to apply -- see {@link Operation}.
 *                  Optional and defaults to {@code BLOW} so every existing hammer entry needed no
 *                  change. This is the second key {@link DeformationTable#find} matches on, and
 *                  the reason it exists at all: the same input item can now have two entries (one
 *                  a hammer reaches, one a draw plate reaches) without either shadowing the other.
 */
public record Deformation(Ingredient input, int work, St hardness, ItemStack result,
                          Tu minTemperature, Tu maxTemperature, Operation operation, ItemStack byproduct) {

    /** A change that drives nothing out of the piece: every step but a bloom's. */
    public Deformation(Ingredient input, int work, St hardness, ItemStack result, Tu minTemperature, Tu maxTemperature, Operation operation) {
        this(input, work, hardness, result, minTemperature, maxTemperature, operation, ItemStack.EMPTY);
    }

    /**
     * {@code StreamCodec.composite} tops out at six fields ({@code Function6}), and this record
     * has seven. Rather than fork a second, hand-rolled composite for exactly one extra field,
     * the temperature band travels as one {@code Tu[2]} slot.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, Tu[]> TEMPERATURE_RANGE_STREAM_CODEC = StreamCodec.composite(
            Units.streamCodec(Tu::new), range -> range[0],
            Units.streamCodec(Tu::new), range -> range[1],
            (min, max) -> new Tu[] { min, max });

    /**
     * For sending the table to the client, which needs it only to <em>print</em> it.
     *
     * <h3>Why the client is told at all</h3>
     * Philosophy 8: what a process requires is published data and costs nothing. The recipe
     * browser is allowed to state {@code 14 Fu} exactly, on day one, with no instrument owned --
     * so the figures have to get across. Nothing on the client ever computes with them.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Deformation> STREAM_CODEC = new StreamCodec<>() {
        /** The first six fields as before; {@code composite} stops there, so the byproduct is written after them by hand. */
        private final StreamCodec<RegistryFriendlyByteBuf, Deformation> head = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Deformation::input,
                ByteBufCodecs.VAR_INT, Deformation::work,
                Units.streamCodec(St::new), Deformation::hardness,
                ItemStack.STREAM_CODEC, Deformation::result,
                TEMPERATURE_RANGE_STREAM_CODEC, d -> new Tu[] { d.minTemperature(), d.maxTemperature() },
                Operation.STREAM_CODEC.cast(), Deformation::operation,
                (input, work, hardness, result, range, operation) ->
                        new Deformation(input, work, hardness, result, range[0], range[1], operation));

        @Override
        public Deformation decode(RegistryFriendlyByteBuf buffer) {
            Deformation d = head.decode(buffer);
            return new Deformation(d.input, d.work, d.hardness, d.result, d.minTemperature, d.maxTemperature, d.operation,
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Deformation d) {
            head.encode(buffer, d);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, d.byproduct);
        }
    };

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

    /**
     * Fu this material absorbs from one blow of the given force, or 0 if the blow is too weak.
     * Always at least 1 once the threshold is met, so a barely-sufficient blow still progresses
     * rather than hammering forever at zero.
     */
    public int workFrom(St blowForce) {
        return landed(blowForce, hardness);
    }

    /** The one rule behind {@link #workFrom}: what a blow of {@code blowForce} puts into a material of {@code hardness}, for a hand forge to read without an entry. */
    public static int landed(St blowForce, St hardness) {
        if (blowForce.value() < hardness.value())
            return 0;
        return Math.max(1, Math.round(blowForce.value() / hardness.value()));
    }
}
