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
 * Every constant the modular bake and its wear read ({@code docs/feedback_modular_items_spec.md}).
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
    public static final float SWING_K = 1.6f;
    /**
     * Swing rate falls with the square root of the inertia (a constant torque turns a pendulum's period
     * with it). It was the fourth root, which made a rapier barely quicker than a machete; at the
     * square root a light, near-hand blade is far quicker and a heavy head is slower, and a sword
     * still swings as it did.
     */
    public static final float SWING_EXPONENT = 0.5f;
    /** Each step of forging flaw takes this share of the edge's toughness. */
    public static final float FLAW_TOUGHNESS_LOSS = 0.15f;

    /** Each step of forging flaw takes this share of the edge's durability: a ruined part (three) starts at half. */
    public static final float FLAW_DURABILITY_LOSS = 1f / 6f;

    /** A thin section bites as the square root of its thinness: a thick one is the reverse. */
    public static final float SECTION_BITE_EXPONENT = 0.5f;

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

    // --- tips and studs: a hard piece set in a tough body ------------------------------------

    /** Share of a tipped zone's toughness and wear that is the tip's; the rest is the body it is set in. */
    public static final float TIP_SHARE = 0.3f;
    /** Share of a piece of plate each set stud covers, and the most they cover. */
    public static final float STUD_SHARE = 0.1f;
    public static final float STUD_MAX = 0.5f;
    /** What the vanilla item of a crystal (a diamond pick) is made of when converted: this metal, the crystal set in it. */
    public static final net.minecraft.resources.ResourceLocation CRYSTAL_BODY =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "steel");
    /** Millibuckets in a cell of a shaped part: one ingot of stock. A part's size is counted in cells, its stock in volume. */
    public static final int CELL_MB = 144;
    /** Studs on a converted crystal armour piece: it was eight diamonds, now four set in steel. */
    public static final int CONVERT_STUDS = 4;
    /** Share of its hardness (and wear) a plated armour plate takes from a sound coat: a skin, not a stud. */
    public static final float COAT_SURFACE_SHARE = 0.1f;
    /** What a coat grown burnt or mixed still does of what a sound one does. */
    public static final float COAT_FAULTY_HOLD = 0.5f;
    /** Chance, per block mined, that a tool used wet gives up a point to rust, times the share of water it does not resist. */
    public static final float CORRODE_CHANCE = 0.05f;
    /** A piece made whole of a material tougher than this share of its hardness is not brittle. */
    public static final float FRACTURE_RATIO = 0.2f;

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

    /** Durability a regrind costs: metal comes off the edge. */
    public static final int GRIND_COST = 2;

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

    // --- instruments --------------------------------------------------------------------------

    /** A balance scale reads to this many Mu. */
    public static final float BALANCE_RESOLUTION = 0.1f;
    /** The hardness of each file in a set: a file bites anything softer than itself. */
    public static final float[] SCRATCH_FILES = {0.5f, 1f, 2f, 5f, 10f, 20f, 40f, 80f};

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

    // --- throwing -----------------------------------------------------------------------------

    /**
     * Launch speed at full effort is {@code THROW_SPEED_K / sqrt(mass in Mu)}, clamped: one arm gives
     * a throw about the same energy whatever it throws, so a heavy thing leaves slower (a thrown
     * speed that read the mass was the fixed 2.0 the hatchet had, which a javelin did not share).
     */
    public static final float THROW_SPEED_K = 2.2f;
    public static final float THROW_SPEED_MIN = 0.9f;
    public static final float THROW_SPEED_MAX = 2.8f;
    /** Ticks held for a full-effort throw; an earlier release gives {@code THROW_EFFORT_FLOOR} up to 1 across the span. */
    public static final float THROW_FULL_CHARGE_TICKS = 20f;
    /** Effort of a throw let go as soon as it is allowed: speed and damage both scale with it. */
    public static final float THROW_EFFORT_FLOOR = 0.5f;
    /** Degrees a tumbling weapon turns per tick of flight. */
    public static final float THROW_SPIN_DEG = 48f;
    /** What a tumbling weapon deals when it lands side-on, as a share of a head-first blow. */
    public static final float TUMBLE_FLOOR = 0.4f;
    /** A thrown weapon in water: drag per tick for one that flies true, and for one that tumbles. */
    public static final float THROW_WATER_TRUE = 0.99f;
    public static final float THROW_WATER_TUMBLE = 0.6f;
    /** Vanilla's loyalty pull per level per tick, which the return rate multiplies. */
    public static final float RETURN_ACCEL = 0.05f;
    /** A roped weapon that struck nothing (a miss, or a block) comes back this many times as fast as one that did. */
    public static final float RETURN_UNATTACHED_MULT = 2.5f;
    /** A lighter piece comes back quicker: the return rate scales with {@code 1/sqrt(mass)} between these. */
    public static final float RETURN_MASS_MIN = 0.6f;
    public static final float RETURN_MASS_MAX = 1.6f;
    /** A roped weapon dragged back that is still not home after this long is caught on something, and drops. */
    public static final int ROPE_RETURN_MAX_TICKS = 120;
    /** A thrown weapon this close to its thrower, on the way back, is caught. */
    public static final double CATCH_DISTANCE = 1.3;
    /** Ticks a thrown weapon stands in a block before it falls out of the world as a dropped item. */
    public static final int THROW_STUCK_LIFE = 1200;
    /** Ticks it stands in a block before a returning one lets go and flies back. */
    public static final int THROW_STUCK_BEFORE_RETURN = 4;
    /** A reeled target is let go within this many blocks of the thrower, or after this many ticks. */
    public static final double REEL_STOP_DISTANCE = 2.5;
    public static final int REEL_MAX_TICKS = 100;
    /** Upward share of the reel, so a target on the ground slides over its own friction. */
    public static final float REEL_LIFT = 0.08f;
}
