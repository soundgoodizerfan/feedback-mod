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

/**
 * Every constant the modular bake and its wear read ({@code feedback_modular_items_spec.md}).
 * Kept beside the bake rather than in {@code FTuning} so the figure and the reason it is that
 * figure sit together. All are placeholders (philosophy 19), calibrated so that the canonical
 * vanilla designs come out near vanilla: an iron pickaxe digs near 6 and lasts near 250, a
 * diamond one near 8 and 1560. Nothing here is per material; a material's own figures do the
 * rest.
 */
public final class ModularTuning {

    private ModularTuning() {
    }

    // --- swing: how a wielder moves what they hold -------------------------------------------

    /** Swings per second at an inertia of one kg m^2. */
    public static final float SWING_K = 1.57f;
    /** Swing rate falls with the fourth root of the inertia: strength does not scale with the tool. */
    public static final float SWING_EXPONENT = 0.25f;
    /** A head-heavy tool is tiring to hold out, which slows it as if it were this much heavier per kg m of moment. */
    public static final float BALANCE_K = 0.3f;
    public static final float SWING_MIN = 0.4f;
    public static final float SWING_MAX = 4f;
    /** Share of a handle's mass that rides with the head in a blow. */
    public static final float ROD_STRIKE_SHARE = 0.3f;

    // --- what a blow does ---------------------------------------------------------------------

    public static final float DAMAGE_K = 1.4f;
    public static final float DIG_K = 2f;
    /** Durability per (edge competence x litre of wearing edge). */
    public static final float DURABILITY_K = 96f;
    /** Bite is a log of the edge's competence, {@code sqrt(hardness x strength)}. */
    public static final float BITE_BASE = 0.35f;
    public static final float BITE_SLOPE = 0.22f;
    public static final float BITE_MIN = 0.3f;
    public static final float BITE_MAX = 1.7f;

    // --- the hard gate on what an edge can dig ------------------------------------------------

    /**
     * Hardness an edge needs for the three classes of block vanilla fences with a tier tag. Read
     * against the material's own hardness (the hammer's floor), not a tier name: stone's 30 passes
     * the iron class, which is why a stone head digs gold and diamond ore and chips doing it.
     */
    public static final float STONE_CLASS_HARDNESS = 3f;
    public static final float IRON_CLASS_HARDNESS = 6f;
    public static final float DIAMOND_CLASS_HARDNESS = 40f;

    // --- misuse -------------------------------------------------------------------------------

    /** A block's destroy time times this is the strain it puts on an edge, read against the edge's toughness. */
    public static final float CHIP_STRAIN_PER_HARDNESS = 4f;
    /** Strain this many times the toughness before an edge chips at all. */
    public static final float CHIP_SLACK = 2f;
    public static final int CHIP_MAX = 3;

    // --- the joint ----------------------------------------------------------------------------

    /** Load a joint of strength 1 carries, in kg m (head mass times its distance from the hand). */
    public static final float JOINT_K = 1.2f;
    public static final float WRAPPED_SHARE = 0.8f;
    public static final float PINNED_SHARE = 1.5f;

    // --- mace and shield ----------------------------------------------------------------------

    /** A mace's smash is scaled by its mass over this: a heavy core head on a full haft, in kg. */
    public static final float MACE_REFERENCE_MASS = 24f;
    /** The most damage a shield stops outright is this plus its face's competence. */
    public static final float SHIELD_BASE_CAPACITY = 6f;

    // --- bows ---------------------------------------------------------------------------------

    /** A string holds this many times its own strength before it is the limit; limbs beyond that strain it. */
    public static final float STRING_SHARE = 2f;
    /** The draw strength vanilla's bow is, and the ticks it takes. */
    public static final float DRAW_REFERENCE_STRENGTH = 3f;
    public static final float DRAW_REFERENCE_TICKS = 20f;
    public static final float DRAW_MIN_TICKS = 14f;
    public static final float DRAW_MAX_TICKS = 60f;
    public static final float VELOCITY_EXPONENT = 0.4f;
    public static final float VELOCITY_MIN = 0.6f;
    public static final float VELOCITY_MAX = 1.9f;

    // --- armour -------------------------------------------------------------------------------

    /** Plate stops a share {@code s / (s + this)} of what plate at its best stops, s being its competence. */
    public static final float ARMOUR_HALF_COMPETENCE = 2.87f;
    /** Armour's durability reads the plate's competence plus this, so that leather is soft but not paper. */
    public static final float ARMOUR_DURABILITY_FLOOR = 1.5f;
    /** Share of a piece's points a lining adds, by how good the lining is. */
    public static final float LINING_SHARE = 0.1f;
    /** Plate this saturated gives armour toughness; the toughness runs 0 to 3 over the last fifth. */
    public static final float TOUGHNESS_FROM_SHARE = 0.8f;
    public static final float TOUGHNESS_MAX = 3f;
    /** Density above 8 gives knockback resistance (mass is hard to push), to a cap. */
    public static final float KNOCKBACK_DENSITY_FLOOR = 8f;
    public static final float KNOCKBACK_PER_DENSITY = 0.02f;
    public static final float KNOCKBACK_MAX = 0.1f;
    /** A piece heavier than this many kg slows the wearer, by this share of base speed per kg over. */
    public static final float SLOW_FREE_MASS = 4f;
    public static final float SLOW_PER_KG = 0.003f;
    public static final float SLOW_MAX = 0.1f;

    // --- reach --------------------------------------------------------------------------------

    public static final float REACH_REFERENCE = 0.9f;
    public static final float REACH_PER_METRE = 2f;

    // --- wear in steps ------------------------------------------------------------------------

    /** Fractions worn at which the figures drop a step, and what is left of them after each. */
    public static final float[] WEAR_STEPS = {0.5f, 0.75f, 0.9f};
    public static final float[] WEAR_FACTOR = {1f, 0.9f, 0.75f, 0.5f};

    // --- fallbacks ----------------------------------------------------------------------------

    /** Strength of a material that states neither a yield nor a toughness. */
    public static final float STRENGTH_FALLBACK = 1f;
    /** Enchantability of a material that states none: poor. */
    public static final int ENCHANTABILITY_FALLBACK = 1;
}
