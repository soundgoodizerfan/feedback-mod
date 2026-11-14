// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;

/**
 * Tuning constants several domains read. Each domain keeps its own figures in a {@code *Tuning} class beside the code that reads them.
 *
 * <h2>Why this file exists</h2>
 * Almost none of these figures are defensible yet. Slice 1 fixes {@code Su} for a few machines
 * and never fixes a speed at all, and the philosophy is explicit that exact values are not
 * decided. They will stay fiction until the slice has been played.
 * <p>
 * So the point of centralising them is not tidiness, it is <em>turnaround</em>. Tuning should be
 * editing one list, not hunting through a dozen block entities. When the numbers start to mean
 * something, this becomes a config file; until then it is the single place a playtest note can
 * be applied without reading any code.
 * <p>
 * Anything here marked SLICE comes from the first-slice design. Everything else is invented.
 */
public final class FTuning {

    private FTuning() {
    }
    public static final int ROTARY_BEARING_MAX_BLOCKS = 1024;

    /**
     * Placeholder (user ruling, 2026-10-23). Rim speed limit of a flywheel is this times the root of its metal's yield
     * strength over density (cast iron comes to about 40 RPM, under the overspeed trip). Past it the rim strains:
     * condition falls by {@code FLYWHEEL_WEAR_PER_OVER} per tick per unit of fractional overspeed, to the floor and no
     * further. Never a burst.
     */
    public static final float FLYWHEEL_RIM_RPM_PER_ROOT = 44f, FLYWHEEL_WEAR_PER_OVER = 0.001f, FLYWHEEL_CONDITION_FLOOR = 0.5f;

    /**
     * SLICE: a hand hammer's ordinary swing is about 3 St ({@code HandSwing.BLOW}, the default).
     *
     * <p>Against copper's hardness of 1 that is 3 Fu a swing, so a 14 Fu plate is five swings and
     * the sixth starts making foil. It is deliberately the same figure as the Mechanical Hammer's
     * long throw: the gentle machine is doing exactly what the player's arm was doing, only without
     * ever getting bored, which is the whole of what beat 1 has to say.
     *
     * <p>Steel stays out of reach by hand with no rule about hands anywhere: its hardness is 15, over
     * every swing below, so nothing lands -- philosophy 8's hard gate, a genuine impossibility.
     */
    public static final St HAND_SWING_BLOW_ST = new St(6.6f);

    /**
     * Bottoms of the condition bands, descending -- how battered a machine looks, in words.
     *
     * <h3>Why this is free, and has no instrument behind it</h3>
     * Damage to a machine is visible in a way its temperature is not. A mushroomed hammer head is
     * something you can see from across the room, so charging an instrument for it would be
     * hiding a free sense (§8). What the adjective withholds is the figure -- calipers exist for
     * that -- and the top band is silent entirely, so a sound machine adds no line to the HUD.
     */
    public static final float CONDITION_SOUND = 0.98f;

    public static final float CONDITION_MARKED = 0.85f;

    public static final float CONDITION_BATTERED = 0.65f;

    /**
     * Where a material figure reads low / middling / high: the tertile edges of that figure across
     * data/feedback/material/*.json (hardness rounded to 1). Display only; null when the figure has no band.
     */
    public static float[] statEdges(String stat) {
        return switch (stat) {
            // Not tertiles: foods crowd the bottom, and hardness runs 0.1 to 100, so thirds put iron, manganese and steel in the band of netherite.
            case "hardness" -> new float[] {3f, 25f};
            case "density" -> new float[] {1.35f, 2.8f};
            case "melting_point" -> new float[] {1000f, 1400f};
            case "service" -> new float[] {250f, 800f};
            case "thermal_conductivity" -> new float[] {0.000125f, 0.0015f};
            case "toughness" -> new float[] {3f, 10f};
            case "yield" -> new float[] {1f, 3f};
            case "friction" -> new float[] {0.4f, 0.6f};
            case "flame" -> new float[] {1200f, 1350f};
            default -> null;
        };
    }

    /**
     * Which condition band a figure falls in -- 0 sound, rising to 3 spent.
     * <p>
     * Lives here rather than in the readout because the server needs it too: wear happens blow by
     * blow and syncing every one of them would be a packet a tick for a line that changes four
     * times in the life of the machine. The server syncs when the <em>band</em> moves.
     */
    public static int conditionBand(float condition) {
        if (condition >= CONDITION_SOUND)
            return 0;
        if (condition >= CONDITION_MARKED)
            return 1;
        if (condition >= CONDITION_BATTERED)
            return 2;
        return 3;
    }


    // --- pulley and belt (machine shop) ------------------------------------------------------

