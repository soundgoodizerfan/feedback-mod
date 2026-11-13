// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import io.github.soundgoodizerfan.feedback.core.unit.Density;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;

/**
 * Tuning constants for the vehicle code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class VehicleTuning {

    private VehicleTuning() {
    }
    /** Placeholder. The most voxels a micro bearing turns: a quarter of a full-size bearing's block cap, in voxels. */
    public static final int ROTARY_BEARING_MAX_VOXELS = 16384;


    // --- Engine design (docs/specs/feedback_vehicles_spec.md section 6, Engine Stand v1) --------------------

    // Every figure here is a placeholder. Material figures are not here: they are
    // the material's own (material/*.json), per the spec's standing rule.
    /** Peak torque per litre of displacement. A 1.6 L engine makes about 160 Su. */
    public static final float ENGINE_SU_PER_MB = 100f;

    /**
     * Mean piston speed a material survives, in m/s, per sqrt(yield strength St / density Mu/mB):
     * steel ~20 m/s (a 4 x 80 x 80 redlines ~7500 RPM), copper ~9.7 (~3600). Placeholder.
     */
    public static final float ENGINE_PISTON_SPEED_PER_SQRT_SPECIFIC_YIELD = 14.5f;

    /** Where peak torque sits, as a fraction of redline. */
    public static final float ENGINE_PEAK_FRACTION = 0.6f;

    /** Metal in an engine, as a multiple of its displacement: a 2 L engine is 40 L of metal. */
    public static final float ENGINE_METAL_PER_MB = 20f;

    /** Ingots of the engine's material to build it from a blueprint. Cost is [OPEN] (spec section 6). */
    public static final int ENGINE_INGOTS_PER_CYLINDER = 1;


    // --- driving ----------------------------------------------------------------------------

    /**
     * Invented. A tire is its own spring until a built spring exists to hang it from
     * ({@code docs/specs/feedback_vehicles_spec.md} section 5): it rings at {@code TIRE_SPRING_HZ} under its
     * share of the car, at {@code TIRE_DAMPING_RATIO} of critical. A frequency, not a stiffness, so a
     * heavy car squats exactly as far as a light one -- {@code g / (2 pi f)^2}, about 3 cm.
     */
    public static final float TIRE_SPRING_HZ = 3f;

    /**
     * Invented. The share of a tire's slip it cancels in one physics step, before grip caps it. Below
     * one so a stiff coupling stepped at a fixed rate cannot overshoot and ring (spec section 5,
     * <em>Stability</em>).
     */
    public static final float TIRE_SLIP_RESPONSE = 0.5f;

    /**
     * Rolling resistance, a fraction of a tire's load, is deformation: the tire flexing as it rolls
     * and the ground giving under it. Each side is {@code ROLLING_SOFT / (1 + hardness /
     * ROLLING_HARDNESS)} of its own material -- placeholder curve through real points: steel on
     * rail about 0.002, a soft tire about 0.1, dirt as ground about 0.09. So a soft, grippy tire
     * (slime) loses the most rolling and a hard one (iron) coasts: grip against coasting, a trade
     * nobody placed.
     */
    public static final float ROLLING_SOFT = 0.15f;

    public static final float ROLLING_HARDNESS = 0.5f;

    /**
     * Placeholder. Ground that falls in vanilla (sand, gravel) is loose whatever its grains'
     * hardness -- sand's 40 is quartz, not a road -- and gives this much more.
     */
    public static final float ROLLING_LOOSE = 0.15f;

    /**
     * Placeholder. Ground that slows a walker in vanilla (soul sand, honey: speed factor 0.4) is
     * ground a wheel sinks into: this times {@code 1 / speedFactor - 1}, about 0.45 there. Real
     * quicksand, not the film kind -- nothing drowns, a weak engine just does not climb out.
     */
    public static final float ROLLING_SINK = 0.3f;

    /**
     * Placeholder. A side whose material's sliding friction is under this loses rolling in
     * proportion: Minecraft's ice is near frictionless (a boat on blue ice is the fastest thing on
     * the ground), so a contact there wastes next to nothing. Blue ice (0.01) is the best coaster
     * the natural world has, as tire or as road -- and, gripping nothing, the worst to drive on.
     * Grippier than this is capped, not penalised again: softness already charges slime.
     */
    public static final float ROLLING_SLICK = 0.5f;

    /** Ground no material claims rolls like firm ground. */
    public static final float ROLLING_UNKNOWN_GROUND = 0.01f;

    /**
     * Real. Half the density of air, in kpg per block^3 (1.225 kg/m^3 over 2, a block a cubic metre
     * and a kpg a tonne): air drag is this times {@code Vdrag} times speed squared. Nothing invented
     * stands between the silhouette the player welded and the force it costs.
     */
    public static final float AIR_HALF_DENSITY_KPG_PER_BLOCK3 = 0.0006125f;

    /**
     * Wings (spec 4, <em>Wings</em>). Real: a flat plate square on to the flow has a drag
     * coefficient of about 2; past stall the plate is a bluff sheet whose force is normal to it,
     * {@code WING_CD_NORMAL x sin(alpha)}, split into lift and drag by the angle.
     * Placeholder: skin-friction drag of a plate edge-on, the floor that keeps lift plus drag
     * dissipative (lift is square to the flow, so it does no work; any drag above zero takes energy out).
     * <b>[OPEN]</b> all four below.
     */
    public static final float WING_CD_NORMAL = 2f, WING_CD0 = 0.02f;

    /**
     * Placeholder. Lift coefficient per radian of angle of attack before stall. A thin-airfoil
     * wing is 2 pi (6.3); a finite wing about 5.
     */
    public static final float WING_LIFT_SLOPE = 5f;

    /**
     * Placeholder. A plate is a wing when its thickness over the root of its area is at most this
     * (0.25 is a quarter-block square) and its stall angle runs from 8 degrees at the thinnest to
     * 18 at this ratio: a thinner sheet lets go earlier, a thicker one holds the flow longer.
     */
    public static final float WING_MAX_THIN = 0.25f, WING_STALL_MIN_RAD = 0.14f, WING_STALL_MAX_RAD = 0.31f;

    /**
     * Placeholder. A slab thicker than {@link #WING_MAX_THIN} of its chord (a block, four voxels) stalls later still, up to this angle at
     * {@link #WING_THICK_FULL} of the chord, and drags more: {@code WING_CD_PER_LAYER} on top of {@link #WING_CD0} for each layer past the first.
     * A column of more than {@link #WING_MAX_LAYERS} voxels is a bluff body, not a wing.
     */
    public static final float WING_STALL_THICK_RAD = 0.45f, WING_THICK_FULL = 0.75f, WING_CD_PER_LAYER = 0.04f;

    public static final int WING_MAX_LAYERS = 4;

    /**
     * Ticks a Driver's Seat key takes to ramp from nothing to full, and back (spec section 5,
     * <em>Keyboard to pedal</em>: "over about a second").
     */
    public static final int SEAT_RAMP_TICKS = 20;

    /**
     * Invented. The share of its torque and speed a running engine keeps with the throttle shut:
     * it idles, so the shaft -- and a controller turning on it -- does not stop at a red light.
     */
    public static final float ENGINE_IDLE_FRACTION = 0.15f;

    /**
     * Placeholder. Power steering: a turning coupled run lends the steering up to this much more
     * force at the kingpins (the arm alone is 200), reaching it at {@code STEER_ASSIST_FULL_RPM}; the
     * run pays {@code STEER_ASSIST_DRAW} of the assist as load, scaled by how far the wheel is off centre.
     */
    public static final Su STEER_ASSIST_SU = new Su(1500f);

    public static final float STEER_ASSIST_FULL_RPM = 16f, STEER_ASSIST_DRAW = 0.1f;

    /**
     * Invented. How hard a brake's pads clamp at full brake, as a torque before the pad's
     * friction: {@code brake x friction x this} is what the brake holds at the hub. With an iron
     * pad (0.6) about 1.5 kN m, which locks a wheel under a quarter of a tonne -- so a full brake
     * skids and half a brake does not, and the pad material decides where between.
     */
    public static final Su BRAKE_CLAMP_SU = new Su(2500f);

    /**
     * Placeholder. Brake pad wear, from misuse only: a hold past the tire's grip loses
     * {@code PAD_WEAR_SKID_PER_S} of condition a second at twice the grip; an engine held near still
     * by the pads loses {@code PAD_WEAR_STALL_PER_TICK} a tick at full brake. Pads never wear
     * below {@code PAD_CONDITION_FLOOR}, and what they hold scales with their condition.
     */
    public static final float PAD_WEAR_SKID_PER_S = 0.05f, PAD_WEAR_STALL_PER_TICK = 0.0005f, PAD_CONDITION_FLOOR = 0.3f;

    /**
     * Placeholder. A slider's travel scales with the yield strength of its strongest tube over
     * {@code SPRING_REFERENCE_YIELD} (clamped to half to double); held against a stop under a load
     * above {@code SPRING_YIELD_G} times that same share for {@code SPRING_SNAP_TICKS} ticks
     * it snaps to a loose ball joint (see {@link #SPRING_SNAP_TICKS}).
     */
    public static final float SPRING_REFERENCE_YIELD = 3f;

    /** Placeholder. A limited slip differential closes this share of the gap between its halves' speeds each physics step. */
    public static final float LIMITED_SLIP_COUPLING = 0.15f;

    /**
     * Placeholder. Ticks (0.75 s) a slider spring held against a stop under overload must last before it snaps.
     * Time off the stop, or at the stop under yield, pays the count down twice as fast: a car resting
     * on its spring (1 g) never snaps it, only an overload does.
     */
    public static final int SPRING_SNAP_TICKS = 15;

    /** Placeholder. The load, in g (1 g is the car resting on the spring), the reference material's spring yields at. */
    public static final float SPRING_YIELD_G = 3f;

    /**
     * The transmission (vehicles spec). Placeholders. A gear costs {@code TRANSMISSION_INGOTS_PER_GEAR}
     * ingots and {@code TRANSMISSION_METAL_PER_GEAR_MB} of metal in the block's mass. Shifting between
     * two gears above {@code TRANSMISSION_FREE_SHIFT_RPM} wears it by speed times
     * {@code TRANSMISSION_WEAR_PER_RPM} over the material's hardness, down to
     * {@code TRANSMISSION_CONDITION_FLOOR}. Worn below {@code TRANSMISSION_POP_BELOW} it jumps out of
     * gear under load, per tick by the shortfall times {@code TRANSMISSION_POP_RATE}.
     */
    public static final int TRANSMISSION_INGOTS_PER_GEAR = 1;

    public static final float TRANSMISSION_METAL_PER_GEAR_MB = 144f, TRANSMISSION_FREE_SHIFT_RPM = 3f,
            TRANSMISSION_WEAR_PER_RPM = 0.03f, TRANSMISSION_CONDITION_FLOOR = 0.3f,
            TRANSMISSION_POP_BELOW = 0.6f, TRANSMISSION_POP_RATE = 0.01f;

    /** Placeholder. The clutch pedal level (0 to 1) at which the transmission's link parts. */
    public static final float TRANSMISSION_CLUTCH_PRESSED = 0.5f;

    /** Placeholder. Phosphor envelope: Tu below which pinching off seals nothing worth having, gas constant offset (Tu to absolute), full scale of a gauge in Pu per atmosphere. */
    public static final float ENVELOPE_SEAL_MIN_TU = 150f;


    // --- the engine burns fuel (spec 9.1): invented ------------------------------------------------

    /** Invented. The fuel tank (mB); mB a tick per litre swept at full opening for a fuel at the reference flame temperature; that reference (Tu). A hotter-burning fuel gives more power per mB and lasts longer. */
    public static final int ENGINE_FUEL_TANK_MB = 2000;

    /** Invented. Compression ratio as a part choice: a fuel igniting below the knock line fires before the spark and knocks; one above the misfire line will not light and floods. Ignition point is the fuel's oxidation temperature (Tu). */
    public static final float ENGINE_MEDIUM_KNOCK_BELOW_TU = 240f, ENGINE_MEDIUM_MISFIRE_ABOVE_TU = 340f;

    public static final float ENGINE_LOW_MISFIRE_ABOVE_TU = 300f, ENGINE_HIGH_KNOCK_BELOW_TU = 280f;

    /** Invented. Knock wears condition by this much a tick at full opening, down to a floor it never passes: degrades, never refuses. */
    public static final float ENGINE_KNOCK_WEAR_PER_TICK = 0.0002f, ENGINE_CONDITION_FLOOR = 0.2f;

    /**
     * Placeholder. Spark plug, wearing by running time in a vehicle (use wear, the vehicle exception). A plug of the reference
     * metal ({@code PLUG_REFERENCE_MELT_TU}, steel) lasts {@code PLUG_LIFE_TICKS} running ticks; a metal that melts hotter lasts
     * proportionally longer (spark erosion), within {@code PLUG_LIFE_RATIO} (least, most). A metal that carries heat away
     * runs colder and fouls: fouling is its conductivity over {@code PLUG_REFERENCE_CONDUCTIVITY}, within {@code PLUG_FOUL_RATIO}.
     */
    public static final float PLUG_LIFE_TICKS = 24000f, PLUG_REFERENCE_MELT_TU = 1450f, PLUG_REFERENCE_CONDUCTIVITY = 0.0125f;
    public static final float[] PLUG_LIFE_RATIO = {0.25f, 4f}, PLUG_FOUL_RATIO = {0.5f, 2f};

    /**
     * Placeholder. A worn plug misses: each running tick starts a stumble with chance {@code PLUG_MISS_CHANCE} x wear squared x
     * fouling, and a stumble gives no torque for {@code PLUG_MISS_TICKS} ticks. It never stops the engine.
     */
    public static final float PLUG_MISS_CHANCE = 0.05f;
    public static final int PLUG_MISS_TICKS = 4;

    /**
     * Placeholder. Tires wear by distance rolled under load: {@code TIRE_WEAR_PER_BLOCK} of condition a block at the softest
     * tread, divided by {@code 1 + hardness / ROLLING_HARDNESS}; a tread slipping past its grip adds {@code TIRE_SKID_WEAR_PER_BLOCK}
     * for each block it slides. Grip scales with condition and never below {@code TIRE_CONDITION_FLOOR}.
     */
    public static final float TIRE_WEAR_PER_BLOCK = 0.0001f, TIRE_SKID_WEAR_PER_BLOCK = 0.0005f, TIRE_CONDITION_FLOOR = 0.3f;

    /** Invented. Air drag on every body (kpg per block): force is this times relative air density times speed squared, so a thruster body's terminal speed is sqrt(thrust / this) (about 20 blocks/s for one reference thruster). Drivetrain frames drag by silhouette instead. */
    public static final float AIR_DRAG = 50f;

    /** Invented. Air drag of a body of micro cells (kpg per block of cross-section, which is a cube's of its voxels): {@link #AIR_DRAG} is tuned for heavy thruster bodies and holds a small light one to under 1 block/s. This puts a stone block's terminal speed near 40 blocks/s. */
    public static final float MICRO_AIR_DRAG = 0.6f;

    /** Invented. Tunnelling guard: the farthest (blocks) a thruster body may move in one physics step; its speed ceiling is this over the step. Sable steps 1/40 s by default (2 substeps), so 1.0 is 40 blocks/s, above the ~20 blocks/s drag terminal speed of one thruster. */
    public static final float THRUSTER_MAX_STEP_BLOCKS = 1.0f;

    /** Invented. Air thinner than this share of the overworld's gives an air-breathing thruster nothing to burn. */
    public static final float THRUSTER_MIN_AIR_DENSITY = 0.05f;

    /** Placeholder. The flight sensor's resolutions: altitude in blocks, airspeed in blocks per second, attitude in degrees. */
    public static final float FLIGHT_ALTITUDE_RESOLUTION = 1f, FLIGHT_AIRSPEED_RESOLUTION = 0.5f, FLIGHT_ATTITUDE_RESOLUTION = 2f;

    /** Placeholder [OPEN]. Gas cells (flight A1): mB one cell block holds (a block is 1000 mB), the most cells one connected group counts, and how far past its rest volume each skin stretches before it weeps (cloth, rubberised, plate). */
    public static final int GAS_CELL_MB = 1000, GAS_GROUP_MAX_CELLS = 4096;

    public static final float GAS_STRETCH_CLOTH = 1.15f, GAS_STRETCH_RUBBER = 1.5f, GAS_STRETCH_PLATE = 3f;

    /** Placeholder [OPEN]. A stretched skin leaks this many mB a tick per unit of stretch past its limit; an arrow or bullet hole leaks this many mB a tick until patched; a patch seals this share of leaks at once (1 = all). */
    public static final float GAS_LEAK_PER_STRETCH = 4f, GAS_HOLE_LEAK_MB_PER_TICK = 3f;

    /** Placeholder [OPEN]. A burning cell group: the least share of its gas it loses a tick however slow the fuel's own burn is, how many seconds a creature beside a burning cell is set alight, and the chance a tick a burning cell lights a flammable neighbour. */
    public static final float GAS_BURN_MIN_SHARE = 0.01f, GAS_BURN_IGNITE_CHANCE = 0.2f;

    public static final int GAS_BURN_ENTITY_SECONDS = 6;

    /** Placeholder [OPEN]. The vent valve's flow at full open (mB a tick) and the ballast tank's size (mB) and dump flow at full open (mB a tick). */
    public static final float VENT_MB_PER_TICK = 25f, BALLAST_DUMP_MB_PER_TICK = 20f;

    public static final int BALLAST_TANK_MB = 4000;

    /** Placeholder [OPEN]. A propeller (flight A2): thrust coefficient at zero airspeed and the advance ratio J = V/(nD) where thrust reaches zero, for fine, medium and coarse pitch; thrust and power go as (blades/3)^PROP_BLADE_EXPONENT. */
    public static final float[] PROP_CT0 = {0.14f, 0.11f, 0.08f}, PROP_J0 = {0.55f, 0.85f, 1.25f};

    public static final float PROP_BLADE_EXPONENT = 0.7f;

    /** Share of forward thrust a propeller makes spun backwards (blade sections run trailing edge first). Invented, tuned in play. */
    public static final float PROP_REVERSE_EFFICIENCY = 0.6f;

    /** Placeholder [OPEN]. Power coefficient as a share of Ct0 x J0, chosen so peak efficiency is about 0.8 at half the zero-thrust advance ratio. */
    public static final float PROP_CP_PER_CT0_J0 = 0.3125f;

    /** Placeholder [OPEN]. Blade tip speed (m/s) a metal survives per sqrt(yield/density), capped near the speed of sound; past it a prop loses thrust and wears. */
    public static final float PROP_TIP_SPEED_PER_SQRT_SPECIFIC_YIELD = 220f, PROP_TIP_SPEED_CAP = 290f, PROP_TIP_SPEED_WATER = 40f;

    /** Placeholder [OPEN]. Condition a prop loses per tick per unit of tip speed over its limit (relative), and the lowest it falls to. */
    public static final float PROP_WEAR_PER_TICK = 0.002f, PROP_CONDITION_FLOOR = 0.2f;

    /** Real-world derived. Water over air by density, for a screw in water. */
    public static final float WATER_OVER_AIR_DENSITY = 816f;

    /**
     * Placeholder (micro-blocks, {@code docs/dev/vehicle.md}). The density a voxel weighs at
     * when no material file claims its block, roughly rock's: a voxel is 1/64 of a block's 1000 mB
     * times its block's material density, and a block nobody has weighed still has to weigh something.
     */
    public static final Density MICRO_FALLBACK_DENSITY = new Density(2.5f);

    /**
     * Placeholder (micro rotation, {@code docs/dev/vehicle.md}). A quarter-size rotation
     * voxel's inertia over its full-size part's: mass times radius squared, mass by volume (1/64,
     * as a voxel's mass already is) and radius by size (1/4), so (1/4)^5.
     */
    public static final float MICRO_ROTATION_INERTIA_SCALE = 1f / 1024f;

    /**
     * Placeholder (micro rotation). A rotation voxel's drag over its full-size part's: by length, so
     * four micro shafts across a block drag as one full shaft does. Drag is the price of a run per
     * block of layout ({@link MachineTuning#SHAFT_DRAG_SU_PER_RPM}); a smaller bearing's lower friction is left out
     * so micro shafting is not a free way past "a long run turns slower".
     */
    public static final float MICRO_ROTATION_DRAG_SCALE = 0.25f;
}
