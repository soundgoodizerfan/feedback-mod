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
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a material becomes when enough of it is cut away -- the removal family's own version of
 * {@link Deformation}, and deliberately the same shape: an input, a total {@code Fu}, a hardness,
 * a result, and now an {@link Operation}.
 *
 * <h2>{@code Operation} was missing, and that was a real bug, not a simplification</h2>
 * This record originally had no {@code Operation} field at all, on the theory that turning,
 * drilling and grinding are "one physical primitive wearing three costumes" since they share one
 * gate. That theory does not survive contact with {@link Deformation}, which has the identical
 * hardness-gated shape across {@code BLOW}/{@code DRAW}/{@code ROLL} and still needed the field --
 * a shared gate was never a shared action. Without it, nothing stopped a Drill Press from
 * "turning" a rod meant for the Lathe, and a recipe browser had no way to say which of the three
 * machines a card even needed. See {@link Operation}'s own class doc for the fuller account.
 *
 * <h2>The gate is a second hardness, not a division</h2>
 * {@link Deformation}'s hardness does two jobs: floor and divisor. Removal's hardness keeps only
 * the floor, and moves it up a level -- {@link #cutFrom} takes the <em>tool's</em> hardness
 * (a physical property of the bit, not of the drive) and refuses to deliver anything at all
 * unless the tool's rating exceeds this material's. That is the identical shape to "3 St under
 * hardness 15 lands nothing" in {@link Deformation}, just checked against a tool property instead
 * of a blow property. Once the gate is open, the arithmetic is the same division
 * {@link Deformation#workFrom} already uses -- drive force over hardness -- so a harder material
 * still eats more of the same cutting force before anything registers.
 *
 * @param input     what is being cut. An Ingredient, same reasoning as {@link Deformation#input}.
 * @param work      cumulative Fu of removal required to reach {@link #result}.
 * @param hardness  how stubborn this material is against a cutting edge, in St -- the tool must
 *                  clear it before any Fu accumulates at all (see {@link #cutFrom}).
 * @param result    what remains once the removal is complete.
 * @param operation which of {@code TURN}/{@code DRILL}/{@code GRIND} this entry is for.
 */
public record Removal(Ingredient input, int work, St hardness, ItemStack result, Operation operation) {

    public static final Codec<Removal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(Removal::input),
            Codec.INT.fieldOf("work").forGetter(Removal::work),
            Units.codec(St::new).optionalFieldOf("hardness", new St(1f)).forGetter(Removal::hardness),
            ItemStack.CODEC.fieldOf("result").forGetter(Removal::result),
            Codec.STRING.xmap(s -> Operation.valueOf(s.toUpperCase()), Operation::name)
                    .fieldOf("operation").forGetter(Removal::operation)
    ).apply(instance, Removal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Removal> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, Removal::input,
            ByteBufCodecs.VAR_INT, Removal::work,
            Units.streamCodec(St::new), Removal::hardness,
            ItemStack.STREAM_CODEC, Removal::result,
            ByteBufCodecs.idMapper(i -> Operation.values()[i], Operation::ordinal), Removal::operation,
            Removal::new);

    /** Whether a tool of this hardness can cut this material at all -- the hard gate (§7). */
    public boolean canBeCutBy(St toolHardness) {
        return toolHardness.value() > hardness.value();
    }

    /**
     * Fu this material yields to one tick of {@code driveForce}, once {@link #canBeCutBy} has
     * already been checked. Same arithmetic as {@link Deformation#workFrom} -- always at least 1
     * once the gate is open, so a barely-sufficient tool still progresses rather than cutting
     * forever at zero.
     */
    public int cutFrom(St driveForce) {
        return Math.max(1, Math.round(driveForce.value() / hardness.value()));
    }
}