    /**
     * Invented. Su a belt can transmit before it slips. Above this, the driven side receives
     * less speed than the nominal diameter ratio would give -- a genuinely different failure
     * character from a Shaft's overstress, which reads as zero speed across the whole network at
     * once. A belt sheds load instead: the far side simply turns more slowly, and the belt itself
     * wears for it (see {@link #BELT_WEAR_PER_SU_SLIPPED}).
     */
    public static final Su BELT_CAPACITY_SU = new Su(60f);

    /** Invented. Condition lost per Su of shortfall the belt slips through, per tick -- real wear
     * under real use, the same shape the Grinding Wheel's grit loss already established, because a
     * slipping belt genuinely burns itself on the pulley face. */
    public static final float BELT_WEAR_PER_SU_SLIPPED = 0.0008f;

    public static final float BELT_CONDITION_FLOOR = 0.3f;

    /** Invented. Longest belt, in blocks between pulley centres. A placement decision (§13's
     * argument for data-link range), not a realism figure. */
    public static final int BELT_MAX_LENGTH = 16;

    /**
     * What one mold holds -- one ingot's worth. Not invented: 144 mB/ingot (so 16 mB/nugget, 9
     * nuggets or 1296 mB to a block) is the de facto standard across the modded-Minecraft fluid
     * ecosystem, and matching it costs nothing and buys free interop with anything that already
     * assumes it.
     */
    public static final int MOLD_CAPACITY_MB = 144;

    /**
     * Invented, placeholder. The speed a centrifugal overspeed trip lets go at: above the Hand
     * Crank's {@link MachineTuning#HAND_CRANK_RPM} and the engine's {@link VesselTuning#STEAM_ENGINE_RPM}, so a run turning
     * at its source's own speed never trips it and only a run geared up past that does.
     * <p>
     * One fixed figure and no setting, the bimetallic strip's rule ({@link ControlTuning#BIMETALLIC_TRIP_TU}):
     * flyweights and a spring are cut for one speed, and wanting a different one means a
     * different part, not a dial (philosophy 3).
     */
    public static final Rpm OVERSPEED_TRIP_RPM = new Rpm(48f);

    /** Invented. Food starts to cook above this, in Tu: the pathogen starts dying (food spec 4.3). */
    public static final Tu FOOD_COOK_TU = new Tu(70f);

    /** Invented. Food reaches full cooking speed at this temperature; between the two it cooks proportionally slower. */
    public static final Tu FOOD_COOK_FULL_TU = new Tu(200f);

    /** Invented. Ticks of full-speed cooking to take a raw food to done. */
    public static final float FOOD_COOK_TICKS = 200f;

    /** Invented. Where food burns when its material states no oxidation temperature, in Tu. */
    public static final Tu FOOD_CHAR_TU = new Tu(300f);

    /** Invented. Char gained per tick, per Tu above the food's char temperature; 1 is charred. */
    public static final float FOOD_CHAR_PER_TU_TICK = 1e-4f;

    /** Invented. Hunger and saturation of a dried food, against its done figure. */
    public static final float FOOD_DRIED_SCALE = 0.6f;

    /** Invented. Hunger and saturation of a charred food, against its done figure. */
    public static final float FOOD_CHARRED_SCALE = 0.1f;

    /** Invented. How far from the pan a lift holds, in blocks. */
    public static final double SKILLET_LIFT_REACH = 4.5;

    /** Invented. The stockpot (food spec 5): water boils here, in Tu, and holds the pot at it while any is left. */
    public static final Tu POT_BOIL_TU = new Tu(100f);

    public static final float FOOD_ROT_PATHOGEN = 0.6f;

    /** Invented. A pathogen or toxin load under this does nothing when eaten. */
    public static final float FOOD_HAZARD_TOLERANCE = 0.05f;

    /** Invented. Dishes (dish spec 2.3): mB one food part adds to the dish's mixture (what toxin and compounds are diluted over), the hunger a litre of container holds, seconds a dish takes to eat, a base plus a share per litre of container (a skewer is a snack, a plate is not), and the knife's durability (a point per dish). */
    public static final float DISH_PART_MB = 150f;

    public static final float CUT_RAGGED_PATHOGEN_FLOOR = 0.08f;

    public static final float POUR_CLEAN = 0.10f;

    public static final float KNEAD_UNKNEADED_SCALE = 0.9f;

    /**
     * Invented. How readily Work crosses a {@code HeatExchangerBlockEntity}, in the same {@code
     * Work/t/Tu} every other {@link Conductance} is in -- see {@code Heat#exchange}. Well under
     * {@link ThermalTuning#FIRE_CONDUCTANCE} (0.6): a flame is in direct contact with a vessel, while two
     * vessels linked by an exchanger are trading heat through a wall built for the purpose, not
     * sitting in each other's fire.
     */
    public static final Conductance HEAT_EXCHANGER_CONDUCTANCE = new Conductance(0.1f);

