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
package io.github.soundgoodizerfan.feedback.core.unit;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The machinery that makes a unit cost one small file instead of a pass over the codebase.
 *
 * <h2>Why this exists at all</h2>
 * The standing argument for paying a fixed cost early (see {@code CLAUDE.md}): a high fixed cost
 * paid once beats no fixed cost and a medium variable cost paid per feature, and the crossover
 * arrives earlier than it feels like it will. Four units were enough to make that true here. The
 * mod already knows it wants stored chemical energy, electrical energy in {@code Eu}, pressure in
 * {@code Pu}, amount of substance in {@code Qu} and possibly radiation, and every one of those
 * would otherwise arrive with its own hand-written codec, its own wire format and its own
 * formatting call. Those three things are the actual cost of a unit. The record declaration is
 * the cheap part.
 *
 * <h2>Adding a unit -- the whole checklist</h2>
 * <ol>
 *   <li>Write the record. One component, named for the dimension; implement {@link Unit#raw()}
 *       and {@link Unit#unitKey()} in a line each. Copy {@link Su} for a base unit or
 *       {@link Drag} for a compound. Base units name their component {@code value} because the
 *       type name is already the dimension; compounds name theirs for the dimension, because
 *       {@code ThermalMass} and {@code Conductance} are both "a float" until the accessor says
 *       otherwise.</li>
 *   <li>Add its symbol to {@code en_us.json} under {@code feedback.unit.<name>}.</li>
 *   <li>If it appears in a datapack table, take a {@link #codec} or {@link #intCodec} and a
 *       {@link #streamCodec} or {@link #intStreamCodec} here. Do not hand-roll either.</li>
 *   <li>If a player may read it as a figure, add a {@code Quantity} and let
 *       {@code Readout.figure} format it. Do not write a bespoke formatter -- that is how §8's
 *       wording rule drifts.</li>
 * </ol>
 * Nothing has to be registered and nothing has to be edited here, which is the point:
 * {@link Unit} is deliberately <em>not</em> a sealed interface. Sealing would buy exhaustive
 * switches, which nothing wants, and charge a second file edit for every new unit, which is
 * exactly the per-unit cost this class exists to remove.
 *
 * <h2>What is deliberately absent</h2>
 * No arithmetic, no conversion, no dimensional algebra. See the package javadoc: the types guard
 * the plumbing, and the physics is written in plain floats unwrapped at the point of use. A
 * conversion table would also be actively wrong for this mod -- {@code Su} does not convert to
 * {@code Eu}, on purpose, and philosophy 4 makes that a design rule rather than an omission.
 */
public final class Units {

    private Units() {
    }

    /** How a float-backed unit is built from its number. */
    @FunctionalInterface
    public interface FloatFactory<U extends Unit> {
        U of(float value);
    }

    /** How an integer-backed unit is built from its number. */
    @FunctionalInterface
    public interface IntFactory<U extends Unit> {
        U of(int value);
    }

    /** How an integer-backed unit gives its number back without going through a float. */
    @FunctionalInterface
    public interface ToInt<U extends Unit> {
        int of(U unit);
    }

    // --- datapack -------------------------------------------------------------------------

    /**
     * A datapack codec for a float-backed unit. The JSON stays a bare number, so retyping a table
     * field never changes a pack that was written against it.
     */
    public static <U extends Unit> Codec<U> codec(FloatFactory<U> factory) {
        return Codec.FLOAT.xmap(factory::of, Unit::raw);
    }

    /**
     * A datapack codec for an integer-backed unit. Separate from {@link #codec} rather than
     * rounding a float, because an integer unit is integral in the model and a table that silently
     * accepted {@code 13.5} would be lying about it.
     */
    public static <U extends Unit> Codec<U> intCodec(IntFactory<U> factory, ToInt<U> value) {
        return Codec.INT.xmap(factory::of, value::of);
    }

    // --- wire -----------------------------------------------------------------------------

    /** The network form of a float-backed unit. Four bytes, same as the float it wraps. */
    public static <U extends Unit> StreamCodec<ByteBuf, U> streamCodec(FloatFactory<U> factory) {
        return ByteBufCodecs.FLOAT.map(factory::of, Unit::raw);
    }

    /** The network form of an integer-backed unit, as a VAR_INT. */
    public static <U extends Unit> StreamCodec<ByteBuf, U> intStreamCodec(
            IntFactory<U> factory, ToInt<U> value) {
        return ByteBufCodecs.VAR_INT.map(factory::of, value::of);
    }

    // --- display --------------------------------------------------------------------------

    /**
     * {@link Mu} per unit of Sable's mass ("kpg"). Sable weighs a normal full block at 1.0, and a
     * block is 1000 mB; at water's density (exactly 1 Mu/mB) that is 1000 Mu, so 1 kpg is a tonne.
     * Stone, which Sable calls heavy at 2.0, then reads 2000 Mu -- near real stone's ~2600 -- so the
     * anchor holds up. Every Mu that enters Sable goes through {@link #kpg}.
     */
    public static final float MU_PER_KPG = 1000f;

    /**
     * {@link Su} per unit of Sable's torque (kpg block^2/s^2). A block is a metre and a kpg a tonne,
     * so Sable's torque is a kilonewton-metre; read as newton-metres, as the engine's 100 Su per
     * litre already reads it (a real engine makes about that), 1 of Sable's is 1000 Su. Every
     * torque crossing between a body and a shaft goes through this, both ways.
     */
    public static final float SU_PER_KPG_TORQUE = 1000f;

    /** {@code mass} in Sable's own unit, for a figure handed to Sable (a box's mass). */
    public static double kpg(Mu mass) {
        return mass.value() / MU_PER_KPG;
    }

    /**
     * A figure with its symbol: {@code 1450 Tu}, {@code 32 RPM}.
     *
     * <h3>This is not the whole display rule</h3>
     * It formats a number that the caller has <em>already decided</em> the player is allowed to
     * see. Whether they are allowed, and at what resolution, is {@code Readout}'s question and
     * stays there -- §8's rule is about which figures exist at all, and a formatter that also
     * answered that would put half the rule in the wrong file.
     */
    public static Component figure(Unit unit) {
        return Component.translatable(unit.unitKey(), format(unit.raw()));
    }

    /**
     * A number, trimmed of a pointless trailing {@code .0}.
     * <p>
     * Whole figures are the common case -- most tuning values are round and most readings are
     * quantised by an instrument before they arrive here -- and {@code 1450 Tu} reads as a
     * measurement in a way {@code 1450.0 Tu} does not.
     */
    public static String format(float value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format("%.1f", value);
    }
}
