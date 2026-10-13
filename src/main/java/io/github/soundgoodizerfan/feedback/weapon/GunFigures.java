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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A gun's baked figures ({@code feedback_guns_spec.md} section 2): derived once from a
 * {@link GunDesign} and its materials, carried on the item, never edited.
 *
 * <p>Everything a shot needs is here, so the client can predict a shot and the server can
 * resolve it from the same record with no lookup by id. Typed where a figure crosses a system
 * seam (pressure, mass); the ballistic floats stay plain, as a vehicle's internal constants do
 * (vehicles spec section 2) -- speed and impulse never leave the weapon package.
 *
 * @param peakPressure   what the charge makes in this case.
 * @param chamberRating  the most the chamber holds. {@link #overpressure} is the ratio.
 * @param muzzleSpeed    blocks per tick.
 * @param muzzleEnergy   Work delivered to the projectile at the muzzle.
 * @param projectileMass the projectile.
 * @param gunMass        the gun's own metal.
 * @param impulse        momentum of projectile and gas, kg m/s: what recoil and kick are made from.
 * @param cycleMs        time between shots, in ms -- wall-clock, not ticks (spec 7.1).
 * @param spreadDeg      cone half-angle at the muzzle; has a floor that is never zero.
 * @param kickDeg        camera kick per shot, in degrees.
 */
public record GunFigures(Pu peakPressure, Pu chamberRating, float muzzleSpeed, float muzzleEnergy,
                         Mu projectileMass, Mu gunMass, float impulse, int cycleMs, float spreadDeg,
                         float kickDeg) {

    public static final Codec<GunFigures> CODEC = RecordCodecBuilder.create(i -> i.group(
            Units.codec(Pu::new).fieldOf("peak_pressure").forGetter(GunFigures::peakPressure),
            Units.codec(Pu::new).fieldOf("chamber_rating").forGetter(GunFigures::chamberRating),
            Codec.FLOAT.fieldOf("muzzle_speed").forGetter(GunFigures::muzzleSpeed),
            Codec.FLOAT.fieldOf("muzzle_energy").forGetter(GunFigures::muzzleEnergy),
            Units.codec(Mu::new).fieldOf("projectile_mass").forGetter(GunFigures::projectileMass),
            Units.codec(Mu::new).fieldOf("gun_mass").forGetter(GunFigures::gunMass),
            Codec.FLOAT.fieldOf("impulse").forGetter(GunFigures::impulse),
            Codec.INT.fieldOf("cycle_ms").forGetter(GunFigures::cycleMs),
            Codec.FLOAT.fieldOf("spread").forGetter(GunFigures::spreadDeg),
            Codec.FLOAT.fieldOf("kick").forGetter(GunFigures::kickDeg)
    ).apply(i, GunFigures::new));

    /** Ten fields is past {@code StreamCodec.composite}'s six, so by hand, in field order. */
    public static final StreamCodec<ByteBuf, GunFigures> STREAM_CODEC = StreamCodec.of(
            (buf, f) -> {
                Units.streamCodec(Pu::new).encode(buf, f.peakPressure);
                Units.streamCodec(Pu::new).encode(buf, f.chamberRating);
                buf.writeFloat(f.muzzleSpeed);
                buf.writeFloat(f.muzzleEnergy);
                Units.streamCodec(Mu::new).encode(buf, f.projectileMass);
                Units.streamCodec(Mu::new).encode(buf, f.gunMass);
                buf.writeFloat(f.impulse);
                ByteBufCodecs.VAR_INT.encode(buf, f.cycleMs);
                buf.writeFloat(f.spreadDeg);
                buf.writeFloat(f.kickDeg);
            },
            buf -> new GunFigures(
                    Units.streamCodec(Pu::new).decode(buf), Units.streamCodec(Pu::new).decode(buf),
                    buf.readFloat(), buf.readFloat(),
                    Units.streamCodec(Mu::new).decode(buf), Units.streamCodec(Mu::new).decode(buf),
                    buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf), buf.readFloat(), buf.readFloat()));

    public GunFigures withSpread(float multiplier) {
        return new GunFigures(peakPressure, chamberRating, muzzleSpeed, muzzleEnergy, projectileMass, gunMass, impulse,
                cycleMs, spreadDeg * multiplier, kickDeg);
    }

    /** Work to warm the gun's metal by one Tu: what a shot's heat is divided by. */
    public ThermalMass thermalMass() {
        return new ThermalMass(Math.max(1f, gunMass.value() * FTuning.GUN_THERMAL_MASS_PER_MU));
    }

    /** Tu a hot barrel sheds per tick: the leak over the thermal mass, so heavy barrels cool slowly. */
    public TuRate coolingRate() {
        return new TuRate(FTuning.GUN_COOLING_WORK_PER_TICK / thermalMass().workPerTu());
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
                cycleMs, spread, kickDeg);
    }

    /** Peak pressure over the chamber's rating. Above 1 the force goes into the gun (spec 3.2). */
    public float overpressure() {
        float rating = chamberRating.value();
        return rating <= 0 ? Float.POSITIVE_INFINITY : peakPressure.value() / rating;
    }
}