    /** Invented. Mush at or above this shows as Mushy on the tooltip. */
    public static final float CANNING_MUSH_SHOWN_AT = 0.15f;

    /**
     * Invented. Ticks-equivalent of TPu lost per tick a hand-authored or vanilla-fallback
     * process spends outside its useful temperature range (TPu may decay, and need not be monotonic).
     *
     * <h3>Why one global constant rather than a per-process field</h3>
     * The exact decay rule is "a tuning decision", not a material property -- nothing
     * about how fast a batch forgets its progress is something a modpack author would credibly
     * want to retune per recipe. Set equal to the fastest possible accumulation rate (suitability
     * 1.0 = 1 TPu/t), so a process interrupted right at its optimum loses ground exactly as fast
     * as it could gain it -- no free ratchet in either direction.
     */
    public static final float TPU_DECAY_PER_TICK = 1f;

    /**
     * A lit campfire (any block in {@code minecraft:campfires} but the soul campfire): wood's
     * flame temperature, since it is a log fire. Melts tin, lead and zinc but sits under gold's and
     * copper's melt -- the safe fire, and it never runs out.
     */
    public static final Tu FIRE_TU_CAMPFIRE = new Tu(760f);

    /** Hard ceiling on a charge's count, whatever the bore (the widest case at {@link GunTuning#GUN_CHARGE_REF_BORE_MM} scaling); only a codec bound. */
    public static final int GUN_CHARGE_CEILING = 40;

    /** The drum's fluid and item inputs: mB of fluid that count as one part beside an item, and the input slots. Placeholders. */
    public static final int MIXER_MB_PER_PART = 144, MIXER_INPUT_SLOTS = 3;

    /** Work rate per level of Haste (vanilla's mining-speed step), and the same less per level of Mining Fatigue; the rate never falls below the floor. */
    public static final float HASTE_RATE_PER_LEVEL = 0.2f, HASTE_RATE_FLOOR = 0.2f;

    /** Propellant ignition temperature when a round names none (the reference load's). */
    public static final float GUN_PROPELLANT_IGNITION_TU = 300f;

    public static final float DEMO_POWER_PER_SQRT = 1.15f, DEMO_POWER_MAX = 8f, DEMO_POWER_MIN = 0.5f,
            DEMO_CONFINE_PER_ST = 0.03f, DEMO_CONFINE_MAX = 1f, DEMO_FIRE_BELOW_BURNED = 0.9f;

    /**
     * One ingot of any metal, in {@link Qu}. One Qu is the amount of metal in a nugget (user ruling
     * 2026-10-28), so an ingot is nine and a block eighty-one; molten, 1 Qu is 16 mB (an ingot is
     * 144 mB).
     */
    public static final Qu QU_PER_INGOT = new Qu(9f);

    /**
     * Placeholder. Electroplating: the window for a coat, in Qu on one piece. Under
     * the first it has not closed (porous); over the second it is too thick to hold and flakes off
     * as it comes out of the water. At a cell's single amp the window is about 12 s to 48 s; at a
     * fine dynamo's two, a quarter of that sooner and narrower. (0.5 and 2 before the nugget
     * definition of Qu, scaled by 0.9 to keep those times.)
     */
    public static final float PLATING_POROUS_BELOW_QU = 0.45f;

    public static final float MASS_PROBE_CONDITION_FLOOR = 0.5f;


    // --- the dynamotor (the modular motor and dynamo, roadmap E5) ---------------------------------

    /**
     * Placeholder. Torque ceiling of the three frames, in Su: what the shaft can carry through the machine at full
     * load. The large frame at a gas turbine's 240 RPM is about 15 kW (1 Su x 1 RPM is 1 W).
     */
    public static final float FRAME_SU_SMALL = 2f, FRAME_SU_MEDIUM = 16f, FRAME_SU_LARGE = 64f;

    /**
     * Anchors untyped {@code Work} to joules: one Work is a kilojoule, so a real specific heat in kJ/(kg K) is a
     * {@code SpecificHeat} unchanged and a latent heat in kJ/kg a {@code LatentHeat}. Author 2026-11-13.
     */
    public static final float JOULES_PER_WORK = 1000f;

    /**
     * Invented. The flow, in mB per tick through one pipe or pump, at which a fluid's {@code shear_thickening} figure is
     * the multiplier its viscosity has reached (read by the pump; the pipe's own flow does not read it).
     */
    public static final float SHEAR_REFERENCE_MB_PER_TICK = 20f;

    /** Invented. The stiffness (1 at rest) past which a pump labouring on a shear-thickening fluid counts as misused. */
    public static final float SHEAR_JAM_STIFFNESS = 3f;

