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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import java.util.EnumMap;
import java.util.Map;

/**
 * A gun's figures as the shot logic, the HUD and the spec sheet read them: a read-only view over
 * the gun {@link Fig}s of {@link ModularFigures} (the stack's baked figures) or over one bake against
 * a round ({@link GunSpec#bake}). Nothing is stored in this shape any more -- the figures are
 * {@code ModularFigures} like a pick's, and this record is what a shot wants without asking
 * for each by enum.
 *
 * <p>Typed where a figure crosses a system seam (pressure, mass); the ballistic floats stay plain.
 *
 * @param peakPressure   what the charge makes in this case.
 * @param chamberRating  the most the chamber holds. {@link #overpressure} is the ratio.
 * @param muzzleSpeed    blocks per tick.
 * @param muzzleEnergy   Work delivered to the projectile at the muzzle.
 * @param projectileMass the projectile.
 * @param gunMass        the gun's own metal.
 * @param impulse        momentum of projectile and gas, kg m/s: what recoil and kick are made from.
 * @param cycleMs        time between shots, in ms -- wall-clock, not ticks (guns spec 7.1).
 * @param spreadDeg      cone half-angle at the muzzle; has a floor that is never zero.
 * @param kickDeg        camera kick per shot, in degrees.
 * @param launcher       what throws the projectile: the sound, the muzzle and the heat limit differ with it.
 * @param report         how loud the shot is against an open muzzle: a suppressor quiets it, a
 *                       brake makes it louder. The sound's volume and the vibration read it.
 * @param heatFactor     heat a shot leaves in the gun against an open muzzle's: a suppressor keeps the gas's.
 * @param leakFactor     how fast the gun sheds heat against a bare barrel's: a shroud's fins.
 */
