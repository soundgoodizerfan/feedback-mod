// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

/**
 * Tuning constants for the weapon code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class GunTuning {

    private GunTuning() {
    }

    // --- Gun design (docs/specs/feedback_guns_spec.md sections 2 and 3, step 1) ----------------------------

    // Every figure here is a placeholder. Material figures are not here: they are
    // the material's own (material/*.json). Pressure is Pu and shares a scale with a material's
    // yield strength, which is what lets a chamber's rating be read straight off the metal.
    /** Peak chamber pressure of the propellant in a full case, in Pu. Black powder; the figure a propellant material takes over (spec 3.6). */
    public static final float GUN_PROPELLANT_PEAK_PU = 3f;

    /** Work done on a projectile per Pu over one cubic metre. Anchors Pu to joules: 3 Pu is about 100 MPa. */
    public static final float GUN_WORK_PER_PU_M3 = 3.3e7f;

    /** Most work a nominal charge of the reference propellant can give a round, however wide or long the barrel (a soft ceiling; scales with the charge's pressure). */
    public static final float GUN_WORK_PER_NOMINAL_CHARGE = 160f;

    /** How far past the nominal charge's work a long barrel can still gain (the soft ceiling's headroom), and how a gun's mass scales the hand work of a manual action (root of mass over the reference, clamped). */
    public static final float GUN_BUDGET_HEADROOM = 1.7f, GUN_MANUAL_MASS_MIN = 0.8f, GUN_MANUAL_MASS_MAX = 2.5f;

    /** Mean pressure behind the projectile over the barrel, as a fraction of the peak. */
    public static final float GUN_PRESSURE_FILL = 0.35f;

    /** Projectile length as a multiple of bore. */
    public static final float GUN_PROJECTILE_LENGTH_PER_BORE = 1.5f;

    /**
     * Shot (spec 12): a shell's projectile cut into pellets this wide (buckshot), at most this
     * many, thrown in a cone at least this wide whatever the barrel. Placeholder.
     */
    public static final float GUN_SHOT_PELLET_MM = 8f, GUN_SHOT_SPREAD_DEG = 4f;

    public static final int GUN_SHOT_MAX_PELLETS = 12;

    /**
     * A barrel as the drill first pierces a turned rod (spec 12): a pilot bore, the rod's outside,
     * its full length. Cut until the wall or the length is under these minimums and it is swarf.
     */
    public static final float GUN_BARREL_PILOT_MM = 3f, GUN_BARREL_STOCK_MM = 30f, GUN_BARREL_STOCK_M = 0.6f;

    public static final float GUN_BARREL_MIN_WALL_MM = 0.5f, GUN_BARREL_MIN_M = 0.05f;

    /**
     * A round in a bore it does not exactly fit (spec 12): it will not chamber more than
     * {@link #GUN_BORE_FIT_MM} off. Looser leaks gas -- work falls per mm, to a floor, and spread
     * grows; tighter squeezes it, and the peak pressure rises per mm. Placeholder.
     */
    public static final float GUN_BORE_FIT_MM = 1f, GUN_BORE_LEAK_PER_MM = 0.6f, GUN_BORE_LEAK_FLOOR = 0.3f,
            GUN_BORE_SPREAD_DEG_PER_MM = 3f, GUN_BORE_SQUEEZE_PER_MM = 1.5f;

    /** Propellant gas leaves with the projectile; this much extra momentum, as a fraction of the projectile's. */
    public static final float GUN_GAS_IMPULSE_FRACTION = 0.25f;

    /** Barrel bands, in metres (spec 3.3): short, medium, long. */
    public static final float GUN_BARREL_SHORT_M = 0.10f, GUN_BARREL_MEDIUM_M = 0.20f, GUN_BARREL_LONG_M = 0.40f;

    /** Chamber wall bands, in mm: thin, standard, thick. */
    public static final float GUN_WALL_THIN_MM = 1.5f, GUN_WALL_STANDARD_MM = 3f, GUN_WALL_THICK_MM = 5f;

    /**
     * The reference load's burn rate per metre of barrel (spec section 3.6): the fraction burned
     * by the muzzle is this times the barrel's length, capped at 1. The same figure as the
     * gunpowder material file, so the reference load is gunpowder's.
     */
    public static final float GUN_PROPELLANT_BURN_RATE_PER_M = 4.25f;

    /**
     * A case is drawn to scale with its bore, so its wall is this fraction of the bore's radius
     * and its rating is the metal's yield times it -- the same hoop-stress rule as a chamber, with
     * the bore cancelled. Brass's 5 then holds 3.75 Pu against gunpowder's nominal 3.
     */
    public static final float GUN_CASE_WALL_PER_RADIUS = 0.75f;

    /**
     * Overpressure ({@code docs/specs/feedback_guns_spec.md} section 3.2). Gun damage is the ratio over the
     * rating times this, summed, and the gun is spent at 1: copper's 1.1 is about fifty shots,
     * lead's 9 is the first. The shooter is hurt only past the onset ratio, a band read off the
     * spec sheet, by this many hp per unit over it, never more than the cap. Each event is
     * multiplied by 1 plus or minus the noise (the floor philosophy 10 keeps inside any band).
     */
    public static final float GUN_OVERPRESSURE_DAMAGE_PER_RATIO = 0.15f;

    public static final float GUN_HAZARD_SHOOTER_ONSET = 1.5f, GUN_HAZARD_SHOOTER_HP_PER_RATIO = 6f,
            GUN_HAZARD_SHOOTER_MAX_HP = 16f, GUN_HAZARD_NOISE = 0.25f;

    /** Charge items that make the propellant's nominal peak pressure, and the most a case will take. */
    public static final int GUN_CHARGE_NOMINAL = 4, GUN_CHARGE_MAX = 8;

    /** Bore (mm) at which {@link #GUN_CHARGE_NOMINAL} and {@link #GUN_CHARGE_MAX} hold; a case's room (and so its nominal charge) grows with the bore's area. */
    public static final float GUN_CHARGE_REF_BORE_MM = 9f;

    /** Case room grows as bore to this power (2 would be the area; less keeps a magnum from being sixteen rimfires). */
    public static final float GUN_CASE_ROOM_EXPONENT = 1.5f;

    /** Rounds each magazine band holds (spec 12); standard is what every magazine held before bands. Placeholder. */
    public static final int GUN_MAGAZINE_SMALL = 6, GUN_MAGAZINE_STANDARD = 10, GUN_MAGAZINE_EXTENDED = 20,
            GUN_MAGAZINE_DRUM = 50, GUN_MAGAZINE_BELT = 100;

    /** Rounds a gun's own feed holds: a revolver's cylinder, and a tube by its barrel's length. */
    public static final int GUN_CYLINDER_ROUNDS = 6, GUN_TUBE_ROUNDS_SHORT = 4, GUN_TUBE_ROUNDS_MEDIUM = 6,
            GUN_TUBE_ROUNDS_LONG = 8;

    /** Wall-clock ms a magazine swap takes, during which the gun does not fire. */
    public static final int GUN_RELOAD_MS = 1400;

    /** Wall-clock ms to put one round into a gun's own feed, and a reload is that per round. */
    public static final int GUN_RELOAD_ROUND_MS = 450;

    /** Wall-clock ms per round into a break action's barrels: the barrels tip open, a shell goes into each and they are shut again. Placeholder. */
    public static final int GUN_RELOAD_BARREL_ROUND_MS = 500;

    /**
     * A gun can be held in one hand if its own metal weighs no more than this (a straight arm carries it)
     * and its recoil speed (impulse over mass) is no more than this (the wrist holds it). Placeholders. Units are metres per second, whatever the {@code _MS} in the name suggests.
     */
    public static final float GUN_ONE_HAND_MAX_MU = 4f, GUN_ONE_HAND_MAX_RECOIL_MS = 6f;

    /** Fired one-handed, spread and kick grow by up to this fraction of themselves, in proportion to how near the gun is to the recoil limit. */
    public static final float GUN_ONE_HAND_SPREAD = 0.6f, GUN_ONE_HAND_KICK = 0.6f;

    /** A reload takes this many times as long with a gun in each hand: the other one has to be held out of the way. */
    public static final float GUN_DUAL_RELOAD_MULT = 2.5f;


    // --- Heat, bloom, cook-off, wear (spec steps 4; sections 3.4 and 6). Placeholders. ---------

    /** Work to warm a gun by one Tu, per Mu of its own metal: a 3 Mu pistol is 36 Work/Tu. */
    public static final float GUN_THERMAL_MASS_PER_MU = 12f;

    /** Work a barrel sheds per tick per Tu above ambient, over its thermal mass: heavy barrels cool slowly. */
    public static final float GUN_COOLING_WORK_PER_TICK = 9f;

    /**
     * Heat dumped in the barrel per Work delivered to the bullet. Most of a charge's energy is not
     * the bullet's, but most of what is not leaves with the gas: was 2, which put about 28 Tu into
     * a pistol per shot and smoked it in nine. A real steel barrel warms a few degrees a shot.
     */
    public static final float GUN_HEAT_PER_WORK = 0.3f;

    /** Spread is this many times wider again at the ignition temperature than cold (a soft consequence, never a lockout). */
    public static final float GUN_BLOOM_AT_IGNITION = 1.5f;

    /** Bore erosion per Work delivered, times (1 + how hot the barrel is as a fraction of ignition). */
    public static final float GUN_WEAR_PER_WORK = 4.5e-7f;

    /** A worn gun bottoms out here (philosophy 10: a worse gun, never an empty one). */
    public static final float GUN_CONDITION_FLOOR = 0.4f;

    /** At the floor a gun delivers this much less energy; spread opens by 1 / condition. */
    public static final float GUN_WEAR_ENERGY_LOSS = 0.3f;

    /** Damage a lever tube's chain-fired round does the gun, as a share of its life. Placeholder. */
    public static final float GUN_CHAIN_FIRE_GUN_DAMAGE = 0.1f;

    /** A cooked-off round hurts the shooter this much per nominal charge, capped. */
    public static final float GUN_COOKOFF_HP = 2f, GUN_COOKOFF_HP_MAX = 8f;


    // --- What a hit does (spec 3.5). Placeholders. ---------------------------------------------

    /**
     * A block whose material has a toughness at or under this is brittle enough for a bullet to
     * break (glass 1, ice 1; stone 6, obsidian 2 are not). It is the marker figure the author
     * ruled on, not a list: a pack's pane qualifies because of what it is.
     */
    public static final float GUN_BREAK_TOUGHNESS_MAX = 1.5f;

    /** Work a bullet gives up to break a block, per unit of the block's toughness. */
    public static final float GUN_BREAK_WORK_PER_TOUGHNESS = 40f;

    /**
     * Momentum a bullet needs to break a brittle block, per unit of its toughness, in kg m/s
     * (spec 3.5, after Create Big Cannons: what breaks a block is a blow, so momentum, and what it
     * costs is work). A 9 mm lead pistol round carries about 2; glass is toughness 1.
     */
    public static final float GUN_BREAK_MOMENTUM_PER_TOUGHNESS = 0.5f;

    /**
     * A bullet softer than the block it meets glances off when it comes in shallower than this:
     * the cosine of the angle to the surface normal (about 70 degrees off it). It keeps
     * {@code GUN_RICOCHET_KEEP} of its energy, at most {@code GUN_RICOCHET_MAX} times.
     */
    public static final float GUN_RICOCHET_MAX_COS = 0.34f, GUN_RICOCHET_KEEP = 0.5f;

    public static final int GUN_RICOCHET_MAX = 2;

    /** The projectile's hardness when a shot names no material: lead's. */
    public static final float GUN_DEFAULT_PROJECTILE_HARDNESS = 0.3f;

    /** Anchor round: ticks and Slowness level that glue a body in place. Tether round: reel speed cap, blocks per tick. Bounce: launch per level of the payload's bounce perk, and how close the shooter must be. Placeholders. */
    public static final int GUN_ANCHOR_TICKS = 100, GUN_ANCHOR_AMPLIFIER = 7;

    public static final float GUN_TETHER_PULL = 0.9f, GUN_BOUNCE_LAUNCH = 0.9f, GUN_BOUNCE_REACH = 4f;

    /** A vial round's cloud against a thrown flask's reach, a share: a vial holds a mouthful. Placeholder. */
    public static final float GUN_VIAL_REACH_SCALE = 0.5f;

    /** How far a hop round throws what it hits, blocks per level of the payload's blink perk. Placeholder. */
    public static final float GUN_HOP_BLOCKS_PER_LEVEL = 6f;

    /** Share of a jacketed bullet's volume that is its core (the rest is jacket). Placeholder. */
    public static final float GUN_CORE_FRACTION = 0.7f;

    /** A sabot's core meets armour this much harder than its metal, and flies on with this share of the drag. Placeholders. */
    public static final float GUN_SABOT_PIERCE = 1.2f, GUN_SABOT_DRAG = 0.6f;

    /** Least share of a core's bore wear a soft jacket lets through. Placeholder. */
    public static final float GUN_JACKET_WEAR_FLOOR = 0.3f;

    /** A bullet that has less than this share of its muzzle energy left drops where it is. */
    public static final float GUN_MIN_ENERGY_FRACTION = 0.03f;


    // --- Handling: aiming, stance, fire modes, recoil climb (spec 7). Placeholders. -------------

    /** Spread multiplier when aimed down the sights, and the kick that is left of it. */
    public static final float GUN_ADS_SPREAD = 0.4f, GUN_ADS_KICK = 0.65f;

    /** Spread multipliers by what the shooter is doing (spec 7.1: TacZ's five stances, as multipliers). */
    public static final float GUN_STANCE_CROUCH = 0.8f, GUN_STANCE_MOVING = 1.6f, GUN_STANCE_SPRINT = 2f,
            GUN_STANCE_AIRBORNE = 3f;

    /** Blocks per tick of horizontal travel past which a shooter is moving. */
    public static final double GUN_MOVING_SPEED = 0.05;

    /**
     * A hand's hold on a gun: shots add their kick (degrees) to a strain that drains at {@link #GUN_STRAIN_DRAIN_DEG_PER_S}; past the
     * limit (a view's worth, level to straight up) the gun is thrown from the hand, past twice that the shooter is hurt too, by
     * {@link #GUN_STRAIN_DAMAGE} hp per limit over, up to the max. An impulse grip is thrown back at {@link #GUN_STRAIN_THROW_SPEED}
     * blocks per tick per limit over instead. [OPEN] until played.
     */
    public static final float GUN_STRAIN_LIMIT_DEG = 90f, GUN_STRAIN_DRAIN_DEG_PER_S = 90f, GUN_STRAIN_DAMAGE = 4f,
            GUN_STRAIN_DAMAGE_MAX = 12f, GUN_STRAIN_THROW_SPEED = 0.8f;

    /** Plates a sight is built from at assembly: iron sights are cut in the slide, a reflex one, a scope two. */
    public static final int GUN_SIGHT_PLATES_IRON = 0, GUN_SIGHT_PLATES_REFLEX = 1, GUN_SIGHT_PLATES_SCOPE = 2;

    /** FOV multiplier aimed through each sight, and ms to come up to it. */
    public static final float GUN_SIGHT_FOV_IRON = 0.85f, GUN_SIGHT_FOV_REFLEX = 0.8f, GUN_SIGHT_FOV_SCOPE = 0.4f;

    public static final int GUN_SIGHT_MS_IRON = 120, GUN_SIGHT_MS_REFLEX = 150, GUN_SIGHT_MS_SCOPE = 300;


    // --- Blends, payloads (spec 3.6). Placeholders. ---------------------------

    /** A charge that is mostly fuel still gives this much of its pressure: a fizzle, not a nothing. */
    public static final float GUN_FIZZLE_FLOOR = 0.1f;

    /**
     * Demolition charge (guns spec 13.4a). Power goes as the square root of the blend's burned
     * potential: sixteen gunpowder bare is about a block of TNT, tempered steel round it nearly
     * twice that. Casing confinement is yield strength times this, to a cap; a charge that burns
     * under this fraction throws fire; one under the minimum power only smokes.
     */
    public static final int DEMO_FILLER_MAX = 16, DEMO_FUSE_TICKS = 60;

    /** An explosive round carries one item's worth of filler per 9 mm bore volume, scaled by this; it arms after this many ticks of flight (point blank is a dud). */
    public static final float DEMO_ROUND_SCALE = 0.6f;

    /** Thrown charge (placeholders): launch speed, fragment damage per point of casing hardness, its cap, and reach per unit of blast power. */
    public static final float GRENADE_THROW_SPEED = 1.0f, GRENADE_FRAG_DAMAGE_PER_HARDNESS = 0.8f, GRENADE_FRAG_DAMAGE_MAX = 14f,
            GRENADE_FRAG_REACH_PER_POWER = 3f,
            GRENADE_PAYLOAD_FRAG = 0.12f, GRENADE_FLASH_REACH = 6f;

    public static final int GRENADE_FLASH_TICKS = 100;

    public static final int DEMO_ROUND_ARM_TICKS = 2;

    /** Seconds a target burns from an incendiary hit. */
    public static final int GUN_INCENDIARY_SECONDS = 5;

    /**
     * Metal in each part off the barrel, in mB: receiver, slide (or bolt), trigger group and grip,
     * each in its own material (spec 2, "a gun is parts"). They were one 0.3 mB frame in the
     * barrel's metal, and still add up to it.
     */
    /** Balance: the share of a barrel's mass ahead of the grip; the front/back ratio above 1 is the nose-heaviness (capped), which adds this much spread and kick per unit, and sinks the view this many degrees a second. */
    public static final float GUN_BARREL_FRONT_SHARE = 0.85f, GUN_NOSE_MAX = 3f, GUN_NOSE_SPREAD = 0.6f, GUN_NOSE_KICK = 0.5f,
            GUN_NOSE_DRIFT_DEG_PER_S = 2.5f, GUN_STOCK_BACK_MU = 0.8f;

    /** Chamber rating a receiver that holds the breech gives, per St of yield; and the wear a shot adds per Tu the barrel runs over the receiver's service temperature. */
    public static final float GUN_RECEIVER_RATING_PER_ST = 0.5f, GUN_RECEIVER_SOFT_WEAR_PER_TU = 0.0005f;

    public static final float GUN_RECEIVER_METAL_MB = 0.08f, GUN_SLIDE_METAL_MB = 0.07f, GUN_TRIGGER_METAL_MB = 0.03f,
            GUN_GRIP_METAL_MB = 0.12f;


    // --- Fittings are data: data/feedback/gun_option/*.json (spec 2, 11 step 7). Placeholders. ---

    /**
     * The report is muzzle blast (gas: the gun's, a muzzle device acts on it) and the projectile's
     * crack (its speed against sound: the round's, nothing on the gun touches it). The crack is this
     * share of a supersonic shot's report and ramps in across a tenth either side of the speed of sound.
     */
    public static final float GUN_CRACK_SHARE = 0.4f, SOUND_MS = 343f, GUN_CRACK_RAMP = 0.1f;

    /**
     * Eddy sleeve: a bullet that is not magnetic couples to the sleeve's field this much; no sleeve
     * drains more than this share of the muzzle energy; the drained energy is this many times itself in extra heat.
     */
    public static final float GUN_EDDY_NONMAGNETIC_COUPLING = 0.1f, GUN_EDDY_MAX_DRAIN = 0.75f, GUN_EDDY_HEAT = 1.5f;

    /** A report below this is a quiet vibration, heard by sculk at a short range. */
    public static final float GUN_QUIET_REPORT = 0.5f;

    /** Muffled report: the quiet band reaches this many times further up the report scale for a shooter who owns it ([OPEN]). */
    public static final float GUN_MUFFLED_SHARE = 1.5f;

    /** Placeholder. A report at or above this is thunderous: sculk hears it from twice as far as a loud one. */
    public static final float GUN_THUNDEROUS_REPORT = 0.95f;

    /** Metal in a revolver's cylinder and in a tube magazine, in mB: the feed's own part, when it has one. */
    public static final float GUN_CYLINDER_METAL_MB = 0.06f, GUN_TUBE_METAL_MB = 0.04f, GUN_BELT_METAL_MB = 0.05f, GUN_BOX_METAL_MB = 0.04f;

    /** Cycle time: a fixed action plus time per unit of impulse (kg m/s), in ms. */
    public static final int GUN_CYCLE_BASE_MS = 90;

    /**
     * Time a hand spends working a manual action between shots, in ms (spec 12): a break action
     * is the least (the next barrel is already loaded), a bolt the most. Placeholder.
     */
    /** Ticks of a flintlock's reprime and cock between shots, ms, and the share of pulls whose priming flashes in the pan without firing the charge. */
    public static final int GUN_CYCLE_MANUAL_MS_FLINTLOCK = 2200;

    public static final int GUN_CYCLE_MANUAL_MS_BREAK = 150, GUN_CYCLE_MANUAL_MS_LEVER = 450,
            GUN_CYCLE_MANUAL_MS_PUMP = 550, GUN_CYCLE_MANUAL_MS_BOLT = 800,
            /** A single-action revolver's thumb cocks the hammer between shots; a double-action one is cocked by the trigger. */
            GUN_CYCLE_MANUAL_MS_SINGLE_ACTION = 350;

    /**
     * Was 30, which made a big-bore automatic cycle at about 450 ms (130 rounds a minute). An
     * action's rate is mostly its own mechanism; impulse only slows it a little.
     */
    public static final float GUN_CYCLE_MS_PER_IMPULSE = 6f;

    /** Spread at a 0.2 m barrel, in degrees, scaling inversely with length down to a floor that is never zero (philosophy 10). */
    public static final float GUN_SPREAD_AT_MEDIUM_DEG = 2f, GUN_SPREAD_FLOOR_DEG = 0.25f;

    /** Camera kick per unit of impulse, in degrees, and how long it takes to land. */
    public static final float GUN_KICK_DEG_PER_IMPULSE = 1.6f;

    /**
     * Kick is read against a reference gun: {@link #GUN_KICK_DEG_PER_IMPULSE} is what a gun this
     * heavy with a grip this grippy kicks per impulse (a steel 9 mm, medium barrel, standard wall
     * weighs about 2.6 Mu; steel's friction is 0.6), and kick goes as reference over actual for both.
     */
    public static final float GUN_KICK_REFERENCE_MU = 2.6f, GUN_KICK_REFERENCE_FRICTION = 0.6f;

    /** Damage per Work delivered. */
    public static final float GUN_DAMAGE_PER_WORK = 0.03f;

    /** Air drag fraction of speed lost per tick, and in a fluid; gravity in blocks per tick squared (9.8 m/s² over 400). */
    public static final float GUN_DRAG_PER_TICK = 0.005f, GUN_FLUID_DRAG_PER_TICK = 0.35f, GUN_GRAVITY_PER_TICK = 0.0245f;

    /** A shot that has not hit anything by now is gone. */
    public static final int GUN_MAX_LIFE_TICKS = 80;

    /** How early the server accepts a shot against the cycle, for network jitter. Timestamp sync replaces this (spec 7.1). */
    public static final int GUN_FIRE_TOLERANCE_MS = 100;

    /**
     * Armed skeletons. Chance a newly spawned skeleton holds a gun; rounds it
     * carries beyond what is loaded; the multiplier on a gun's spread for a mob's untrained hands (never a perfect
     * aim); the drop in blocks of a flat-fired round past which a skeleton will not take the shot (its effective
     * range is worked out from the gun's muzzle speed, not set). Placeholders.
     */
    public static final float SKELETON_GUN_CHANCE = 0.03f;

    /** Chance a newly spawned wither skeleton holds a gun (same early pool; the Nether's ambient heat is the difference). Placeholder. */
    public static final float WITHER_SKELETON_GUN_CHANCE = 0.06f;

    public static final int SKELETON_GUN_SPARE = 6;

    public static final float SKELETON_GUN_STANCE = 2.5f;

    public static final float SKELETON_GUN_DROP_BLOCKS = 3f;

    /**
     * Wind-up before each shot, announced by a sound and sparks at the muzzle so a player can parry (20 t, 1 s);
     * the least gap between one shot and the next wind-up, whatever the gun's own cycle (40 t, 2 s); how far off
     * the aim the head may point and still count as aiming. Placeholders.
     */
    public static final int SKELETON_GUN_WINDUP_TICKS = 20;

    public static final int SKELETON_GUN_MIN_INTERVAL_TICKS = 40;

    public static final float SKELETON_GUN_FACING_DEG = 10f;

    /**
     * Aim error of a skeleton's shot, in degrees: each shot is aimed off the target by up to this much in a
     * random direction, on top of the gun's spread, so a target standing still is missed often (about half
     * the time at 10 blocks) and more at range. Placeholder.
     */
    public static final float SKELETON_GUN_AIM_ERROR_DEG = 3f;

    /**
     * The gun-fu melee stance (combat plan section 0, "Gun-fu first move"): a gun swung as a bludgeon. Damage is
     * {@code BASE + PER_MU x gunMass} capped at MAX, so a heavy gun hits harder and nothing is keyed on a
     * gun's identity. The cooldown is {@code TICKS_PER_MU x gunMass} between MIN and MAX ticks: a heavy gun
     * is slow to bring round again. Knockback is {@code BASE + PER_MU x gunMass}. A gun at or over
     * HEAVY_MU lands with the strong attack sound, a lighter one with the weak. Placeholders, to be tuned in play.
     */
    public static final float GUN_BLUDGEON_DAMAGE_BASE = 1f, GUN_BLUDGEON_DAMAGE_PER_MU = 1f, GUN_BLUDGEON_DAMAGE_MAX = 10f;

    public static final float GUN_BLUDGEON_TICKS_PER_MU = 4f;

    public static final int GUN_BLUDGEON_TICKS_MIN = 8, GUN_BLUDGEON_TICKS_MAX = 30;

    public static final float GUN_BLUDGEON_KNOCKBACK_BASE = 0.3f, GUN_BLUDGEON_KNOCKBACK_PER_MU = 0.08f, GUN_BLUDGEON_HEAVY_MU = 4f;

    /**
     * A bludgeon hit staggers when its impact ({@code damage x ParryTuning.JOULES_PER_DAMAGE}) over the target's
     * stand-in mass gives at least this many raw stagger ticks (the parry's formula, unclamped); the target is
     * staggered for that many ticks, bounded as the parry's, and the gun then fires one free shot at it.
     * A 3 Mu gun (4 damage, 40 J) on a zombie (about 84 Mu) gives 8 ticks, so it staggers; a 1 Mu gun
     * (2 damage) gives 4 and does not. Placeholder.
     */
    public static final float GUN_BLUDGEON_STAGGER_MIN_TICKS = 6f;

    /** Half-width of the bludgeon's swing line, blocks: a gun is a narrow head. */
    public static final float GUN_BLUDGEON_HALF_WIDTH = 0.3f;

    /** Gun-fu spin (skill unlock): a sneaking swing hits every body within this many blocks, and the longer cooldown it costs (multiplier). Placeholder. */
    public static final double GUN_SPIN_RADIUS = 2.0;

    public static final float GUN_SPIN_COOLDOWN_MULT = 1.5f;

    /** Muzzle energy that reads as full power for sound pitch and volume. */
    public static final float GUN_SOUND_REFERENCE_WORK = 1000f;


    // --- synthetic diamond: invented ------------------------------------------------------------

    /** Invented. A Shock Capsule makes grit only when the blast that reaches it has a radius in this window (vanilla: creeper 3, TNT 4, respawn anchor 5, charged creeper and end crystal 6); weaker leaves it whole, stronger vaporises the carbon back to graphite. */
    public static final float SHOCK_MIN_RADIUS = 2.5f, SHOCK_MAX_RADIUS = 5.5f;

    /** Invented. Grit one capsule yields. */
    public static final int SHOCK_GRIT = 4;
}
