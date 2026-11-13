// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.combat.gear.GearTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * What a gun fitting does, from the metal it is machined in and the geometry its file gives. Nothing here is a flat number a
 * file states: a suppressor's baffles pull heat from the gas as fast as the metal conducts it and hold only as much as their
 * mass, so a copper stack quiets best and a lead one softens first; a brake's ports are cut by the gas, so its hardness against
 * {@link GunTuning#FIT_HARD_MATCH} is how fast it wears the gun; a shroud sheds heat by conductivity over its mass; a vent opens
 * where its metal yields against the barrel's.
 *
 * @param ventYield   the yield strength of a relief vent's seat (St), or {@code Float.MAX_VALUE} for no vent.
 * @param bleed       share of the shot's work a vent leaks.
 * @param gasCut      share of the gas's part of the recoil a brake throws away.
 * @param blast       multiplier on the gas's part of the report.
 * @param heat        multiplier on the heat a shot leaves in the gun.
 * @param shed        multiplier on how fast the barrel sheds heat.
 * @param eddy        share of a coupled bullet's muzzle energy drained, times the metal's ampacity.
 * @param erosion     wear a shot adds to the gun (a share of its life) when this fitting is worked; a vent counts only once it opens
 *                    ({@link #ventErosion}).
 * @param ventErosion what an opening vent adds on top, per shot that opens it.
 * @param serviceTu   the heat at which the fitting starts to soften.
 * @param meltTu      the heat at which it runs off the gun.
 */
public record FittingPhysics(float ventYield, float bleed, float gasCut, float blast, float heat, float shed, float eddy,
                             float erosion, float ventErosion, float serviceTu, float meltTu) {

    /** A fitting that does nothing to the figures. */
    public static final FittingPhysics NONE = new FittingPhysics(Float.MAX_VALUE, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f,
            Float.MAX_VALUE, Float.MAX_VALUE);

    /**
     * Whether {@code m} can be {@code option}'s metal: a gun metal that states what the fitting reads. A suppressor's baffles and a
     * shroud's fins need a stated conductivity; every fitting needs a service and a melting temperature for the heat rule; an eddy
     * sleeve needs a conductor.
     */
    public static boolean suits(GunOption option, Material m) {
        if (!GunSpec.buildable(m) || m.thermal().serviceTemperature().isEmpty() || m.thermal().meltingPoint().isEmpty())
            return false;
        return switch (option.form()) {
            case "suppressor", "barrel_shroud" -> m.thermal().conductivity().isPresent();
            case "eddy_sleeve" -> m.ampacity().isPresent();
            default -> true;
        };
    }

    /** The fitting {@code option} machined in {@code m}. */
    public static FittingPhysics of(GunOption option, Material m) {
        float mu = option.metalMb() * m.density().muPerMb();
        float take = GearTuning.saturate(m.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f), GunTuning.FIT_K_HALF);
        float hold = GearTuning.saturate(mu, GunTuning.FIT_MU_HALF);
        float mismatch = 1f - Math.min(1f, m.hardness().value() / GunTuning.FIT_HARD_MATCH);
        float wear = GunTuning.FIT_WEAR_FLOOR + GunTuning.FIT_WEAR_WORST * mismatch * mismatch;
        float service = m.thermal().serviceTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE);
        float melt = m.thermal().meltingPoint().map(t -> t.value()).orElse(Float.MAX_VALUE);
        FittingPhysics base = new FittingPhysics(Float.MAX_VALUE, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, service, melt);
        return switch (option.form()) {
            case "suppressor" -> {
                float cut = GunTuning.SUPP_MAX_CUT * take * hold;
                yield base.with(0f, 0f, 1f - cut, 1f + GunTuning.SUPP_HEAT_PER_CUT * cut, 1f, 0f, 0f, 0f);
            }
            case "muzzle_brake" -> {
                float cut = GunTuning.BRAKE_MAX_CUT * GearTuning.saturate(option.param("port_area", 1f), 0.5f);
                yield base.with(0f, cut, 1f + GunTuning.BRAKE_LOUD * cut, 1f, 1f, 0f, wear * cut, 0f);
            }
            case "barrel_shroud" -> base.with(0f, 0f, 1f, 1f,
                    1f + GunTuning.SHROUD_MAX_GAIN * take * GearTuning.saturate(mu * option.param("fin_area", 1f), GunTuning.FIT_MU_HALF), 0f, 0f, 0f);
            case "relief_vent" -> {
                float yield = m.mechanical().yieldStrength().map(y -> y.value()).orElse(0f);
                float leak = GunTuning.VENT_LEAK * (1f - 0.7f * GearTuning.saturate(m.hardness().value(), GunTuning.VENT_SEAT_HARD_HALF));
                yield new FittingPhysics(yield, leak, 0f, 1f, 1f, 1f, 0f, 0f, wear * option.param("port_area", 1f), service, melt);
            }
            case "eddy_sleeve" -> base.with(0f, 0f, 1f, 1f, 1f, option.param("eddy", 0f) * m.ampacity().orElse(0f), 0f, 0f);
            default -> base;
        };
    }

    private FittingPhysics with(float bleed, float gasCutOrZero, float blast, float heat, float shed, float eddy, float erosion, float ventErosion) {
        return new FittingPhysics(ventYield, bleed, gasCutOrZero, blast, heat, shed, eddy, erosion, ventErosion, serviceTu, meltTu);
    }

    /**
     * Where a relief vent opens, as a share of the chamber's rating: a seat as strong as the barrel opens at
     * {@link GunTuning#VENT_OPEN_AT_EQUAL}, a weaker one earlier, a stronger one no later than {@link GunTuning#VENT_OPEN_MAX};
     * {@code Float.MAX_VALUE} when there is no vent.
     */
    public float reliefAt(float barrelYield) {
        if (ventYield == Float.MAX_VALUE)
            return Float.MAX_VALUE;
        return Math.max(GunTuning.VENT_OPEN_MIN, Math.min(GunTuning.VENT_OPEN_MAX,
                GunTuning.VENT_OPEN_AT_EQUAL * ventYield / Math.max(0.01f, barrelYield)));
    }

    /**
     * How much of the fitting still works at {@code barrelTu}: 1 up to its service temperature, falling to 0 at its melting
     * point (where it runs off the gun).
     */
    public float soundness(float barrelTu) {
        if (barrelTu <= serviceTu)
            return 1f;
        if (meltTu <= serviceTu)
            return 0f;
        return Math.max(0f, 1f - (barrelTu - serviceTu) / (meltTu - serviceTu));
    }
}
