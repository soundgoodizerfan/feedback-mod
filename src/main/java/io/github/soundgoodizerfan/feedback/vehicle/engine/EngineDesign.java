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
 * An engine as designed on the Engine Stand ({@code docs/specs/feedback_vehicles_spec.md} section 6, v1):
 * four settings, and every figure the engine has derived from them and its material -- nothing
 * stored that could disagree with the settings.
 *
 * <p>What the material contributes is read off the material system ({@link Material}), never a
 * table here: yield strength over density sets how fast the pistons may move, density how heavy the metal is. A
 * figure a material lacks is added to {@code Material}, not to this class.
 *
 * <p>The torque curve is SummerCraft's ({@code CarPhysics}, the author's own): peak torque at the
 * peak RPM, falling off as {@code 1 - 0.55x²} either side, never below 30%.
 *
 * @param boreMm   cylinder width, in mm; a design parameter, not a quantity any system trades in.
 * @param strokeMm how far a piston travels, in mm.
 */
public record EngineDesign(int cylinders, int boreMm, int strokeMm, ResourceLocation material, Compression compression) {

    /**
     * The compression ratio, a part choice (cylinder head and piston crown; petrochemistry spec 9.1).
     * A fuel igniting under {@code knockBelow} fires before the spark: knock, which wears condition. A
     * fuel igniting over {@code misfireAbove} will not light: no power, and the fuel floods through.
     * Either way the engine runs worse; nothing is refused.
     */
    public enum Compression implements net.minecraft.util.StringRepresentable {
        LOW(0f, FTuning.ENGINE_LOW_MISFIRE_ABOVE_TU),
        MEDIUM(FTuning.ENGINE_MEDIUM_KNOCK_BELOW_TU, FTuning.ENGINE_MEDIUM_MISFIRE_ABOVE_TU),
        HIGH(FTuning.ENGINE_HIGH_KNOCK_BELOW_TU, Float.MAX_VALUE);

        public static final Codec<Compression> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Compression::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Compression> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Compression::ordinal).mapStream(b -> b);

        public final float knockBelow, misfireAbove;

        Compression(float knockBelow, float misfireAbove) {
            this.knockBelow = knockBelow;
            this.misfireAbove = misfireAbove;
        }

