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

import io.github.soundgoodizerfan.feedback.core.unit.XPu;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;

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

    /**
     * Damage per unit of blow ({@code bite x sqrt(impact)}), fitted to the vanilla diamond designs as
     * this mod converts them: a diamond-tipped steel head, not a whole diamond (which is brittle and
     * shatters, so is no yardstick). At 1.8 that sword lands 6.7 (vanilla 7), the pick 5.4 (5), the
     * shovel 5.3 (5.5), and the iron sword 5.2 (6) and netherite 7.2 (8) follow from their own
     * materials with nothing set for them. It was 1.4, fitted to nothing: an iron sword was 4.3.
     */
    public static final float DAMAGE_K = 1.8f;
    /** Dig speed per unit of blow, fitted to an iron pickaxe that digs near 6. */
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
    /** Toughness the iron class also asks, unless diamond-hard: stone is hard and brittle, so a bronze edge out-reaches it. */
    public static final float IRON_CLASS_TOUGHNESS = 50f;

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

    /**
     * Plate stops a share {@code s / (s + this)} of what plate at its best stops, s being its competence.
     * 1.4 puts leather near vanilla (7 a set), iron 17, diamond 19.4; 2.87 left leather at 4 and lost to vanilla mobs.
     */
    public static final float ARMOUR_HALF_COMPETENCE = 1.4f;
    /** Armour's durability reads the plate's competence plus this, so that leather is soft but not paper. */
    public static final float ARMOUR_DURABILITY_FLOOR = 1.5f;
    /** Share of a piece's points a lining adds, by how good the lining is. */
    public static final float LINING_SHARE = 0.1f;
    /** Plate this saturated gives armour toughness, running 0 to 3 over the last tenth: steel a little, diamond and netherite most. */
    public static final float TOUGHNESS_FROM_SHARE = 0.9f;
    public static final float TOUGHNESS_MAX = 3f;
    /** Density above 8 gives knockback resistance (mass is hard to push), to a cap. */
    public static final float KNOCKBACK_DENSITY_FLOOR = 8f;
    public static final float KNOCKBACK_PER_DENSITY = 0.02f;
    public static final float KNOCKBACK_MAX = 0.1f;
    /**
     * Boots only: knockback resistance a sound coat adds per unit its friction is over the plate's own
     * (a grippier skin plants the foot, a slicker one gives it up), times how closed the coat is.
     * <b>[OPEN]</b> the size; metals sit at 0.5 to 0.6, so today the spread is small.
     */
    public static final float COAT_GRIP_PER_FRICTION = 0.1f;
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
    /**
     * A barb held in a body is a wound held open: every blow on a target a harpoon is in lands this many
     * times as hard, from any source, while the barb stays (a blow worries the wound; it is not a curse).
     */
    public static final float HELD_WOUND_MULT = 1.25f;


    // --- material perks (MaterialPerks): vanilla's own per-level figures, applied free -------------
    /** Damage per level of smite and of impaling, as the enchantments give. */
    public static final float PERK_DAMAGE_PER_LEVEL = 2.5f;
    /** Seconds a fire-aspect part sets a target alight, per level. */
    public static final int PERK_IGNITE_SECONDS_PER_LEVEL = 4;
    /** Ticks an echoing blow outlines its target. */
    public static final int PERK_ECHO_GLOW_TICKS = 60;
    /**
     * The vibration an echoing blow gives off at its target. Instrument play, not entity damage: the
     * blow's own damage already emits ENTITY_DAMAGE on every hit, so that would add nothing, whereas
     * the shard's ring is a sounded note and INSTRUMENT_PLAY is vanilla's sculk-sensor-frequency event
     * for exactly that (a goat horn), so sensors and wardens hear it only from echoing blows.
     */
    public static final Holder<GameEvent> PERK_ECHO_EVENT = GameEvent.INSTRUMENT_PLAY;
    /** Share of a featherlight part's mass the swing does not feel, per level (floored at {@link #PERK_FEATHER_FLOOR} felt). */
    public static final float PERK_FEATHER_PER_LEVEL = 0.4f;
    public static final float PERK_FEATHER_FLOOR = 0.2f;
    /** Freezing ticks a chilling blow adds per level (a freeze shows at 140). */
    public static final int PERK_CHILL_TICKS_PER_LEVEL = 70;
    /** Ticks of Poison per level of a toxic blow (poison I never kills). */
    public static final int PERK_TOXIC_TICKS_PER_LEVEL = 60;
    /** Upward speed a bouncing blow gives, per level, in blocks per tick. */
    public static final double PERK_BOUNCE_PER_LEVEL = 0.8;
    /** Ticks of Slowness per level of a clinging blow. */
    public static final int PERK_CLING_TICKS_PER_LEVEL = 60;
    /** Experience a siphoning kill adds, per level. */
    public static final int PERK_SIPHON_XP_PER_LEVEL = 3;
    /** Farthest a blinking kill carries the wielder, in blocks. */
    public static final double PERK_BLINK_MAX_BLOCKS = 6.0;

    // --- swept melee volume (combat/hit, plan 2.4 spike) --------------------------------------------
    /** Whether a melee swing also hits what its swept volume covers beyond vanilla's one picked target. The spike's one switch. */
    public static final boolean MELEE_SWEEP = true;
    /** Half-width of the swept volume, in blocks: a floor for a point-headed weapon, plus a share for a head-heavy one ({@code Fig.BALANCE}, 0 to 1). */
    public static final float SWEEP_HALF_WIDTH_MIN = 0.2f;
    public static final float SWEEP_HALF_WIDTH_BALANCE = 0.6f;
    /** Most living things one swing hits, the picked target included. */
    public static final int SWEEP_MAX_TARGETS = 6;
    /** Horizontal push on each extra target, before the swing's charge scales it (vanilla's attack uses 0.5). */
    public static final double SWEEP_KNOCKBACK = 0.5;

    // --- the grapple (combat/tether/GrappleHook) -----------------------------------------------

    /** Hook flight speed, blocks per tick, leaving the hand. */
    public static final float GRAPPLE_SPEED = 2.2f;
    /** Hook fall per tick: a line is thrown, not fired, so a long cast arcs. */
    public static final double GRAPPLE_GRAVITY = 0.03;
    /** The line's length in blocks: how far a hook can fly and the tether's rope length. */
    public static final float GRAPPLE_ROPE = 24f;
    /** Blocks per tick the reel draws the lighter body in (a harpoon's reel is its own figure). */
    public static final float GRAPPLE_REEL = 0.55f;
    /** The reel lets go this close to the anchor. */
    public static final double GRAPPLE_STOP_DISTANCE = 1.8;
    /** The reel lets go after this long however far it is (10 s). */
    public static final int GRAPPLE_MAX_TICKS = 200;
    /** Ticks after the hook bites before sneaking lets go, so the sneak that cast it does not undo it. */
    public static final int GRAPPLE_SNEAK_GRACE = 6;
    /** Mu of load a line takes per St of its material's yield strength (string is wool's 3 St: 120 Mu, a little over a player's 78). Stand-in. */
    public static final float GRAPPLE_RATING_PER_ST = 40f;
    /** Yield strength assumed when the rope material has no figure. */
    public static final float GRAPPLE_FALLBACK_ST = 1f;
    /** A tick that closed less than this share of the reel's rate is a stall (caught on a corner, held by the ground). */
    public static final float GRAPPLE_STALL_FRACTION = 0.25f;
    /** Load multiple of a stalled line: the reel is now straining against something, not just moving a body. */
    public static final float GRAPPLE_STALL_LOAD = 3f;
    /** Strain gained per tick per unit of load over the rating; the line parts at 1, so overload frays before it snaps. */
    public static final float GRAPPLE_STRAIN_PER_TICK = 0.02f;
    /** Durability the grapple loses when its line parts (a cast costs 1). */
    public static final int GRAPPLE_BREAK_WEAR = 20;
    public static final int GRAPPLE_DURABILITY = 240;

    // --- attuning (EnchantGame, docs/feedback_enchanting_spec.md): every figure [OPEN] until played ----
    /** Coercive field, loop width and lag (ticks) of a material with no {@code attuning} block. */
    public static final float ENCHANT_COERCIVE = 0.3f, ENCHANT_LOOP_WIDTH = 0.25f, ENCHANT_LAG_TICKS = 8f;
    /** Tu between a fallback material's attuning point and its lock temperature: the window, 160 ticks on the item curve. */
    public static final float ENCHANT_WINDOW_TU = 240f;
    /** Attuning point of a fallback material that is worked cold, which has no working window to borrow the cold end of. */
    public static final float ENCHANT_COLD_POINT_TU = 300f;
    /** A session is at most this many ticks (20 s). */
    public static final int ENCHANT_MAX_TICKS = 400;
    /** [OPEN] Letters of an effect's name written on the pad, at most. */
    public static final int ENCHANT_WORD_MAX = 7;
    /** [OPEN] Ticks the pen must be up for a letter to count as finished. */
    public static final int ENCHANT_LIFT_TICKS = 10;
    /** [OPEN] Ink a letter holds, as a multiple of the walk through its stars (cells), and spare ink per letter (cells). */
    public static final float ENCHANT_INK_FACTOR = 1.8f, ENCHANT_INK_SLACK = 2f;
    /** [OPEN] Reach of a mark to a star, in cells, before the material's loop width widens it. */
    public static final float ENCHANT_REACH_CELLS = 0.8f;
    /** [OPEN] Scale from word accuracy loss to the distance the effect's band is judged against. */
    public static final float ENCHANT_GRADE_SCALE = 0.5f;
    /** [OPEN] How much of the material's lag drags the pen behind the mouse. */
    public static final float ENCHANT_PEN_LAG_SCALE = 0.25f;
    /** [OPEN] Cells per tick the pen must move per unit of coercive field to leave a mark. */
    public static final float ENCHANT_BITE_CELLS = 0.5f;
    /** [OPEN] Hand tremor on the pen's target, in cells, at the middle of the window (0.5x at the lock end, 1.5x at the top). */
    public static final float ENCHANT_PEN_JITTER = 0.12f;
    /** Ticks a session's length or start heat may differ from the server's own, for Begin's trip over the network. */
    public static final int ENCHANT_TICK_SLACK = 40;
    /** Experience a session costs, spent whatever the outcome: drawn from a Creative XP source beside the table, else the player's own points. */
    public static final XPu ENCHANT_XP_COST = new XPu(30);
    /** The share of {@link #ENCHANT_XP_COST} a part at its attuning point or hotter saves; a part at its lock saves none. [OPEN] */
    public static final float ENCHANT_HOT_DISCOUNT = 0.6f;
    /** The share of the experience cost a fresh potion burned as an attunement's reagent saves, times its potency. [OPEN] until played */
    public static final float ENCHANT_REAGENT_DISCOUNT = 0.5f;
    /** A part takes one attunement per this much enchantability, rounded up, at least one. */
    public static final int ENCHANT_PER_SLOT = 10;
    /** A part's top level is 1 + enchantability / this, capped. */
    public static final int ENCHANT_PER_LEVEL = 8, ENCHANT_MAX_LEVEL = 3;
    /** Flame a handle burns the wielder at when its material has no flame temperature of its own, Tu. */
    public static final float ENCHANT_BURN_FALLBACK_TU = 500f;
}