public record GunFigures(Pu peakPressure, Pu chamberRating, float muzzleSpeed, float muzzleEnergy,
                         Mu projectileMass, Mu gunMass, float impulse, int cycleMs, float spreadDeg,
                         float kickDeg, GunSpec.Launcher launcher, float report, float heatFactor, float leakFactor) {

    /** The gun figures of a baked item, or null if it has none (a pick, or a gun not yet baked). */
    public static GunFigures of(ModularFigures f) {
        if (f == null || f.get(Fig.CHAMBER_RATING) <= 0)
            return null;
        return new GunFigures(new Pu(f.get(Fig.PEAK_PRESSURE)), new Pu(f.get(Fig.CHAMBER_RATING)), f.get(Fig.MUZZLE_SPEED),
                f.get(Fig.MUZZLE_ENERGY), new Mu(f.get(Fig.BULLET_MASS)), new Mu(f.get(Fig.MASS)), f.get(Fig.IMPULSE),
                Math.round(f.get(Fig.CYCLE)), f.get(Fig.SPREAD), f.get(Fig.KICK), GunSpec.Launcher.values()[(int) f.get(Fig.LAUNCHER)],
                f.get(Fig.REPORT), f.get(Fig.HEAT_FACTOR), f.get(Fig.SHED_FACTOR));
    }

    /** These figures as {@link Fig} entries: how the bake of a gun type fills {@link ModularFigures}. */
    public Map<Fig, Float> figs() {
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        f.put(Fig.PEAK_PRESSURE, peakPressure.value());
        f.put(Fig.CHAMBER_RATING, chamberRating.value());
        f.put(Fig.MUZZLE_SPEED, muzzleSpeed);
        f.put(Fig.MUZZLE_ENERGY, muzzleEnergy);
        f.put(Fig.BULLET_MASS, projectileMass.value());
        f.put(Fig.MASS, gunMass.value());
        f.put(Fig.IMPULSE, impulse);
        f.put(Fig.CYCLE, (float) cycleMs);
        f.put(Fig.SPREAD, spreadDeg);
        f.put(Fig.KICK, kickDeg);
        f.put(Fig.LAUNCHER, (float) launcher.ordinal());
        f.put(Fig.REPORT, report);
        f.put(Fig.HEAT_FACTOR, heatFactor);
        f.put(Fig.SHED_FACTOR, leakFactor);
        f.put(Fig.OVERPRESSURE, Float.isInfinite(overpressure()) ? 99f : overpressure());
        return f;
    }

    /**
     * One pellet of {@code n}: the projectile and its energy shared out, the speed the same, in a
     * cone at least shot's own width.
     */
    public GunFigures asPellet(int n) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy / n,
                new Mu(projectileMass.value() / n), gunMass, impulse, cycleMs,
                Math.max(spreadDeg, FTuning.GUN_SHOT_SPREAD_DEG), kickDeg, launcher, report, heatFactor, leakFactor);
    }

    public GunFigures withSpread(float multiplier) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                cycleMs, spreadDeg * multiplier, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    /** These figures with the kick, the chamber's rating, the heat a shot leaves, the cycle or the gun's mass changed: what a trait reshapes. */
    public GunFigures withKick(float multiplier) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                cycleMs, spreadDeg, kickDeg * multiplier, launcher, report, heatFactor, leakFactor);
    }

    public GunFigures withRating(float multiplier) {
        return new GunFigures(peakPressure, new Pu(chamberRating.value() * multiplier), muzzleSpeed, muzzleEnergy, projectileMass,
                gunMass, impulse, cycleMs, spreadDeg, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    public GunFigures withHeat(float multiplier) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                cycleMs, spreadDeg, kickDeg, launcher, report, heatFactor * multiplier, leakFactor);
    }

    public GunFigures withCycle(float multiplier) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                Math.round(cycleMs * multiplier), spreadDeg, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    public GunFigures withCycleMs(int ms) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                ms, spreadDeg, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    /** Thrown by a field, not a gas or a spring. */
    public boolean coil() {
        return launcher == GunSpec.Launcher.COIL;
    }

    /** What shows at the muzzle: smoke from a gas, a crackle from a field, a flick of dust from a spring. */
    public net.minecraft.core.particles.SimpleParticleType muzzleParticle() {
        return switch (launcher) {
            case COIL -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
            case SPRING -> net.minecraft.core.particles.ParticleTypes.CRIT;
            case STEAM -> net.minecraft.core.particles.ParticleTypes.CLOUD;
            case CHAMBER -> net.minecraft.core.particles.ParticleTypes.SMOKE;
        };
    }

    /** Recoil speed of the gun, m/s: impulse over its own mass. */
    public float recoilSpeed() {
        return impulse / Math.max(0.01f, gunMass.value());
    }

    /** Whether a hand alone can hold this gun: light enough to carry at arm's length, soft enough to recoil against (guns spec 7.4). */
    public boolean oneHandable() {
        return gunMass.value() <= FTuning.GUN_ONE_HAND_MAX_MU && recoilSpeed() <= FTuning.GUN_ONE_HAND_MAX_RECOIL_MS;
    }

    /** Fired with one hand: spread and kick grow with how much of the hand's recoil limit the gun uses. */
    public GunFigures oneHanded() {
        float load = Math.min(1f, recoilSpeed() / FTuning.GUN_ONE_HAND_MAX_RECOIL_MS);
        return withSpread(1 + FTuning.GUN_ONE_HAND_SPREAD * load).withKick(1 + FTuning.GUN_ONE_HAND_KICK * load);
    }

    public GunFigures withMass(float addedMu) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass,
                new Mu(gunMass.value() + addedMu), impulse, cycleMs, spreadDeg, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    /** Work to warm the gun's metal by one Tu: what a shot's heat is divided by. */
    public ThermalMass thermalMass() {
        return new ThermalMass(Math.max(1f, gunMass.value() * FTuning.GUN_THERMAL_MASS_PER_MU));
    }

    /** Tu a hot barrel sheds per tick: the leak over the thermal mass, so heavy barrels cool slowly. */
    public TuRate coolingRate() {
        return new TuRate(FTuning.GUN_COOLING_WORK_PER_TICK * leakFactor / thermalMass().workPerTu());
    }

    /**
     * This gun as it is now, not as it was built: a worn bore delivers less and sprays wider, a
     * hot barrel sprays wider. Pure, so the client predicts the same figures the server fires.
     *
     * @param condition 1 as built, down to {@link FTuning#GUN_CONDITION_FLOOR}.
     * @param heat      the barrel's temperature as a fraction of the round's ignition temperature.
     */
    public GunFigures aged(float condition, float heat) {
        float energy = muzzleEnergy * (1 - FTuning.GUN_WEAR_ENERGY_LOSS * (1 - condition) / (1 - FTuning.GUN_CONDITION_FLOOR));
        float speed = muzzleSpeed * (float) Math.sqrt(energy / muzzleEnergy);
        float spread = spreadDeg / condition * (1 + FTuning.GUN_BLOOM_AT_IGNITION * Math.max(0f, heat));
        return new GunFigures(peakPressure, chamberRating, speed, energy, projectileMass, gunMass, impulse,
                cycleMs, spread, kickDeg, launcher, report, heatFactor, leakFactor);
    }

    /** Peak pressure over the chamber's rating. Above 1 the force goes into the gun (spec 3.2). */
    public float overpressure() {
        float rating = chamberRating.value();
        return rating <= 0 ? Float.POSITIVE_INFINITY : peakPressure.value() / rating;
    }
}
