// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

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
        /** The hafted plan's third face: its dig speed, edge hardness and edge toughness. */
        DIG_SPEED_THIRD,
        HARDNESS_THIRD,
        EDGE_TOUGHNESS_THIRD,
        /** The hafted plan's second and third faces' own durability, as {@link #DURABILITY} is the front face's. */
        DURABILITY_B,
        DURABILITY_C,
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
        STRIKE_ST,
        // --- a dish's (family DISH), the baked meal ---
        /** Hunger points the parts give, capped by the container. */
        HUNGER,
        SATURATION,
        /** Live-organism load of the worst part: any raw part taints the whole. */
        PATHOGEN,
        /** Stable-toxin concentration over the dish's mixture. */
        TOXIN,
        /** 0 to 1: how burnt the worst part is. */
        CHARRED,
        FAT,
        FIBRE,
        /** Litres the container holds, which caps the meal. */
        LITRES,
        // --- a glove's (family GLOVE) ---
        /** Damage added to a barehanded swing, before wear. */
        FIST,
        /** Share of attack speed a metacarpal plate's weight takes off the wearer's swing, 0 to 1. */
        SWING_DRAG,
        // --- a dish's profile as it is judged (DishProfile): live state, then composition. Baked to zero; read off the stack when a dish is judged or tasted. ---
        /** 0 to 1: how cooked through the dish is now. */
        DISH_COOK,
        /** The dish's water now. */
        DISH_MOISTURE,
        /** 0 to 1: salt in the blend. */
        DISH_SALT,
        /** 0 to 1: smoke coat. */
        DISH_SMOKE,
        /** 0 to 1: how fresh it is now (one minus rot). */
        DISH_FRESH,
        /** {@code Tu}: how warm the dish is now. */
        DISH_WARMTH,
        /** Volume share of animal parts (meat). */
        SHARE_MEAT,
        /** Volume share of seafood parts. */
        SHARE_SEAFOOD,
        /** Volume share of vegetable, fruit and fungus parts. */
        SHARE_PLANT,
        /** Volume share of grain and dough parts. */
        SHARE_STARCH,
        /** Share of the parts that vote for each cuisine ({@link io.github.soundgoodizerfan.feedback.process.Material.Trait}). */
        CUISINE_ITALIAN,
        CUISINE_EAST_ASIAN,
        CUISINE_MEXICAN,
        CUISINE_EASTERN_EUROPEAN,
        // --- a backpack's (family BACKPACK) ---
        /** {@code Mu}: the load the strap carries before the wearer is slowed; the strap's strength times the wear left. */
        CARRY,
        /** Item slots of the bag, fixed by the shell's shape. */
        SLOTS,
        /** Mounting points of the shell and frame left once the fittings are on; a fitting that does not fit refuses the bake. */
        POINTS,
        /** Extra typed slots the fittings add beside the fixed ones (holster, bottle loops, bedroll lashing, bandolier). */
        STORES,
        /** A bedroll's comfort: how soft its fill and sheet are; the share of health a night in it gives back. */
        COMFORT,
        // --- a powered suit piece's (family ARMOUR), zero where no servo or fitting is on ---
        /** Amps the servo winding carries: the winding conductor's ampacity times its cells of wire. The piece's muscle. */
        SUIT_DRIVE,
        /** 0 to 1: the share of the servo's drain that leaves as waste heat (the winding conductor's drop over its length). */
        SUIT_LOSS,
        /** The step factor of the gauge lens on the piece (0.65 clearest, 1 none): how finely the suit's gauges read. 0 when there is no gauge. */
        SUIT_GAUGE,
        /** Cells of fibre in the sensor head; 0 without one. Longer bundles see further. */
        SUIT_SENSE,
        /** 1 when the piece has a battery bay (the plate that holds it), else 0. */
        SUIT_BAY,
        /** The ids of the suit upgrades the piece's module carries (see {@code suit/SuitModules}), one figure per attachment, 0 for none. */
        SUIT_ATT_1, SUIT_ATT_2, SUIT_ATT_3, SUIT_ATT_4,
        /** How strongly each of those upgrades works, from the material its attachment is cut in (0.25 to 1.5), in the same order. */
        SUIT_POW_1, SUIT_POW_2, SUIT_POW_3, SUIT_POW_4,
        /** A suit module's draw multiplier from its wiring: heavy conductor runs it cooler (below 1), thin wire wastes more (above 1). 0 where no module is held. */
        SUIT_WIRING,
        // --- an armour piece's, from the plate's axes ---
        /** 0 to {@code PIERCE_RESIST_MAX}: the share of pierce (projectile) damage the piece turns aside; worn pieces add by slot weight. */
        PIERCE_RESIST,
        /** The plate material's thermal conductivity times the plate's volume: how readily it carries fire's heat to the wearer. 0 for cloth and leather. */
        FIRE_CONDUCT,
        // --- a spring gun's (family AIR_GUN) ---
        /** Joules the cocked spring stores, before any set. */
        SPRING_ENERGY,
        /** 0 to 1: the share of the spring's energy that reaches the pellet, from the barrel's length and bore. */
        AIR_EFFICIENCY,
        // --- an electric gun's (family ELECTRIC_GUN) ---
        /** Joules a shot takes from the battery at full draw. */
        SHOT_ENERGY,
        // --- shears (family SHEARS) ---
        /** The speed multiple of coarse clipping (wool, leaves): the blade pair's snap and the bite of its length. */
        CLIP,
        /** The speed multiple of fine clipping (cobweb, vines): the same snap, held back by a long blade's lack of control. */
        CLIP_FINE,
        // --- a bed's (family BED) ---
        /** A bed's warmth: how well its padding keeps the sleeper's heat in, 0 to 1. */
        WARMTH;

        /** Most attachments one module carries. */
        public static final int ATTACHMENTS = 4;

        /** The attachment figure of attachment {@code i}, 0 to {@link #ATTACHMENTS} - 1. */
        public static Fig att(int i) {
            return values()[SUIT_ATT_1.ordinal() + i];
        }

        /** The power figure of attachment {@code i}. */
        public static Fig pow(int i) {
            return values()[SUIT_POW_1.ordinal() + i];
        }
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
