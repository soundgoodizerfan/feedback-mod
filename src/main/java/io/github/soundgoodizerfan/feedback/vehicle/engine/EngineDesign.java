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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Material;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An engine as designed on the Engine Stand ({@code feedback_vehicles_spec.md} section 6, v1):
 * four settings, and every figure the engine has derived from them and its material -- nothing
 * stored that could disagree with the settings.
 *
 * <p>What the material contributes is read off the material system ({@link Material}), never a
 * table here: hardness sets how fast the pistons may move, density how heavy the metal is. A
 * figure a material lacks is added to {@code Material}, not to this class.
 *
 * <p>The torque curve is SummerCraft's ({@code CarPhysics}, the author's own): peak torque at the
 * peak RPM, falling off as {@code 1 - 0.55x²} either side, never below 30%.
 *
 * @param boreMm   cylinder width, in mm; a design parameter, not a quantity any system trades in.
 * @param strokeMm how far a piston travels, in mm.
 */
public record EngineDesign(int cylinders, int boreMm, int strokeMm, ResourceLocation material) {

    public static final int MIN_CYLINDERS = 1, MAX_CYLINDERS = 8;
    /** Bore and stroke range and step, in mm. The step size is [OPEN] (spec section 6); 5 mm for now. */
    public static final int MIN_MM = 40, MAX_MM = 120, STEP_MM = 5;

    public static final Codec<EngineDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_CYLINDERS, MAX_CYLINDERS).fieldOf("cylinders").forGetter(EngineDesign::cylinders),
            Codec.intRange(MIN_MM, MAX_MM).fieldOf("bore").forGetter(EngineDesign::boreMm),
            Codec.intRange(MIN_MM, MAX_MM).fieldOf("stroke").forGetter(EngineDesign::strokeMm),
            ResourceLocation.CODEC.fieldOf("material").forGetter(EngineDesign::material)
    ).apply(i, EngineDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EngineDesign::cylinders,
            ByteBufCodecs.VAR_INT, EngineDesign::boreMm,
            ByteBufCodecs.VAR_INT, EngineDesign::strokeMm,
            ResourceLocation.STREAM_CODEC, EngineDesign::material,
            EngineDesign::new);

    /** The settings pulled back into range and onto the step, so a design is always buildable. */
    public EngineDesign {
        cylinders = Mth.clamp(cylinders, MIN_CYLINDERS, MAX_CYLINDERS);
        boreMm = snap(boreMm);
        strokeMm = snap(strokeMm);
    }

    private static int snap(int mm) {
        return Mth.clamp(Math.round(mm / (float) STEP_MM) * STEP_MM, MIN_MM, MAX_MM);
    }

    /** Swept volume of every cylinder, in mB -- litres, since 1 mB is 1 L. */
    public float displacementMb() {
        double bore = boreMm / 1000.0, stroke = strokeMm / 1000.0;
        return (float) (cylinders * Math.PI / 4 * bore * bore * stroke * 1000);
    }

    public Su peakTorque() {
        return new Su(FTuning.ENGINE_SU_PER_MB * displacementMb());
    }

    /** Fastest the pistons may run in this material, over twice the stroke: a short stroke revs higher. */
    public Rpm redline(Material m) {
        float pistonSpeed = FTuning.ENGINE_PISTON_SPEED_PER_CBRT_HARDNESS * (float) Math.cbrt(m.hardness().value());
        return new Rpm(pistonSpeed / (2 * strokeMm / 1000f) * 60);
    }

    public Rpm peakRpm(Material m) {
        return new Rpm(FTuning.ENGINE_PEAK_FRACTION * redline(m).value());
    }

    /** Torque at {@code rpm}: SummerCraft's curve around the peak. */
    public Su torqueAt(Rpm rpm, Material m) {
        float peak = peakRpm(m).value();
        float x = (rpm.value() - peak) / peak;
        return new Su(peakTorque().value() * Mth.clamp(1 - 0.55f * x * x, 0.3f, 1f));
    }

    /** The engine's own weight: its metal times the material's density. Zero for a material nobody has weighed. */
    public Mu mass(Material m) {
        return new Mu(FTuning.ENGINE_METAL_PER_MB * displacementMb() * m.density().muPerMb());
    }

    /** A first design for an empty stand: a 1.6 L-ish inline four. */
    public static EngineDesign starter(ResourceLocation material) {
        return new EngineDesign(4, 80, 80, material);
    }
}
