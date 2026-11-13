// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import io.github.soundgoodizerfan.feedback.core.FTuning;


import io.github.soundgoodizerfan.feedback.core.unit.XPu;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;

/**
 * Every constant the modular bake and its wear read.
 * Kept beside the bake rather than in {@code FTuning} so the figure and the reason it is that
 * figure sit together. All are placeholders, calibrated so that the canonical
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
    /** Invented. What each test rig can distinguish: the Impact Pendulum's step in toughness, the Strike Post's in joules and in damage. */
    public static final float PENDULUM_STEP = 5f, POST_JOULE_STEP = 0.5f, POST_DAMAGE_STEP = 0.5f;
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
    /** A knife blade's reference lengths in cm (boning, chef, slicer) and the cut feel each gives: pull toward the grain and slide on fat. */
    public static final float[] KNIFE_LENGTH_CM = {12f, 20f, 30f}, KNIFE_GRAIN = {1.3f, 1f, 0.4f}, KNIFE_SLIDE = {0.2f, 1f, 1.6f};
    /** The sliding friction a bearing's balls are read against (a bearing's drag scales by its balls' friction over this), and a grip's wrap likewise. */
    public static final float BEARING_REF_FRICTION = 0.4f, GRIP_REF_FRICTION = 0.6f;
    /** A tip thinner than the first draws as a needle, thicker than the second as a cap (the hafted sprites). */
    public static final float TIP_ART_NEEDLE_BELOW = 0.9f, TIP_ART_CAPPED_ABOVE = 1.15f;
    /** A flywheel disc's rim-mass axis (0 spoked, 1 solid, 2 rimmed) and the mass and rim strength it gives, read between these points. */
    public static final float[] FLYWHEEL_RIM_MASS_AT = {0f, 1f, 2f}, FLYWHEEL_MASS = {0.6f, 1f, 1.3f}, FLYWHEEL_RIM = {0.8f, 1f, 0.9f};
    /** What each unit of a shield rim's width beyond the first adds to the shield's capacity multiple (1.15 banded, 1.3 flanged). */
    public static final float RIM_CAPACITY_PER_WIDTH = 0.15f;
    /** A servo's drive at a fully rated motor (what a heavy-dense copper winding gave of old), and the loss a motor short of full rating adds. */
    public static final float SUIT_DRIVE_PER_MOTOR_GRADE = 88f, SUIT_LOSS_PER_GRADE_SHORT = 0.3f;
    /** What each wrap beyond the first adds to a binding's joint multiple (1.3 at two wraps, 1.6 at three). */
    public static final float BINDING_JOINT_PER_WRAP = 0.3f;
    /** A pommel's weight axis moves the part's centre of mass this many metres per unit beyond the default (a ball -0.03, a counterweight -0.07 of old). */
    public static final float AXIS_WEIGHT_CENTRE_PER_UNIT = -0.035f;
    /** A pack shell's reference sizes in cells (pouch, standard, pack) and the store slots and mounting points each gives. */
    public static final float[] PACK_REF_CELLS = {1f, 3f, 9f}, PACK_REF_SLOTS = {9f, 18f, 27f}, PACK_REF_POINTS = {1f, 2f, 4f};
    /** A dynamotor frame's reference sizes in cells of stock (small, medium, large) and what each gives: torque ceiling, thermal mass, wall area. */
    public static final float[] FRAME_CELLS = {2f, 4f, 8f},
            FRAME_SU = {FTuning.FRAME_SU_SMALL, FTuning.FRAME_SU_MEDIUM, FTuning.FRAME_SU_LARGE},
            FRAME_MASS = {FTuning.FRAME_MASS_SMALL.workPerTu(), FTuning.FRAME_MASS_MEDIUM.workPerTu(), FTuning.FRAME_MASS_LARGE.workPerTu()},
            FRAME_AREA = {ModularTuning.FRAME_AREA_SMALL, ModularTuning.FRAME_AREA_MEDIUM, ModularTuning.FRAME_AREA_LARGE}; // float literals: inlined, safe before their declaration
    /**
     * Placeholder. A winding's reference sizes in cells of wire (coarse, fine, dense, heavy dense), the band each puts on a line and the
     * RPM each is built for (a steam engine's 16 for the two light ones, a gas turbine's 240 for the two heavy). Nothing is rated Trunk.
     */
    public static final float[] WINDING_CELLS = {1f, 2f, 4f, 8f}, WINDING_RATED_RPM = {16f, 16f, 240f, 240f};
    public static final io.github.soundgoodizerfan.feedback.core.unit.VoltageBand[] WINDING_BAND = {
            io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.LV, io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.MV,
            io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.UHV, io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.EHV};
    /** An engine head's torque at the lowest compression, and what each step of compression adds (0.95 low, 1.08 high). */
    public static final float ENGINE_HEAD_TORQUE_LOW = 0.95f, ENGINE_HEAD_TORQUE_PER_COMPRESSION = 0.065f;
    /** A propeller hub weighs this much more per blade beyond the reference count. */
    public static final float PROP_HUB_MASS_PER_BLADE = 0.2f, PROP_HUB_MASS_REF_BLADES = 3f;
    /** Share of an inlaid edge slot's hardness that the edge zone takes on (a katana's hard edge on a soft spine: it bites like the edge, rests on the body's strength). */
    public static final float EDGE_SLOT_HARDNESS_SHARE = 0.8f;
    /** Light a scope's lens loses when its material states no reflectivity. */
    public static final float LENS_DEFAULT_LOSS = 0.1f;
    /** Share of a piece of plate each set stud covers, and the most they cover. */
    public static final float STUD_SHARE = 0.1f;
    public static final float STUD_MAX = 0.5f;
    // --- addons that carry a trait of their own ---------------------------------------------------

    /** Placeholders [OPEN]. A spring's stored blow damage and its throw-speed gain per litre of spring times the stock's yield strength (iron 5): 0.1 L of iron stores 6 and throws 1.6 times as fast. */
    public static final float SPRING_STORED_PER_LS = 12f, SPRING_THROW_PER_LS = 1.2f;
    /** Placeholder [OPEN]. How far a cocked spring's release lunges a blade's wielder forward, in blocks per tick of speed. */
    public static final float SPRING_LUNGE = 0.6f;
    /** Ticks a spring-thrown weapon needs to be held before it goes, and to reach full effort. */
    public static final float SPRING_MIN_CHARGE_TICKS = 2f, SPRING_FULL_CHARGE_TICKS = 4f;
    /** Placeholder [OPEN]. Heat a cooling addon sheds, Work per tick per Tu, per litre of it times its material's conductivity. */
    public static final float COOLING_SHED_PER_LS = 120f;
    /** Placeholder [OPEN]. Litres of magazine that hold one bolt (a repeater's load is the magazine's volume over this). */
    public static final float MAGAZINE_LITRES_PER_BOLT = 0.04f;
    // --- the spring gun ({@link SpringGunSpec}) ---------------------------------------------------

    /** Placeholder [OPEN]. Joules a spring stores per litre of spring times its stock's yield strength (wrought iron 8: two 0.05 L wires store 120 J). */
    public static final float AIR_JOULES_PER_LS = 150f;
    /** Placeholder [OPEN]. Joules per litre of receiver times its yield strength that the mount holds before the spring takes a set. */
    public static final float AIR_RATING_PER_LS = 150f;
    /** Placeholder [OPEN]. Joules of spring a wielder of strength 1 cocks per tick: the cocking time is the stored energy over this. */
    public static final float AIR_COCK_J_PER_TICK = 5f;
    public static final float AIR_COCK_MIN_TICKS = 12f, AIR_COCK_MAX_TICKS = 200f;
    /** Placeholder [OPEN]. The share of its energy that the push can give the piston and pellet at best, before the barrel and bore take theirs. */
    public static final float AIR_ENVELOPE = 0.8f;
    /** Placeholder [OPEN]. A barrel this long (m) keeps half the push; a bore this wide (mm) wastes half of what reaches it. */
    public static final float AIR_LENGTH_HALF = 0.15f, AIR_BORE_HALF = 8f;
    /** Placeholder [OPEN]. The share of the piston's mass that moves with the pellet at the muzzle. */
    public static final float AIR_PISTON_SHARE = 0.3f;
    /** Placeholder [OPEN]. A pellet is a plug of its length in bores of the bore: its volume is this times the bore's disc area times the bore. */
    public static final float AIR_PELLET_LENGTHS = 1.5f;
    /** Litres of metal in one nugget (1 Qu, 16 mB): how many pellets of a bore it is worth. */
    public static final float AIR_NUGGET_LITRES = 0.016f;
    /** Placeholder [OPEN]. Hit damage per joule the pellet carries, and the fastest one leaves, in blocks per tick (a vanilla bow's arrow: 3). */
    public static final float AIR_DAMAGE_PER_J = 0.5f, AIR_SPEED_MAX = 4.5f, AIR_BOW_ARROW_SPEED = 3f;
    /** Placeholder [OPEN]. The reference pellet metal's density (iron, kg per litre) the baked velocity figure reads. */
    public static final float AIR_REF_DENSITY = 7.87f;
    /** Placeholder [OPEN]. The shot's inaccuracy at a barrel as long as {@link #AIR_LENGTH_HALF}; a longer barrel shoots truer. */
    public static final float AIR_SPREAD = 1.5f;
    /** Placeholder [OPEN]. Ticks between a shot and the next click, and the loudness of the release against a bow's. */
    public static final int AIR_SHOT_COOLDOWN = 6;
    public static final float AIR_VOLUME = 0.25f;

    // --- the pulse gun (ElectricGunSpec); every figure a placeholder [OPEN] -------------------------------

    /** Joules a seat of ampacity 1 (copper) can expel in one pulse at most: the ceiling the three power tiers are fractions of. */
    public static final float EGUN_SEAT_CEIL_J = 400f;
    /** The share of that ceiling Light, Standard and Heavy take. */
    public static final float[] EGUN_TIER_FRACTION = {0.4f, 0.7f, 1.0f};
    /** Share of a pulse a barrel of ampacity 1 takes out of it (and turns to heat), and the most any barrel takes. */
    public static final float EGUN_BARREL_LOSS_PER_AMPACITY = 0.3f, EGUN_BARREL_LOSS_MAX = 0.8f;
    /** Extra heat per shot a conductive barrel leaves, per unit of the share it takes out of the pulse. */
    public static final float EGUN_BARREL_HEAT_PER_LOSS = 2f;
    /** How far the bore's width moves a pulse's damage (and its cone) across the bore axis's range: narrow is below 1, wide above. */
    public static final float EGUN_BORE_SPAN = 0.4f;
    /** Damage a joule of pulse does, and the joules one Eu of a cell's charge is worth. */
    public static final float EGUN_DAMAGE_PER_J = 0.03f, EGUN_J_PER_EU = 0.5f;
    /** The cell voltage a seat's refill rate is measured against, and the one the Blueprint Table assumes. */
    public static final io.github.soundgoodizerfan.feedback.core.unit.VoltageBand EGUN_REF_BAND = io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.MV;
    /** Cutoff range in blocks: a base, and per metre of barrel; each power tier above Light adds this share. */
    public static final float EGUN_RANGE_BASE = 30f, EGUN_RANGE_PER_M = 100f, EGUN_RANGE_PER_TIER = 0.15f;
    /** Mobs a pulse goes through beyond the first, per power tier step (Light 1, Standard 2, Heavy 3 is {@code tier * this}). */
    public static final int EGUN_PIERCE_PER_TIER = 1;
    /** Degrees of cone a pulse has before a stock, a bore or a scope change it. */
    public static final float EGUN_SPREAD_BASE_DEG = 0.5f;

    /** Cylinder: chambers, and the refill in chambers a second at copper, two cells, the reference volts and a Standard pulse. */
    public static final int EGUN_CHAMBERS = 6;
    public static final float EGUN_CYL_REFILL_PER_S = 0.6f;
    /** Capacitor bank: seconds to recharge at Light, Standard and Heavy with a two-cell battery (more cells take longer); the shot is this many times a tier's pulse; each dielectric band adds this share to what it holds. */
    public static final float[] EGUN_BANK_RECHARGE_S = {8f, 12f, 16f};
    public static final float EGUN_CORE_MULT = 3f, EGUN_CORE_RANGE = 0.35f, EGUN_CORE_SPEED = 1.6f, EGUN_CORE_VENT_KEEP = 0.25f, EGUN_CORE_PARRY_RANGE = 3.5f,
            EGUN_CORE_PELLET_MU = 0.02f, EGUN_BANK_MULT = 3f, EGUN_BANK_DIELECTRIC_STEP = 0.15f;
    /** Burst bank: ticks between the three shots of a burst. */
    public static final int EGUN_BURST_GAP_TICKS = 2;
    /** Flywheel: ticks of a held trigger to full spin (at the reference density), ticks to spin down, and the spin the first shot waits for. */
    public static final int EGUN_FLY_UP_TICKS = 20, EGUN_FLY_DECAY_TICKS = 30;
    public static final float EGUN_FLY_FIRST_SPIN = 0.6f;
    /** Flywheel: the density a wheel spins up at the reference rate; denser stores more and spins up slower. */
    public static final float EGUN_FLY_REF_DENSITY = 7.87f;
    /** Recoil dynamo: the share of a pulse's energy returned to the cells. */
    public static final float EGUN_DYNAMO_RETURN = 0.10f;

    /** Ticks between shots: a cylinder's floor, a flux cycler, dynamo or tether, a flywheel at full spin, a capacitor bank. */
    public static final int EGUN_COOLDOWN = 8, EGUN_FLUX_CYCLE = 4, EGUN_FLY_CYCLE = 3, EGUN_BANK_CYCLE = 10, EGUN_CORE_CYCLE = 10;

    /** Heat a Standard pulse leaves, falls by this share each second; the fraction of normal rate a throttle never goes below. */
    public static final float EGUN_HEAT_PER_SHOT = 0.08f, EGUN_HEAT_COOLING_PER_S = 0.15f, EGUN_THROTTLE_FLOOR = 0.4f;
    /** Damage to the wielder from a shot fired at the heat limit, and from a shock. */
    public static final float EGUN_OVERHEAT_BURN = 2f, EGUN_SHOCK = 2f, EGUN_SHOCK_PER_EXCESS = 4f;
    /** Forced vent: lockout ticks at the limit exactly and when run a whole shot's heat over; the heat left after the vent. */
    public static final int EGUN_VENT_LOCK_MIN_TICKS = 30, EGUN_VENT_LOCK_MAX_TICKS = 80;
    public static final float EGUN_VENT_RESET_HEAT = 0.3f;
    /** Dead-man cutoff: the heat the barrel must cool below before fire resumes. */
    public static final float EGUN_CUTOFF_RESUME = 0.6f;
    /** Steam cloud: lifetime in ticks, ticks a body must stay in it before it scalds, the damage of a scald, and the cloud's half-width in blocks. */
    public static final int EGUN_STEAM_TICKS = 60, EGUN_STEAM_SCALD_TICKS = 20;
    public static final float EGUN_STEAM_DAMAGE = 3f, EGUN_STEAM_RADIUS = 1.2f;

    /** Hold charge: ticks to full, the extra power and the extra mobs pierced at full. */
    public static final int EGUN_HOLD_MAX_TICKS = 10, EGUN_HOLD_PIERCE = 2;
    public static final float EGUN_HOLD_POWER = 0.5f;
    /** Pump charge: pumps to arm the blast, the extra power, heat, spread and (with a splitter) beams of each, and the blast's reach and damage to others and to the wielder. */
    public static final int EGUN_PUMP_MAX = 3;
    /** Ticks from the pump key to its click: the forend's throw (client {@code GunMotion.jerk}, 0.55 of the stroke) lands about then. */
    public static final int EGUN_PUMP_SOUND_TICKS = 4;
    public static final float EGUN_PUMP_POWER = 0.25f, EGUN_PUMP_HEAT = 0.1f, EGUN_PUMP_SPREAD = 0.5f, EGUN_PUMP_BEAMS = 0.6f,
            EGUN_PUMP_BLAST_RADIUS = 4f, EGUN_PUMP_BLAST_DAMAGE = 10f, EGUN_PUMP_BLAST_SELF = 10f;
    /** Beam splitter: the energy of all the beams together against one pulse, and the cone's degrees per beam. */
    public static final float EGUN_SPLIT_TOTAL = 1.5f, EGUN_SPLIT_DEG_PER_BEAM = 1.2f;
    /** Arc fork: the reach of an arc from one target to the next, and the share of the pulse an arc carries. */
    public static final float EGUN_FORK_RANGE = 6f, EGUN_FORK_SHARE = 0.6f;
    /** A wet wielder's arc fork reaches this far for everything wet, with this share of the pulse. */
    public static final float EGUN_WET_FORK_RANGE = 8f, EGUN_WET_FORK_SHARE = 0.5f;
    /** Shock: the extra share of the hit a wet or metal-armoured target takes, and the ticks of stagger per joule. */
    public static final float EGUN_SHOCK_BONUS = 0.5f, EGUN_STAGGER_TICKS_PER_J = 0.05f;
    /** Scope zoom charge: ticks of aiming to full, the extra cutoff range and the share the cone closes at full. */
    public static final int EGUN_ZOOM_TICKS = 40;
    public static final float EGUN_ZOOM_RANGE = 0.5f, EGUN_ZOOM_TIGHT = 0.5f;
    /** A cylinder pulse redirected by a coin hits this many times as hard. */
    public static final float EGUN_COIN_CYLINDER = 1.5f;
    public static final float EGUN_VOLUME = 0.6f;
    /** Placeholder [OPEN]. The fewest and most a wielder's strength scales the cocking time by. */
    public static final float AIR_PULL_MIN = 0.5f, AIR_PULL_MAX = 3f;
    /** What the vanilla item of a crystal (a diamond pick) is made of when converted: this metal, the crystal set in it. */
    public static final net.minecraft.resources.ResourceLocation CRYSTAL_BODY =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "steel");
    /** Millibuckets in a cell of a shaped part: one ingot of stock. A part's size is counted in cells, its stock in volume. */
    public static final int CELL_MB = 144;
    /** The smallest stock there is: a nugget, a ninth of a cell (one Qu of metal). A part's stock is rounded up to it. */
    public static final int NUGGET_MB = 16;
    /** A raised shield slows its bearer by this share per Mu of mass above {@link #SHIELD_SLOW_FREE_MASS}, to at most {@link #SHIELD_SLOW_MAX}. */
    public static final float SHIELD_SLOW_PER_MASS = 0.016f;
    /** The mass (Mu) a raised shield can have before it slows anyone: a light buckler is carried without notice. */
    public static final float SHIELD_SLOW_FREE_MASS = 6f;
    public static final float SHIELD_SLOW_MAX = 0.35f;
    /** The learning game: parts read per specimen at most, the fewest ticks an honest reading takes, and the start levels perfect notes give. */
    public static final int NOTES_MAX_PARTS = 6;
    public static final int NOTES_MIN_READ_TICKS = 16;
    public static final float NOTES_MAX_LEVELS = 2f;
    /** Filing: strokes a file takes before it wears one point (hand tools may always wear). */
    public static final int FILE_STROKES_PER_WEAR = 5;
    /** The share of swarf's volume lost when it melts back at the crucible (oxide and drag-out). */
    public static final float SWARF_REMELT_LOSS = 0.10f;
    /** The share of a whole part's volume lost when it melts back at the crucible: less than filings, which have more surface. */
    public static final float PART_REMELT_LOSS = 0.05f;
    /** Studs on a converted crystal armour piece: it was eight diamonds, now four set in steel. */
    public static final int CONVERT_STUDS = 4;
    /** Share of its hardness (and wear) a plated armour plate takes from a sound coat: a skin, not a stud. */
    public static final float COAT_SURFACE_SHARE = 0.1f;
    /** What a coat grown burnt or mixed still does of what a sound one does. */
    public static final float COAT_FAULTY_HOLD = 0.5f;
    /** Share of a brittle coat one hit chips off per multiple of the strain its material takes; the strain of a blow is its damage. */
    public static final float COAT_CHIP_SHARE = 0.04f;
    /** Strain an armour point of damage puts on a coat (a plate's damage is a quarter of the blow). */
    public static final float COAT_ARMOUR_STRAIN = 4f;
    /** Most a whole coat that does not melt cuts the heat a shield face takes in. */
    public static final float COAT_HEAT_SHIELD = 0.5f;
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
    /** [OPEN] Metres a part of a still item hangs from its joint: the lever of its weight against the joint's strength (a stand-in; the Blueprint Table's per-link figure). */
    public static final float JOINT_STATIC_LEVER = 1f;
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

    /** Suit servo: amps of drive per amp of the winding conductor's ampacity per cell of wire. */
    public static final float SUIT_DRIVE_PER_AMPACITY = 11f;
    /** Suit servo: waste share of the drain with a perfect conductor (friction in the joints), per unit of conductor drop per cell of wire, and the most it can reach. Invented. */
    public static final float SUIT_LOSS_FLOOR = 0.12f, SUIT_LOSS_PER_DROP = 4f, SUIT_LOSS_MAX = 0.5f;
    /** The least and most an upgrade's material can make it work, as a share of full strength. */
    public static final float SUIT_POWER_MIN = 0.25f, SUIT_POWER_MAX = 1.5f;
    /** A module's draw multiplier runs from thin wire to heavy conductor: {@code WIRING_WORST} down to {@code WIRING_BEST}, the wiring's ampacity over cells half-saturating at {@code WIRING_HALF_AMPS}. */
    public static final float WIRING_WORST = 1.3f, WIRING_BEST = 0.7f, WIRING_HALF_AMPS = 12f;
    /** Amps of wiring (ampacity times cells) per attachment a module can feed, at least one. */
    public static final float WIRING_AMPS_PER_ATTACHMENT = 8f;
    /** A suit module's support parts: the share of an upgrade's strength a worthless one still gives, and the capacity (Eu) and rating (amps) at which a cell or motor is half as good as it gets; placeholders. */
    public static final float SUPPORT_FLOOR = 0.5f, SUPPORT_CELL_HALF = 2000f, SUPPORT_MOTOR_HALF = 20f;
    /** The share of its strength an upgrade whose bore is cut far from the size it wants still has. */
    public static final float BORE_FLOOR = 0.5f;
    /** The grade a drawing assumes for a support it does not have: a middling one. */
    public static final float DRAWN_SUPPORT_GRADE = 0.5f;
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

    // --- backpack ---
    /** {@code Mu} of carry rating per unit of the strap material's strength. Invented. */
    public static final float PACK_CARRY_PER_STRENGTH = 8f;
    /** Slots of a shell shaped in no named variant (the satchel). */
    public static final float PACK_DEFAULT_SLOTS = 18f;
    /** Share of base speed an overloaded wearer loses: a switch, not a dial. */
    public static final float PACK_OVERLOAD_SLOW = 0.3f;
    /** Wear points the strap takes each second spent over its rating. [OPEN] the rate. */
    public static final int PACK_OVERLOAD_WEAR = 1;
    /** {@code Mu} of a stack item in the tag {@code feedback:pack_mass/light}, and in {@code heavy}, and of any other item no material explains. */
    public static final float PACK_MASS_LIGHT = 0.05f, PACK_MASS_HEAVY = 3f, PACK_MASS_DEFAULT = 0.5f;

    /** Mounting points a shell with no named variant has, and a frame adds. Invented. */
    public static final int PACK_DEFAULT_POINTS = 2, PACK_FRAME_POINTS = 2;
    /** Mounting points each fitting takes (the crafting pad takes more); the frame takes none and gives {@link #PACK_FRAME_POINTS}. Invented. */
    public static final int PACK_FITTING_POINTS = 1, PACK_PAD_POINTS = 2;
    /** {@code Mu} of carry per unit of strength: a frame's rods (they never wear), a hip belt's band, a padded panel's softness times the strap's rating. Invented. */
    public static final float PACK_FRAME_CARRY = 3f, PACK_BELT_CARRY = 5f, PACK_PANEL_GAIN = 0.25f;
    /** Share of max health a night in a bedroll of comfort 1 gives back, and the wear it takes. Invented. */
    public static final float BED_HEAL = 0.5f;
    public static final int BED_WEAR = 1;
    /** Blocks around the sleeper that a hostile must be outside of, as for a bed. */
    public static final int BED_SAFE_RADIUS = 8;

    /** Invented. A bed's warmth: the conductivity (Conductance scale) at which a padding keeps half the heat in; and how much of a mattress's comfort a metre of frame lift adds (a cot lifts 0.6, a bedstead 1). */
    public static final float BED_WARM_HALF_CONDUCTIVITY = 1e-4f, BED_LIFT_COMFORT = 0.25f;
    /** Invented. The warmth at which a night in the bed also thaws powder-snow chill. */
    public static final float BED_THAWS_AT = 0.6f;
    /** Seconds between the fallback weighings of a worn pack (equip, unequip and every change of contents weigh at once). */
    public static final int PACK_FALLBACK_TICKS = 200;
    /** Load multiplier of a soaked cloth shell without a coat, wear a second a bare cloth shell takes while its wearer burns, ticks of freeze a warm liner takes off each second, and the light a lantern in the bracket gives. Invented. */
    public static final float PACK_WET_FACTOR = 1.5f;
    public static final int PACK_SCORCH_WEAR = 2, PACK_THAW_TICKS = 40, PACK_LAMP_LEVEL = 12, PACK_LIGHT_TICKS = 40;

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

    /** The most a spear head's {@code barb} axis reads: a target held by a barbed head at this barb takes the full {@link #HELD_WOUND_MULT}. */
    public static final float BARB_MAX = 3f;
    /** Placeholder. Share of a blow a barb tears out of the target, per point of barb: a pulled target is a wounded one. */
    public static final float BARB_TEAR_PER_BARB = 0.1f;
    /** Placeholder. Blocks a second of pull a barbed hit puts on the target toward the wielder, per point of barb. */
    public static final float BARB_PULL_PER_BARB = 0.25f;
    /** Placeholder. Ticks the weapon is held up working a barb out of the target, per point of barb. */
    public static final int BARB_WITHDRAW_TICKS = 8;
    /** Placeholder. A spear build whose baked {@code BALANCE} (share of swing moment in the head) is below this is balanced for the hand and thrown on release; at or above it is head-heavy and charges. */
    public static final float SPEAR_THROW_BALANCE = 0.45f;
    /** Placeholder. Each tine past the first costs this share of the thrust: every target takes {@code 1 / (1 + drag x (tines - 1))} of the blow. */
    public static final float TINE_DRAG = 0.1f;
    /** Placeholder. The look-line match a one-tine spear needs to land a blow (1 is dead centre). */
    public static final float TINE_CONE_NARROW = 0.9f;
    /** Placeholder. The same match at five tines: a fork covers a wider cone. */
    public static final float TINE_CONE_WIDE = 0.6f;
    /** Placeholder. Armour value at which a tine's thinner section has lost half of its bite: the blow is cut by {@code (1 - 1 / sqrt(tines))} times armour over (armour + this). */
    public static final float TINE_ARMOUR_HALF = 10f;


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
    /** The bare grapple's line in blocks: how far a hook can fly and the tether's rope length. */
    public static final float GRAPPLE_ROPE = 16f;
    /** The line with the extra coil fitted. */
    public static final float GRAPPLE_ROPE_LONG = 28f;
    /** Blocks per tick the bare reel draws a body in (a harpoon's reel is its own figure). */
    public static final float GRAPPLE_REEL = 0.3f;
    /** Reel speed multiple with the winch fitted. */
    public static final float GRAPPLE_WINCH_MULT = 2f;
    /** Share of speed kept and added on letting go of a line while moving at least {@link #GRAPPLE_RELEASE_MIN} (the swing's momentum thrown on). */
    public static final double GRAPPLE_RELEASE_BOOST = 1.2;
    /** Blocks per tick under which a release carries no boost. */
    public static final double GRAPPLE_RELEASE_MIN = 0.35;
    /** Most speed a release boost can reach, blocks per tick. */
    public static final double GRAPPLE_RELEASE_MAX = 1.9;
    /** Most blocks a tick of stretch is drawn back along a swinging line (a stand-in; past it the line stretches). */
    public static final double GRAPPLE_SWING_DRAW = 1.5;
    /** The reel lets go this close to the anchor. */
    public static final double GRAPPLE_STOP_DISTANCE = 1.8;
    /** The reel lets go after this long however far it is (10 s). */
    public static final int GRAPPLE_MAX_TICKS = 200;
    /** Ticks after the hook bites before sneaking lets go, so the sneak that cast it does not undo it. */
    public static final int GRAPPLE_SNEAK_GRACE = 6;
    /** Mu of load a line takes per St of its material's yield strength (string is wool's 3 St: 120 Mu, a little over a player's 78). Stand-in. */
    public static final float GRAPPLE_RATING_PER_ST = 40f;
    /** Invented. A line's weight: its material's density times the longest line it carries times this, added to the load it takes. */
    public static final float GRAPPLE_LINE_AREA = 0.15f;
    /** Invented. Share of its rating a fibrous line keeps wet (swimming or in rain). */
    public static final float GRAPPLE_WET_RATING = 0.75f;
    /** Invented. Chance each second that lightning finds a metal line and its holder out in a thunderstorm. */
    public static final float GRAPPLE_STRIKE_CHANCE = 0.03f;
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
    /** Most places a line bends round block edges (grapplemod's SegmentHandler idea, own algorithm). */
    public static final int GRAPPLE_MAX_BENDS = 8;
    /** How far outside a block edge a bend sits, blocks, so the line clears the block. */
    public static final double GRAPPLE_BEND_CLEARANCE = 0.05;
    /** Extra share of the load a line takes for every edge it is dragged over: a corner saws the line. */
    public static final float GRAPPLE_BEND_LOAD = 0.25f;
    /** STAND-IN: Mu a block holds a hook per point of hardness (a player is ~78 Mu: dirt lets go, stone holds). */
    public static final float GRAPPLE_HOLD_PER_HARDNESS = 100f;
    /** STAND-IN: share of the holding figure a falling block (sand, gravel) keeps. */
    public static final float GRAPPLE_HOLD_LOOSE = 0.5f;
    /** STAND-IN: share a slick block (ice) keeps, and the block friction from which it counts as slick. */
    public static final float GRAPPLE_HOLD_SLICK = 0.3f;
    public static final float GRAPPLE_SLICK_FRICTION = 0.9f;
    /** STAND-IN: line strain per tick while fire or lava touches a line it can burn (20 ticks to part). */
    public static final float GRAPPLE_BURN_PER_TICK = 0.05f;
    /** STAND-IN: strain per (attack damage over rating) for a blade or arrow across the line. */
    public static final float GRAPPLE_CUT_PER_DAMAGE = 20f;
    /** STAND-IN: blocks from the line a weapon swing or arrow counts as touching it. */
    public static final double GRAPPLE_CUT_WIDTH = 0.6;
    public static final double GRAPPLE_CUT_REACH = 3.0;
    /** Blocks per tick squared a held direction adds while the line is taut and the player is airborne (pumping a swing). */
    public static final double GRAPPLE_AIR_ACCEL = 0.045;
    /** Horizontal speed a pumped swing stops gaining at, blocks per tick. */
    public static final double GRAPPLE_AIR_CAP = 0.9;
    /** Share of horizontal speed kept per tick on a line or just after it; vanilla air drag keeps 0.91. */
    public static final double GRAPPLE_AIR_KEEP = 0.985;
    /** Ticks after letting go that the better air keep lasts. */
    public static final int GRAPPLE_COAST_TICKS = 20;
    /** Upward speed a jump off a line adds when airborne, blocks per tick. */
    public static final double GRAPPLE_JUMP_IMPULSE = 0.45;
    /** Ticks of winching a drum takes to reach full reel speed. */
    public static final int GRAPPLE_SPIN_UP_TICKS = 10;
    /** Blocks from the pivot inside which a winched line drags speed down, so the drum does not slam the player into the anchor. */
    public static final double GRAPPLE_BRAKE_RADIUS = 3.0;
    /** Share of speed kept per tick inside the brake radius while winching. */
    public static final double GRAPPLE_BRAKE_KEEP = 0.8;
    /** Blocks per tick the line pays out while the pay-out key is held. */
    public static final float GRAPPLE_PAYOUT = 0.35f;
    /** Passes the client makes over two lines so both hold together (a joint constraint, not one line then the other). */
    public static final int GRAPPLE_JOINT_PASSES = 3;

    // --- attuning (EnchantGame, docs/specs/feedback_enchanting_spec.md): every figure [OPEN] until played ----
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
    /** Invented. The level an XP run beside the table must stand at before it pays a session; a trickle at a low level cannot push a sigil through. [OPEN] */
    public static final float ENCHANT_RUN_MIN_LEVEL = 5f;
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
    /** Placeholder [OPEN]. The spread of an as-built axis around its aim, as a share of the axis's range: a clean forging, a hand-laid part, and a forging far off its work. */
    public static final float AXIS_ERROR_GOOD = 0.04f, AXIS_ERROR_LAID = 0.04f, AXIS_ERROR_POOR = 0.25f;
    /** Placeholder [OPEN]. How far the work ratio may stray from 1 before the spread is the poor one, and the spread each flaw adds (share of range). */
    public static final float AXIS_ERROR_RATIO_SPAN = 0.5f, AXIS_ERROR_PER_FLAW = 0.02f;
    /** Placeholder [OPEN]. The most an armour piece's cover reaches: a tall, wide plate covers more than the canonical one, to this share. */
    public static final float ARMOUR_COVER_MAX = 1.3f;
    /** Placeholder [OPEN]. A {@code length} axis (cm): metres of reach per unit beyond its default, and the share of that the centre of mass moves. */
    public static final float AXIS_LENGTH_M_PER_UNIT = 0.01f, AXIS_LENGTH_CENTRE_SHARE = 0.5f;
    /** Placeholder [OPEN]. A {@code width} axis scales strike by the square root of its ratio, kept within these. */
    public static final float AXIS_WIDTH_STRIKE_MIN = 0.8f, AXIS_WIDTH_STRIKE_MAX = 1.25f;
    /** Placeholder [OPEN]. The most pierce damage one piece turns aside, as a share. */
    public static final float PIERCE_RESIST_MAX = 0.6f;
    /** Placeholder [OPEN]. How much of the whole pierce resistance each worn slot carries: chest, legs, head, feet (they sum to 1). */
    public static final float PIERCE_WEIGHT_CHEST = 0.4f, PIERCE_WEIGHT_LEGS = 0.3f, PIERCE_WEIGHT_HEAD = 0.2f, PIERCE_WEIGHT_FEET = 0.1f;
    /** Placeholder [OPEN]. Heat damage a second per point of fire conduction summed over the worn pieces, while the wearer burns or stands in lava. */
    public static final float FIRE_SOAK_PER_CONDUCT = 0.5f;
    /** Placeholder [OPEN]. Ticks between a burning wearer's armour soaking heat in (and hurting). */
    public static final int FIRE_SOAK_INTERVAL = 20;

    /** Pressure vessel: half the block's side in cm, the cube's volume in mB, and the wall (cm) the reference density is quoted at. */
    public static final float TANK_HALF_CM = 50f, TANK_CUBE_MB = 1000f, TANK_REF_WALL_CM = 5f;
    /**
     * Pu a shell holds per unit of yield strength times wall over inner radius, for a cylinder. Fitted so a 5 cm steel cylinder
     * danger line lands on the old fixed vessel's 5 Pu (steel's yield strength is 15).
     */
    public static final float TANK_PU_PER_STRESS = 3f;
    /** The critical pressure over the danger one: the old vessel's 7 over 5. */
    public static final float TANK_CRITICAL_OVER_DANGER = 1.4f;

    /** Heat exchanger: the tube area (percent of the wall) the fixed block's conductance was quoted at. */
    public static final float EXCHANGER_REF_AREA = 50f;
    /** Gas tank: cubes of gas a bottle holds, and mB of gas per Pu per cube (the old 16000 mB at 1500 mB per Pu). */
    public static final float GAS_TANK_ROOMS = 16f, GAS_TANK_MB_PER_ROOM_PU = 1.5f;
    /** Water wheel: the diameter (cm) and bucket count the fixed wheel stands for, its Su, RPM per flowing face and inertia, and the density the inertia is quoted at (wood, Mu per mB). */
    public static final float WHEEL_REF_CM = 200f, WHEEL_REF_BUCKETS = 8f, WHEEL_CAPACITY_SU = 256f, WHEEL_RPM_PER_FLOW = 4f,
            WHEEL_INERTIA = 400f, WHEEL_REF_DENSITY = 0.6f;

    /** Thermal store: the wall (cm) the fixed block stands for, its heat mass and leak, and the brick density and conductivity they were quoted at. */
    public static final float STORE_REF_CM = 20f, STORE_MASS = 1800f, STORE_LEAK = 0.05f, STORE_REF_DENSITY = 1.9f, STORE_REF_CONDUCTIVITY = 0.000175f;
    /** Leyden jar: the plate area (percent) and glass wall (mm) the fixed jar stands for, and its capacitance, leak per tick and burst amps. */
    public static final float JAR_REF_AREA = 50f, JAR_REF_WALL_MM = 4f, JAR_CAPACITY_PACKETS = 10f, JAR_LEAK = 0.002f, JAR_BURST_AMPS = 8f;

    /** Pressure shell addons: how much the bands raise the danger line at best, and the share of the room a relief valve vents per tick. */
    public static final float TANK_BANDS_GAIN = 0.6f, TANK_RELIEF_PER_ROOM = 0.004f;
    /** Heat exchanger fins: the share of a fin metal's conductivity that counts. */
    public static final float FIN_SHARE = 0.6f;
    /** Water wheel rim ring: inertia per unit of ring density. */
    public static final float RING_INERTIA = 120f;
    /** Thermal store: the salt fill's heat mass at the reference density, and the least a lagging can cut the leak to. */
    public static final float STORE_SALT_MASS = 900f, STORE_SALT_DENSITY = 2.16f, STORE_LAG_FLOOR = 0.2f;
    /** Leyden jar: the current the foil's ampacity is quoted at, and the share of leak an outer sleeve keeps. */
    public static final float JAR_REF_AMPACITY = 8f, JAR_SLEEVE_LEAK = 0.5f;

    // --- fit: a blueprint is a specification, a part is judged against it --------------------

    /** Share of each axis's range a hand-fitted part (no blueprint) may be off its mate before the fit takes a flaw. */
    public static final float FIT_TOLERANCE_HAND = 0.20f;
    /** The same, for a part built to a blueprint. */
    public static final float FIT_TOLERANCE_BLUEPRINT = 0.05f;
    /** Overshoot past the tolerance, in shares of the axis range, that gives a part its full fit flaw: the flaw grows linearly to here and stops. */
    public static final float FIT_FLAW_FULL_OVERSHOOT = 0.30f;
    /** Share of joint strength a full fit flaw costs. */
    public static final float FIT_JOINT_LOSS_CAP = 0.30f;
    /** A material this far below a drawn material's yield strength, hardness or conductivity is still clean. */
    public static final float FIT_FLOOR_SLACK = 0.15f;
    /** A material within this share of the drawn material's density keeps the mass the drawing predicts. */
    public static final float FIT_DENSITY_BAND = 0.25f;
    /** A gun's spread grows by this per share of fit lost. */
    public static final float FIT_SPREAD_PER_LOSS = 2f;
    /** The least the fit figure and the durability head start fall to. */
    public static final float FIT_MIN = 0.5f, HEAD_MIN = 0.5f;
    /** Share of the durability head start lost when every axis is out of tolerance (scaled by the out share). */
    public static final float HEAD_OUT_LOSS = 0.25f;
    /** Start condition per fastener level above (or, negative, short of) the floor, for both figures. */
    public static final float START_PER_LEVEL = 0.05f;
    /** Fastener levels a blueprint's presence counts as. */
    public static final int START_BLUEPRINT_LEVELS = 1;
    /** Width of the start-condition swing a minigame score of 0 to 1 gives, centred on 0.5 (so a perfect play adds half of it). */
    public static final float START_MINIGAME_SPAN = 0.10f;
    /** Start-condition levels lost when a hammer falls short of a recipe's {@code hammer_strength} by the most a craft allows (half of it); in between, in proportion. */
    public static final int START_HAMMER_SHORT_LEVELS = 2;
    /** The share of a recipe's {@code hammer_strength} a hammer may fall short by; past it the craft is physically impossible and refused. */
    public static final float HAMMER_SHORT_REFUSE = 0.5f;

    /** Torque minigame: the most pulls one assembly plays (one per fastener spent, so a long fastener list is capped). */
    public static final int TORQUE_MAX_PULLS = 8;
    /** Torque minigame: half the width of the green band (share of the gauge) for a quality-0 fastener, and what each quality level adds. */
    public static final float TORQUE_BAND_BASE = 0.045f, TORQUE_BAND_PER_QUALITY = 0.012f;
    /** Torque minigame: how far past the band the thread-strip zone runs (share of the gauge); held past it, the thread strips. */
    public static final float TORQUE_STRIP = 0.12f;
    /** Torque minigame: ticks of holding to wind the gauge from empty to full (about two seconds). */
    public static final int TORQUE_WIND_TICKS = 42;
    /** Rivet (upset) minigame: the most strikes one assembly plays (one per rivet spent). */
    public static final int UPSET_MAX_STRIKES = 8;
    /** Rivet minigame: half the band width for a quality-0 rivet, and what each quality level adds. */
    public static final float UPSET_BAND_BASE = 0.05f, UPSET_BAND_PER_QUALITY = 0.012f;
    /** Rivet minigame: band width multiplier for the softest and the hardest metal. */
    public static final float UPSET_SOFT_MULT = 1.4f, UPSET_HARD_MULT = 0.7f;
    /** Rivet minigame: how much further up the gauge the band sits for the hardest metal. */
    public static final float UPSET_HARD_SHIFT = 0.12f;
    /** Rivet minigame: how far past the band the head splits (share of the gauge). */
    public static final float UPSET_SPLIT = 0.12f;
    /** Rivet minigame: ticks of holding to raise the hammer from empty to full. */
    public static final int UPSET_WIND_TICKS = 34;
    /** Rivet minigame: the fewest ticks one strike can honestly take. */
    public static final int UPSET_MIN_STRIKE_TICKS = 12;
    /** Torque minigame: the fewest ticks one pull can honestly take; a result sent sooner than pulls times this scores nothing. */
    public static final int TORQUE_MIN_PULL_TICKS = 14;

    /** Fitting minigame: the most pieces one assembly plays (the grid has nine slots). */
    public static final int FIT_MAX_PIECES = 9;
    /** Fitting minigame: the fewest ticks seating one piece can honestly take; a result sent sooner than pieces times this scores nothing. */
    public static final int FIT_MIN_SEAT_TICKS = 6;
    /** Fitting minigame: how much one mis-seat costs, in pieces: the score is pieces over pieces plus this times the mis-seats. */
    public static final float FIT_MISSEAT_WEIGHT = 1.5f;
    /**
     * Placeholder. St a machine head can land per Mu of head per unit of its edge competence (the square root of
     * hardness times strength, less what brittleness costs it): St of a blow is the head's, never the
     * machine's. An iron head of four ingots (about 3.9 Mu, competence about 6.9) comes to 24 St, the ceiling
     * the Hammer and the Press had before heads existed.
     */
    public static final float HEAD_ST_PER_MU_COMPETENCE = 0.88f;

    /**
     * Placeholder. The most St of an ordinary blow ({@code HandSwing.BLOW}) a hand tool's head can carry, per Mu of
     * tool times the head's competence. The arm sets the blow and the head only has to carry it
     * ({@code ModularHandTool}: the head's figure is a share of the ordinary blow, capped at 1), so a better head
     * never lifts a hand over the hard gate steel stands behind. Fitted so a cobblestone head of four cells on a
     * stick (about 1.35 Mu, competence 5.5) comes to 3.75 St, a quarter over the 3 an ordinary blow asks, and a light
     * or soft head (bone, copper) falls short of it and lands a smaller share of every swing.
     */
    public static final float HAND_ST_PER_MU_COMPETENCE = 0.5f;

    /** Placeholder. The most points of wear one act at the stonecutter costs a saw: stock this many times harder than the saw's edge (or more) wears it this much. */
    public static final int SAW_MAX_WEAR = 8;

    // --- construction options of hand tools and shears ----------------------------------------

    /** Invented. Shears: how a blade pair's length moves the clip. Coarse work (wool, leaves) goes as length to this power (a bigger bite per snap); fine work (cobweb, vines) goes as length to minus this power (control). */
    public static final float SHEARS_CLIP_EXPONENT = 0.5f;
    /** Invented. Shears: blocks of interaction range per whole length over the default blade. */
    public static final float SHEARS_REACH_PER_RATIO = 0.6f;
    /** Invented. Shears: share of its life a pivot pair keeps with no pin through it. */
    public static final float SHEARS_NO_PIN_HOLD = 0.4f;

    /** Invented. Screwdriver: fastener levels a tip shape gains or loses against the fastener it is set in (cross on a screw, hex on a bolt) and what a shaft a whole length over the default adds (it reaches the seat square). */
    public static final float DRIVER_SHAFT_LEVELS_PER_RATIO = 1.5f;
    /** Invented. Screwdriver: wear a craft costs per whole length of shaft over the default (a long shaft twists and tires). */
    public static final float DRIVER_SHAFT_WEAR_EXPONENT = 1.5f;

    /** Invented. File: tool marks (0 smooth to 1 gouged) a coarse stroke leaves and a smooth one burnishes away; the marks at which a part reads one flaw worse. */
    public static final float FILE_MARKS_FLAW_AT = 0.5f;

    /** Invented. Spoon: bowl depth's effect on scooping (skim and taste) against stirring: scoop goes as depth to this power, stir as depth to minus it. */
    public static final float SPOON_DEPTH_EXPONENT = 0.5f;
    /** Invented. A use released inside this many ticks is a tap (a skim); held longer it is a stir. */
    public static final int SPOON_TAP_TICKS = 6;

    /** Invented. The volume of the reference spoon (bowl and handle), mB: the spoon whose stir and skim figures are as written. A bigger bowl does proportionally more. */
    public static final float SPOON_REF_BOWL_MB = 155f;

    /** Invented. Ticks of stirring per point of spoon wear. */
    public static final int POT_STIR_WEAR_TICKS = 100;

    /** Invented. Share of a pot's excess heat that reaches the hand through a spoon handle, as {@link FTuning#TONGS_EXPOSURE} is for tongs. */
    public static final float SPOON_EXPOSURE = 0.5f;

    /** Invented. A dish on a warming container (blaze rod) is held at this heat, in Tu: warm, under the point a bare hand feels pain. */
    public static final Tu WARMING_HOLD_TU = new Tu(70f);

    /** Invented. A dish whose container has the featherlight perk (breeze rod) takes this share of the eating time. */
    public static final float DISH_FEATHER_EAT_SCALE = 0.6f;

    /**
     * Invented. The adjectives a dish earns from its figures (dish spec 5): blended water over soggy or under dry, fat share
     * over rich, hunger per litre of container over dense or under light, the strongest compound's concentration over bright
     * or under bland. Each is glossed on Shift from the constant that decides it.
     */
    public static final float DISH_SOGGY_MOISTURE = 0.8f;

    public static final float DISH_DRY_MOISTURE = 0.45f;

    public static final float DISH_RICH_FAT = 0.25f;

    public static final float DISH_DENSE_HUNGER_PER_LITRE = 16f;

    public static final float DISH_LIGHT_HUNGER_PER_LITRE = 8f;

    public static final float DISH_BRIGHT_COMPOUND = 0.25f;

    public static final float DISH_BLAND_COMPOUND = 0.05f;

    public static final float DISH_HUNGER_PER_LITRE = 20f;

    public static final float DISH_EAT_BASE_SECONDS = 0.6f;

    public static final float DISH_EAT_SECONDS_PER_LITRE = 3f;

    /** Invented. How much shorter a dish takes to eat per level of cut on its food parts (sliced 1, minced 2): small pieces go down quicker. */
    public static final float DISH_EAT_CUT_SCALE = 0.12f;

    /** Invented. The water in a pot serving used as a soup's base (a ladle of broth or stew): wet, but not all water. */
    public static final float SERVING_MOISTURE = 0.9f;

    /**
     * Invented. Moisture migration (dish spec 2.3): across each neighbouring pair of food parts this share of the gap in water
     * moves from the wetter to the drier (conserved: the dish's blend does not change), times the container's seal scale. A
     * part under {@link #DISH_DRY_MOISTURE} next to one over {@link #DISH_SOGGY_MOISTURE} is soaked and feeds {@link #SOAK_SCALE}
     * of its hunger and saturation.
     */
    public static final float MIGRATION = 0.5f;

    public static final float SOAK_SCALE = 0.85f;

    public static final float SPILL_SHARE = 0.34f;

    public static final float CUT_FIBRE_LOSS_PER_LEVEL = 0.25f;

    public static final float CUT_EVEN_CV = 0.15f;

    public static final float CUT_MIXED_CV = 0.4f;

    /** Invented. The knife's feel: grain lines every this many line units, how hard fibre pulls a stroke to the nearest one (at fibre 1), and how far fat lets it slide on. */
    public static final int CUT_GRAIN = 125;

    public static final float CUT_GRAIN_PULL = 0.8f;

    public static final float CUT_FAT_SLIDE = 60f;

    /** Invented. The most cut marks a knife pass can place on a piece. */
    public static final int CUT_MAX_MARKS = 6;

    /** Invented. The mark count a minced cut uses. */
    public static final int CUT_MINCED_MARKS = 4;

    /** Invented. Ticks a mark may be placed no faster than (the server refuses a result sent quicker than that), and the slack for the begin packet's travel. */
    public static final int CUT_TICKS_PER_MARK = 4;

    public static final int PREP_SLACK_TICKS = 10;

    /**
     * Invented. The pour: a vessel fills to 1000 units and the line sits at {@link #POUR_TARGET}; a food of viscosity 1 (or
     * none given) pours {@link #POUR_FLOW} units a tick, and each decade of viscosity slows it by {@link #POUR_DRAG}
     * of that (honey, 10000, runs a quarter of the speed). Strength is poured over the line, kept between the floor and
     * ceiling; within {@link FTuning#POUR_CLEAN} of the line is clean, within {@link #POUR_SHAKY} shaky. A finish nobody poured
     * by hand counts as {@link #POUR_AUTOMATION} (sloppy, never a refusal). The hold is capped at {@link #POUR_MAX_TICKS}.
     */
    public static final int POUR_FULL = 1000, POUR_TARGET = 600;

    public static final float POUR_FLOW = 24f;

    public static final float POUR_DRAG = 0.75f;

    public static final float POUR_SHAKY = 0.30f;

    public static final float POUR_MIN = 0.3f, POUR_MAX = 2.0f;

    public static final float POUR_AUTOMATION = 0.8f;

    public static final int POUR_MAX_TICKS = 400;

    /**
     * Invented. The other two finishes, graded into the same strength as a pour so everything downstream is shared. A dry
     * powder is shaken: each stroke of the shaker (a mouse sweep back and forth) puts {@link #SHAKE_UNITS} units on the
     * dish, at most {@link #SHAKE_MAX_STROKES}, no faster than one per {@link #SHAKE_TICKS_PER_STROKE} ticks. Pieces are
     * placed one by one: {@link #PLACE_TARGET} pieces is the line, at most {@link #PLACE_MAX}; their spacing is graded as
     * a cut's is, and uneven pieces count for {@link #PLACE_MIXED_SHARE} / {@link #PLACE_RAGGED_SHARE} of their number
     * (the flavour comes in lumps).
     */
    public static final int SHAKE_UNITS = 40, SHAKE_MAX_STROKES = 40, SHAKE_TICKS_PER_STROKE = 2;

    public static final int PLACE_TARGET = 5, PLACE_MAX = 10;

    public static final float PLACE_MIXED_SHARE = 0.85f, PLACE_RAGGED_SHARE = 0.65f;

    /**
     * Invented. The knead ({@code docs/specs/feedback_food_spec.md} 6): one sample every {@link #KNEAD_STEP_TICKS} ticks of how hard
     * the dough is pressed, 0..100, at most {@link #KNEAD_MAX_SAMPLES} (the session length) and at least {@link #KNEAD_MIN_SAMPLES}.
     * Per sample at full press: gluten builds {@link #KNEAD_GLUTEN} (less the more water {@link #KNEAD_STICKY} and fat
     * {@link #KNEAD_FAT_GLUTEN} hold, and less the more tense the dough {@link #KNEAD_TENSE_RESIST}); tension builds
     * {@link #KNEAD_TENSION} by the square of the press (more with fibre {@link #KNEAD_FIBRE_TENSION}, less with fat
     * {@link #KNEAD_FAT_TENSION}) and rests away {@link #KNEAD_REST} by the slack of the press (slower with fibre
     * {@link #KNEAD_FIBRE_REST}). Tension past the tear limit ({@link #KNEAD_TEAR} scaled by {@link #KNEAD_TEAR_DRY} to
     * {@link #KNEAD_TEAR_WET} with moisture, so dry dough tears sooner) piles up overwork; {@link #KNEAD_TOUGH_OVER} of it is
     * tough. Otherwise gluten under {@link #KNEAD_GOOD_AT} is crumbly, over {@link #KNEAD_GLOSSY_AT} with the dough rested
     * back under {@link #KNEAD_RELAXED} is glossy, else good. The baked loaf's hunger and saturation scale by class; dough
     * nobody kneaded bakes at {@link FTuning#KNEAD_UNKNEADED_SCALE}, the unskilled floor.
     */
    public static final int KNEAD_STEP_TICKS = 2, KNEAD_MAX_SAMPLES = 300, KNEAD_MIN_SAMPLES = 10;

    public static final float KNEAD_GLUTEN = 0.02f, KNEAD_STICKY = 0.8f, KNEAD_FAT_GLUTEN = 0.5f, KNEAD_TENSE_RESIST = 0.6f;

    public static final float KNEAD_TENSION = 0.024f, KNEAD_FIBRE_TENSION = 1.0f, KNEAD_FAT_TENSION = 0.5f;

    public static final float KNEAD_REST = 0.03f, KNEAD_FIBRE_REST = 0.8f;

    public static final float KNEAD_TEAR = 1.0f, KNEAD_TEAR_DRY = 0.6f, KNEAD_TEAR_WET = 1.4f, KNEAD_TOUGH_OVER = 0.5f;

    public static final float KNEAD_GOOD_AT = 0.5f, KNEAD_GLOSSY_AT = 0.85f, KNEAD_RELAXED = 0.35f;

    /** Invented. The share of the samples' ticks the server insists have passed (the rest is allowance for a lagging server), so a knead cannot be sent faster than it could be played. */
    public static final float KNEAD_TIME_SHARE = 0.8f;

    /**
     * Invented. Shingling, the hand version of hammering a bloom into iron (docs/dev/modular.md, Shingling): a billet is a unit square
     * holding a few slag pockets per slag grade; one blow shifts a pocket by its landed force over the slag's viscosity, no further than
     * the footprint. A bloom is done when at most this share of its pockets is left; a heavy blow splits a bloom whose delivered work
     * per blow tops its density times the split figure, and each split loses a ninth of the metal.
     */
    public static final int SHINGLE_POCKETS_BASE = 3, SHINGLE_POCKETS_PER_SLAG = 3;

    public static final float SHINGLE_DONE_SHARE = 0.2f, SHINGLE_FOOTPRINT = 0.22f, SHINGLE_PUSH = 0.05f, SHINGLE_SPLIT_PER_DENSITY = 1.1f;

    /** Invented. Ticks a session may run past its slag's cool-down, and the longest one (a hearth holds the heat; this is the cap). */
    public static final int SHINGLE_MAX_TICKS = 600;

    /** Invented. Tu above the slag's melting point over which the pockets run at full speed; closer to it they creep. */
    public static final float SHINGLE_FLUID_RANGE_TU = 150f;

    /** Invented. Pockets leaked for each bloomery slag dropped. */
    public static final int SHINGLE_LEAK_PER_SLAG = 3;

    /** Invented. A bloom held above this Tu scales away: this many ticks of it in one session burn it to burnt iron. */
    public static final float SHINGLE_SCALE_TU = 1400f;

    public static final int SHINGLE_BURN_TICKS = 120;

    /** Invented. Each unfinished return costs this many sixteenths (nuggets) of the iron to scale; nine of them burn the bloom. */
    public static final int SHINGLE_SCALE_LIMIT = 9;

    /** Rounds in a burst. */
    public static final int GUN_BURST_ROUNDS = 3;

    /**
     * Placeholder. Hot-dip overrun: a coat total past {@link MachineTuning#PLATING_SPALLS_ABOVE_QU} is dwell the part should not have had.
     * Each this many Qu of overrun leaves the wear part one more forging flaw (a brittle intermetallic layer); past
     * {@link #HOT_DIP_DISSOLVE_ABOVE_QU} of overrun every further dip also eats {@link #HOT_DIP_DISSOLVE_WORK} of the part's
     * thickness (the {@code work} dimension, so it thins and lightens as an over-hammered part does) until it reaches
     * {@link #HOT_DIP_SCRAP_WORK} and is scrap.
     */
    public static final float HOT_DIP_FLAW_PER_QU = 0.5f;

    public static final float HOT_DIP_DISSOLVE_ABOVE_QU = 1.5f;

    public static final float HOT_DIP_DISSOLVE_WORK = 0.4f;

    public static final float HOT_DIP_SCRAP_WORK = 3f;

    /**
     * Placeholder. Wall area of the three frames, in blocks of housing: how much of a housing's conductivity each can shed
     * through. A frame's heat grows with its throughput, so its area grows with it.
     */
    public static final float FRAME_AREA_SMALL = 1f, FRAME_AREA_MEDIUM = 5f, FRAME_AREA_LARGE = 12f;

    /**
     * Placeholder. Conductance of the housing to air, Work/tick per Tu, per block of wall area per unit of the housing
     * material's {@code thermal.conductivity} (iron 0.02): a finned large iron frame sheds 10 Work a tick per Tu.
     */
    public static final float MACHINE_SHED_PER_CONDUCTIVITY = 42f;

    /** Placeholder. Length of conductor, in blocks of cable (the unit of a material's {@code electrical.drop}), per cell of wire in a winding: its loss is amps squared x drop x this. */
    public static final float WINDING_BLOCKS_PER_CELL = 0.5f;

    /** Invented. The share of a glove's exposure that is left when an insulating lining sits against the hand (a gauntlet's lining role). */
    public static final float GLOVE_LINING_SHARE = 0.5f;

    /**
     * Invented. A shield as a thermal sink. A point of blocked damage
     * is this much Work ({@code SHIELD_WORK_PER_DAMAGE}); this share of it stays in the face
     * ({@code SHIELD_HEAT_FRACTION}); the face's thermal mass is its baked mass times
     * {@code SHIELD_THERMAL_MASS_PER_MU} (the gun's figure, for the same lack of a specific heat);
     * it sheds {@code SHIELD_COOLING_WORK_PER_TICK} of Work a tick over that mass, so a heavy face
     * cools slowly. Tuned so a six-point blow warms a steel-faced shield a couple of Tu and a thin
     * wooden one ten times that.
     */
    public static final float SHIELD_WORK_PER_DAMAGE = 40f, SHIELD_HEAT_FRACTION = 0.5f,
            SHIELD_THERMAL_MASS_PER_MU = 12f, SHIELD_COOLING_WORK_PER_TICK = 9f;

    /**
     * Invented. Capacity holds in full up to this share of the way from the room to the face's
     * softening point, falls linearly to {@code SHIELD_SOFTEN_FLOOR} of itself at it, and past it a
     * blow wears the shield {@code SHIELD_HOT_WEAR_MULT} times as hard.
     */
    public static final float SHIELD_SOFTEN_ONSET = 0.5f, SHIELD_SOFTEN_FLOOR = 0.25f, SHIELD_HOT_WEAR_MULT = 2f;

    /** Invented. Ticks a fire's contact draws a face towards its temperature per blow or fire tick (fire damage lands about every 10). */
    public static final int SHIELD_FIRE_TICKS = 10;

    /** Invented. The share of a shield face's excess over the room the hand feels through its strap ({@link FTuning#GLOVE_EXPOSURE}'s way of saying it). */
    public static final float SHIELD_GRIP_EXPOSURE = 0.25f;

    /** Placeholder. Wither ticks a radiant strike gives per level, and glow ticks. */
    public static final int RADIANT_WITHER_TICKS_PER_LEVEL = 60, RADIANT_GLOW_TICKS = 100;
}
