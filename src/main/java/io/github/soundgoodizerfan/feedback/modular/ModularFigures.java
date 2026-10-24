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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Mu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a design makes, baked once by {@link ModularBake} and kept on the stack -- the gun's
 * {@code GunFigures} pattern. Every figure is derived from the parts' materials; none is read from
 * a per-material stat table. The client reads these for the tooltip and nothing else, since the
 * vanilla components the bake also writes ({@code TOOL}, attributes, max damage) are what the game
 * itself reads.
 *
 * <p>A fixed-order list of floats indexed by {@link Fig}, not a record of twenty fields: not every
 * type has every figure, a codec group tops out at sixteen, and a new figure is one enum line.
 *
 * @param step how worn the item was last baked: the figures degrade in steps on the way down
 *             ({@link ModularBake#wearStep}), so a worn tool is visibly worse before it stops.
 */
public record ModularFigures(List<Float> values, int step) {

    /** One figure. Units noted where there is one; the rest are ratios or plain counts. */
    public enum Fig {
        /** {@code Mu}. */
        MASS,
        /** Share of the swing's moment that sits in the head: near 1 is head-heavy. */
        BALANCE,
        /** Swings per second a wielder can manage. */
        SWING,
        /** Joules a blow carries. */
        IMPACT,
        /** How readily the edge cuts: hardness and strength, as the edge is ground. */
        BITE,
        /** Extra interaction range, in blocks. */
        REACH,
        DAMAGE,
        ATTACK_SPEED,
        DIG_SPEED,
        DURABILITY,
        ENCHANTABILITY,
        /** The leverage the head puts on the joint over what the joint can carry. Over 1 it works loose. */
        JOINT,
        /** The edge's hardness, which gates what it can dig. */
        HARDNESS,
        /** The edge's toughness, which decides what it chips on. */
        EDGE_TOUGHNESS,
        ARMOUR,
        ARMOUR_TOUGHNESS,
        KNOCKBACK_RESISTANCE,
        /** Fraction of walking speed the wearer gives up to carry it. */
        SLOW,
        /** The most damage a shield stops outright. */
        CAPACITY,
        /** Ticks to draw. */
        DRAW,
        /** Launch speed over the vanilla bow's. */
        VELOCITY,
        /** 0 to 1: how well it survives water, the wearing part's own and any coat's together. */
        CORROSION,
        /** A twin-headed tool's second head: its dig speed, edge hardness and edge toughness, as the first head's are above. */
        DIG_SPEED_OFF,
        HARDNESS_OFF,
        EDGE_TOUGHNESS_OFF,
        // --- a gun's (family GUN), baked at the reference load; a pick leaves them at zero as a gun leaves the pick's ---
        /** {@code Pu}: what the charge makes in the case. */
        PEAK_PRESSURE,
        /** {@code Pu}: the most the chamber holds. */
        CHAMBER_RATING,
        /** Blocks per tick. */
        MUZZLE_SPEED,
        /** Work delivered to the projectile at the muzzle. */
        MUZZLE_ENERGY,
        /** {@code Mu}: the projectile. */
        BULLET_MASS,
        /** Momentum of projectile and gas: what recoil and kick are made from. */
        IMPULSE,
        /** Milliseconds between shots. */
        CYCLE,
        /** Cone half-angle at the muzzle, degrees; never zero. */
        SPREAD,
        /** Camera kick per shot, degrees. */
        KICK,
        /** {@code GunSpec.Launcher} ordinal: what throws the projectile. */
        LAUNCHER,
        /** How loud a shot is against an open muzzle. */
        REPORT,
        /** Heat a shot leaves in the gun against an open muzzle's. */
        HEAT_FACTOR,
        /** How fast the gun sheds heat against a bare barrel's. */
        SHED_FACTOR,
        /** Peak pressure over the chamber's rating; above 1 the force goes into the gun. */
        OVERPRESSURE,
        // --- a charge's (family CHARGE) ---
        /** Explosion power: the blend's burned potential and the casing's confinement. */
        BLAST,
        /** Fraction of the filler that burns; under the fire line the rest goes up as fire. */
        BURNED,
        /** Items of filler packed. */
        PACKED,
        // --- a machine head's (family HEAD) ---
        /** {@code St}: the most a blow of this head carries, before wear. */
        STRIKE_ST
    }

    public static final Codec<ModularFigures> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.listOf().fieldOf("values").forGetter(ModularFigures::values),
            Codec.INT.optionalFieldOf("step", 0).forGetter(ModularFigures::step)
    ).apply(instance, ModularFigures::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularFigures> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), ModularFigures::values,
            ByteBufCodecs.VAR_INT, ModularFigures::step,
            ModularFigures::new);

    public static ModularFigures of(Map<Fig, Float> figures, int step) {
        List<Float> out = new ArrayList<>();
        for (Fig f : Fig.values())
            out.add(figures.getOrDefault(f, 0f));
        return new ModularFigures(List.copyOf(out), step);
    }

    public float get(Fig fig) {
        return fig.ordinal() < values.size() ? values.get(fig.ordinal()) : 0f;
    }

    public Mu mass() {
        return new Mu(get(Fig.MASS));
    }

    public ModularFigures withStep(int step) {
        return new ModularFigures(values, step);
    }

    public Map<Fig, Float> map() {
        Map<Fig, Float> out = new EnumMap<>(Fig.class);
        for (Fig f : Fig.values())
            out.put(f, get(f));
        return out;
    }
}
