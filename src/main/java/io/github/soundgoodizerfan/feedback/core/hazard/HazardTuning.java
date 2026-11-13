// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

/**
 * Tuning constants for the core/hazard code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class HazardTuning {

    private HazardTuning() {
    }
    /** Placeholder. Damage a bare, loaded conductor does to what touches it, twice a second. */
    public static final float BARE_WIRE_SHOCK_DAMAGE = 1f;


    // --- dark cell (docs/specs/feedback_automation_spec.md, section 4). Every figure here is invented. ---

    /** Invented. Radiation a contaminated drop gives per item, Ru. */
    public static final float CONTAMINATED_DROP_RU = 0.05f;

    public static final Tu CONTACT_BURN_TU = new Tu(300f);

    public static final Tu CONTACT_SEAR_TU = new Tu(700f);

    /** Invented. Damage a band does per application, in hearts' halves. */
    public static final float CONTACT_PAIN_DAMAGE = 1f, CONTACT_BURN_DAMAGE = 2f, CONTACT_SEAR_DAMAGE = 4f;

    /** Invented. Seconds of fire a burn and a sear set, a scald sets none. */
    public static final int CONTACT_BURN_IGNITE_SECONDS = 2, CONTACT_SEAR_IGNITE_SECONDS = 4;

    /** Invented. Twice a second, as the bare wire's shock already is: a hazard and not a death trap. */
    public static final int CONTACT_INTERVAL_TICKS = 10;
    /** The share of a held piece's heat above ambient the hand takes with each burn. */
    public static final float CONTACT_HEAT_TAKEN = 0.15f;

    /**
     * Invented. A grip insulates when it conducts heat worse than this, on the same scale as a
     * material's {@code conductivity} (copper is {@link FTuning#HEAT_EXCHANGER_CONDUCTANCE}'s 0.1, wood
     * and leather are 3.75e-5). A property, not an identity: a metal grip simply is not one.
     */
    public static final float GRIP_MAX_CONDUCTIVITY = 0.001f;

    /**
     * Invented. Damage per shock by band, LV up. LV keeps {@link #BARE_WIRE_SHOCK_DAMAGE}: the
     * wire's old single figure is the bottom band rather than a second rule.
     */
    public static final float SHOCK_MV_DAMAGE = 2f, SHOCK_HV_DAMAGE = 5f, SHOCK_UHV_DAMAGE = 10f, SHOCK_EHV_DAMAGE = 20f, SHOCK_TRUNK_DAMAGE = 40f;

    /** Invented. A discharge counts for at least this many packets' worth of Eu in a shock's damage, and at most this many. */
    public static final float SHOCK_MIN_PACKETS = 0.25f, SHOCK_MAX_PACKETS = 4f;

    /** Invented. Ceiling of {@code Exposure}'s multiplier on a shock. */
    public static final float FENCE_EXPOSURE_MAX = 2f;

    /** Invented. Wet (in water or rain) multiplies a shock by this. */
    public static final float FENCE_WET_FACTOR = 1.5f;

    /** Invented. Worn armour: all four slots conductive adds this, all four insulating takes it off. */
    public static final float FENCE_ARMOUR_GAIN = 0.5f, FENCE_ARMOUR_LOSS = 0.5f;

    /** Invented. Standing on a conductive floor, an insulating floor, or on nothing at all. */
    public static final float FENCE_GROUND_CONDUCTIVE = 1.25f, FENCE_GROUND_INSULATING = 0.5f,
            FENCE_AIRBORNE_FACTOR = 0.5f;

    /** Invented. Electrical conductivity (copper 1) at or above which a body counts as conducting. */
    public static final float FENCE_CONDUCTIVE_MIN = 0.1f;

    /** Invented. Blocks around a weeping or bursting boiler that its steam reaches. */
    public static final float STEAM_SCALD_REACH = 1.6f;

    public static final float STEAM_BURST_REACH = 4f;

    /** Invented. Damage of a scald per application; a burst is a sear's worth and a half. */
    public static final float STEAM_SCALD_DAMAGE = 2f;

    public static final float STEAM_BURST_DAMAGE = 8f;

    /** Placeholder. A hot item on the ground lights a flammable neighbour once its stamp is at least this, checked every this many ticks. */
    public static final float HOT_IGNITE_TU = 300f;

    public static final int HOT_IGNITE_INTERVAL_TICKS = 10;

    /** Placeholder. Fumes: radius in blocks around hot toxic solder, ticks of poison per level of the toxic perk, ticks between doses. */
    public static final float FUME_RADIUS_BLOCKS = 3f;

    public static final int FUME_POISON_TICKS_PER_LEVEL = 100;

    /**
     * Placeholder. Fume cloud (a volatile toxic gas such as osmium tetroxide): ticks a cloud lasts per level of the toxic perk, ticks between doses,
     * exposure gained per dose per level, exposure lost per second outside, the exposure at which nausea, blindness and damage begin, and the damage
     * per dose (never taken below {@link #FUME_CLOUD_DAMAGE_FLOOR_HEALTH}, so the cloud hurts and never kills).
     */
    public static final int FUME_CLOUD_TICKS_PER_LEVEL = 200;

    public static final int FUME_CLOUD_INTERVAL_TICKS = 20;

    public static final float FUME_CLOUD_DOSE_PER_LEVEL = 0.5f;

    public static final float FUME_CLOUD_DECAY_PER_SECOND = 2f;

    public static final float FUME_CLOUD_NAUSEA = 4f, FUME_CLOUD_BLIND = 8f, FUME_CLOUD_HURT = 12f;

    public static final float FUME_CLOUD_DAMAGE = 1f, FUME_CLOUD_DAMAGE_FLOOR_HEALTH = 2f;


    // --- radiation: every figure below is a placeholder ----------------------------

    /** Placeholder. How far a block or a dropped item still counts toward a body's dose rate; a source gives 1 / (1 + d^2) of its figure. */
    public static final int RAD_REACH_BLOCKS = 5;

    /** Placeholder. Ticks between a player's dose being worked out (the server's tick; a Geiger counter reads its own 5 times a second). */
    public static final int RAD_INTERVAL_TICKS = 20;

    /** Placeholder. Dose (Ru x s) a body clears each second: a source under this in Ru does nothing however long it stays. */
    public static final float RAD_CLEARANCE = 0.2f;

    /** Placeholder. Dose at which the body complains, in order: sick (Hunger), ill (Weakness and Mining Fatigue), and the third (Wither, which is harmful but not itself a death sentence). */
    public static final float RAD_SICK_DOSE = 300f, RAD_ILL_DOSE = 900f, RAD_LETHAL_DOSE = 2400f;

    /** Placeholder. How hard worn armour shields: dose rate over (1 + this x the four slots' mean density / 10), a slot bare counting nothing. */
    public static final float RAD_SHIELD_GAIN = 6f;

    /** Placeholder. Shielding by density (density / 10 times this, then 1 / (1 + that)): per block of a wall along the line, and for the lining of a container. */
    public static final float RAD_WALL_GAIN = 1.5f, RAD_CONTAINER_GAIN = 30f;

    /** Invented. Vapour cloud: ticks between steps, the share of a source's potency it emits per step, the cap and floor of a cell, the share handed to neighbours, the decay per step. */
    public static final int VAPOUR_INTERVAL_TICKS = 10;

    public static final float VAPOUR_EMIT = 0.3f, VAPOUR_CAP = 1f, VAPOUR_FLOOR = 0.02f, VAPOUR_SPREAD = 0.5f, VAPOUR_DECAY = 0.9f;

    /** Invented. A flash fire: seconds a victim burns and damage at full concentration. */
    public static final int VAPOUR_FLASH_FIRE_SECONDS = 4;

    public static final float VAPOUR_FLASH_DAMAGE = 6f;

    /** Invented. A spatially unstable fluid let loose (core/hazard/SpatialHop): the radius (blocks) around the leak whose items and creatures may hop, the nearest and farthest hop (blocks), the chance at nothing lost, and the mB lost that adds a full point of chance. */
    public static final float SPATIAL_HOP_RADIUS = 3f, SPATIAL_HOP_MIN = 2f, SPATIAL_HOP_MAX = 6f, SPATIAL_HOP_BASE_CHANCE = 0.2f;

    public static final int SPATIAL_HOP_MB_FOR_CERTAIN = 125;

    /** Invented. The electrolyser's lining (glass and plate) resists a corrosive liquid to this degree; a vessel is bitten when its resistance is below {@link #CORROSIVE_BITE}, by {@code CORROSION_WEAR_PER_SECOND} of condition a second times the shortfall. */
    public static final float ELECTROLYSER_LINING = 0.95f, CORROSIVE_BITE = 0.9f, CORROSION_WEAR_PER_SECOND = 0.0005f;
}