    /** The specific heat of a middling wet food, kJ/(kg K): a food's stated heft is for a piece at this, and a drier piece's heft shrinks with its specific heat. */
    public static final float FOOD_REFERENCE_SPECIFIC_HEAT = 3.5f;

    /** Placeholder. Thermal mass of the three frames, Work per Tu (a motor was 12). */
    public static final ThermalMass FRAME_MASS_SMALL = new ThermalMass(6f), FRAME_MASS_MEDIUM = new ThermalMass(20f),
            FRAME_MASS_LARGE = new ThermalMass(60f);

    /**
     * Placeholder. Amps a winding carries per unit of its conductor's {@code ampacity} (copper 1) per cell of wire in the
     * winding part: a dense winding is four cells, so copper reaches 44 A, past what a large frame lets through at
     * the heavy winding's torque per amp.
     */
    public static final float WINDING_AMPS_PER_AMPACITY = 11f;

    /** Placeholder. Share of throughput a core of a material with no {@code electrical.core_loss} loses as heat. */
    public static final float CORE_LOSS_DEFAULT = 0.15f;

    /** Placeholder. Service temperature of a housing whose material states none, Tu. */
    public static final float MACHINE_SERVICE_TU = 300f;

    /**
     * Placeholders, the lead-acid battery (roadmap E4, tier-by-tier spec in docs/dev/machine.md). A cell holds
     * {@code BATTERY_INGOTS_PER_CELL} ingots of plate and {@code euPerIngot} of its material per ingot; a
     * full battery may be emptied in {@code BATTERY_DISCHARGE_TICKS} (max current is capacity over that) and an
     * empty one fills in {@code BATTERY_CHARGE_TICKS} (charge current likewise). {@code BATTERY_CHARGE_EFFICIENCY}
     * is the share of charged work kept; the rest heats the case. The acid fills at {@code BATTERY_ACID_MB_PER_CELL};
     * the water tank is vented by overcharge (a band over the case's). Self-discharge is the share lost a day, caught up lazily.
     */
    public static final int BATTERY_INGOTS_PER_CELL = 2;

    public static final float BATTERY_CHARGE_EFFICIENCY = 0.85f;

    public static final int BATTERY_ACID_MB_PER_CELL = 250, BATTERY_WATER_TANK_MB = 1000;

    /** Bands of the free charge adjectives (low, part, nearly, full), as a share of capacity. */
    public static final float BATTERY_SOC_LOW = 0.2f, BATTERY_SOC_PART = 0.6f, BATTERY_SOC_NEARLY = 0.9f;

    /** Invented. A hub's suspension band (soft, stock, stiff): the tire's ring frequency, so a banded choice, not a scroll. Stock is {@link VehicleTuning#TIRE_SPRING_HZ}. */
    public static final float[] SUSPENSION_HZ = {2f, VehicleTuning.TIRE_SPRING_HZ, 4.5f};

    public static final float TIRE_DAMPING_RATIO = 0.7f;

    /**
     * Invented. How much of a piece's excess over the room a hand actually feels, through a
     * glove (thick, held in the palm) and through a pair of tongs (a forearm of arm and a grip
     * between the jaws and the hand). A thing is safe to hold while {@code ambient + exposure x
     * (T - ambient)} stays under what the gripping material can stand, so the arithmetic is the
     * material's own figure and not a number printed on the item.
     */
    public static final float GLOVE_EXPOSURE = 0.25f, TONGS_EXPOSURE = 0.1f;

    /** Invented. Horizontal knockback strength of a fence shock. */
    public static final float FENCE_KNOCKBACK = 0.5f;

    /**
     * Invented. Above this many Pu a boiler with a relief valve fitted vents steam. Under
     * {@link VesselTuning#BOILER_SEEP_PU} so the valve lifts before the seams weep; relief only, it never
     * vents below this and never regulates (ruling 2026-10-13).
     */
    public static final Pu BOILER_RELIEF_PU = new Pu(3.0f);

    public static final float SOLDER_BRIDGE_OFFSET_PITCHES = 0.5f;

    /** Invented. The share of a fume dose a worn Respirator lets through. */
    public static final float RESPIRATOR_FUME_SHARE = 0.2f;

    public static final float ENGINE_FUEL_MB_PER_TICK_PER_L = 0.004f, ENGINE_REFERENCE_FLAME_TU = 1300f;

    /** Invented. The thruster: force at the block for a reference fuel (kpg x blocks / s^2, Sable's units) and mB a tick it burns for one. */
    public static final float THRUSTER_FORCE = 20000f, THRUSTER_FUEL_MB_PER_TICK = 1f;

    /** Placeholder. Gravity in blocks per second squared, Sable's default, for weighing thrust against mass. */
    public static final float GRAVITY_BLOCKS_PER_S2 = 9.81f;
}