        public Compression step(int dir) {
            return values()[Math.floorMod(ordinal() + dir, values().length)];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A design with the middle compression: what every design before the part existed was. */
    public EngineDesign(int cylinders, int boreMm, int strokeMm, ResourceLocation material) {
        this(cylinders, boreMm, strokeMm, material, Compression.MEDIUM);
    }


    public static final int MIN_CYLINDERS = 1, MAX_CYLINDERS = 8;
    /** Bore and stroke range and step, in mm. The step size is [OPEN] (spec section 6); 5 mm for now. */
    public static final int MIN_MM = 40, MAX_MM = 120, STEP_MM = 5;

    public static final Codec<EngineDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_CYLINDERS, MAX_CYLINDERS).fieldOf("cylinders").forGetter(EngineDesign::cylinders),
            Codec.intRange(MIN_MM, MAX_MM).fieldOf("bore").forGetter(EngineDesign::boreMm),
            Codec.intRange(MIN_MM, MAX_MM).fieldOf("stroke").forGetter(EngineDesign::strokeMm),
            ResourceLocation.CODEC.fieldOf("material").forGetter(EngineDesign::material),
            Compression.CODEC.optionalFieldOf("compression", Compression.MEDIUM).forGetter(EngineDesign::compression)
    ).apply(i, EngineDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EngineDesign::cylinders,
            ByteBufCodecs.VAR_INT, EngineDesign::boreMm,
            ByteBufCodecs.VAR_INT, EngineDesign::strokeMm,
            ResourceLocation.STREAM_CODEC, EngineDesign::material,
            Compression.STREAM_CODEC, EngineDesign::compression,
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

    /**
     * Fastest the pistons may run in this material, over twice the stroke: a short stroke revs higher.
     * A piston's own inertia loads it as density x speed², so the speed it survives goes as
     * sqrt(yield strength / density) -- a light strong metal revs highest. Hardness was used first,
     * only because it was the one figure every material had (spec section 6).
     *
     * <p>A material with no measured yield strength, or no density, has no rev limit to stand
     * behind, so it redlines at zero and makes no torque rather than borrowing a default figure;
     * the stand does not offer it.
     */
    public Rpm redline(Material m) {
        var yield = m.mechanical().yieldStrength();
        if (yield.isEmpty() || m.density().muPerMb() <= 0)
            return new Rpm(0f);
        float pistonSpeed = FTuning.ENGINE_PISTON_SPEED_PER_SQRT_SPECIFIC_YIELD
                * (float) Math.sqrt(yield.get().value() / m.density().muPerMb());
        return new Rpm(pistonSpeed / (2 * strokeMm / 1000f) * 60);
    }

    public Rpm peakRpm(Material m) {
        return new Rpm(FTuning.ENGINE_PEAK_FRACTION * redline(m).value());
    }

    /** Torque at {@code rpm}: SummerCraft's curve around the peak. */
    public Su torqueAt(Rpm rpm, Material m) {
        float peak = peakRpm(m).value();
        if (peak <= 0)
            return new Su(0f);
        float x = (rpm.value() - peak) / peak;
        return new Su(peakTorque().value() * Mth.clamp(1 - 0.55f * x * x, 0.3f, 1f));
    }

    /** The engine's own weight: its metal times the material's density. Zero for a material nobody has weighed. */
    public Mu mass(Material m) {
        return new Mu(FTuning.ENGINE_METAL_PER_MB * displacementMb() * m.density().muPerMb());
    }

    /**
     * Tooltip and screen lines: what was chosen, then what it makes. These are an engine's spec
     * sheet -- philosophy 8 gives equipment specs away free and exact.
     */
    public java.util.List<net.minecraft.network.chat.Component> describe(@org.jetbrains.annotations.Nullable Material m) {
        var out = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.settings", cylinders, boreMm, strokeMm,
                net.minecraft.network.chat.Component.translatable("material." + material.getNamespace() + "." + material.getPath())));
        out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.displacement", String.format("%.2f", displacementMb())));
        if (m == null) {
            out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.unknown_material"));
            return out;
        }
        out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.torque",
                Math.round(peakTorque().value()), Math.round(peakRpm(m).value())));
        out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.redline", Math.round(redline(m).value())));
        out.add(net.minecraft.network.chat.Component.translatable("engine.feedback.mass", Math.round(mass(m).value())));
        return out;
    }

    /** Ingots of its material it takes to build. */
    public int ingots() {
        return cylinders * FTuning.ENGINE_INGOTS_PER_CYLINDER;
    }

    /**
     * Whether an engine can be made of {@code m}: a metal, weighed, with a yield strength to rev
     * to and an ingot to pay in. One rule, asked by the stand's sample slot, its screen and the
     * stand itself -- every figure from the material system.
     */
    public static boolean buildable(ResourceLocation id, Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0
                && m.mechanical().yieldStrength().isPresent() && m.item(id, "ingot").isPresent();
    }

    /** The material a sample item names for an engine -- any form of a buildable metal -- if any. */
    public static java.util.Optional<ResourceLocation> sampleMaterial(java.util.Map<ResourceLocation, Material> materials,
                                                                      net.minecraft.world.item.ItemStack sample) {
        if (sample.isEmpty())
            return java.util.Optional.empty();
        return Material.find(materials, sample.getItem()).filter(l -> buildable(l.id(), l.material())).map(Material.Located::id);
    }

    public EngineDesign withMaterial(ResourceLocation m) {
        return new EngineDesign(cylinders, boreMm, strokeMm, m, compression);
    }

    /** A first design for an empty stand: a 1.6 L-ish inline four. */
    public static EngineDesign starter(ResourceLocation material) {
        return new EngineDesign(4, 80, 80, material);
    }
}
