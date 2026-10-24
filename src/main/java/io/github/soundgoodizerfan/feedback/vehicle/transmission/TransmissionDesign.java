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
package io.github.soundgoodizerfan.feedback.vehicle.transmission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * A transmission as designed on the Car Design Stand ({@code docs/feedback_vehicles_spec.md} section
 * 6): how many gears, and each one's ratio, in steps of 0.05 ({@link #PER_UNIT} per 1.0, stored as a
 * whole number so two designs compare equal). A ratio is input turns per output turn: 3.5 is a low
 * gear, 0.8 an overdrive. There is also neutral (nothing coupled) and a reverse the size of first.
 * Materials set how fast it wears when shifted at speed ({@link FTuning#TRANSMISSION_WEAR_PER_RPM}).
 */
public record TransmissionDesign(List<Integer> steps, ResourceLocation material) {

    public static final int MIN_GEARS = 2, MAX_GEARS = 6;
    /** Ratio steps per 1.0, and the range, as steps (0.5 to 5.0). */
    public static final int PER_UNIT = 20, MIN_STEP = 10, MAX_STEP = 100;

    public static final Codec<TransmissionDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().fieldOf("steps").forGetter(TransmissionDesign::steps),
            ResourceLocation.CODEC.fieldOf("material").forGetter(TransmissionDesign::material)
    ).apply(i, TransmissionDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransmissionDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TransmissionDesign::steps,
            ResourceLocation.STREAM_CODEC, TransmissionDesign::material,
            TransmissionDesign::new);

    /** Pulled back into range, and padded or trimmed to a legal gear count, so a design is always buildable. */
    public TransmissionDesign {
        List<Integer> fixed = new ArrayList<>();
        for (int s : steps)
            fixed.add(Mth.clamp(s, MIN_STEP, MAX_STEP));
        while (fixed.size() < MIN_GEARS)
            fixed.add(MIN_STEP);
        steps = List.copyOf(fixed.subList(0, Math.min(fixed.size(), MAX_GEARS)));
    }

    public int gears() {
        return steps.size();
    }

    /** Ratio of forward gear {@code gear} (1 up). */
    public float ratio(int gear) {
        return steps.get(Mth.clamp(gear, 1, gears()) - 1) / (float) PER_UNIT;
    }

    /**
     * Positions the shifter has: 0 neutral, 1 to {@code gears()} forward, {@code gears() + 1} reverse.
     */
    public int positions() {
        return gears() + 2;
    }

    /** Speed ratio output over input at shifter position {@code position}: 0 for neutral, negative in reverse. */
    public float speedRatio(int position) {
        if (position <= 0)
            return 0f;
        if (position > gears())
            return -1f / ratio(1);
        return 1f / ratio(position);
    }

    public TransmissionDesign withGears(int gears) {
        List<Integer> next = new ArrayList<>(steps);
        while (next.size() < gears)
            next.add(next.get(next.size() - 1));
        return new TransmissionDesign(next.subList(0, Mth.clamp(gears, MIN_GEARS, MAX_GEARS)), material);
    }

    public TransmissionDesign withStep(int gear, int step) {
        List<Integer> next = new ArrayList<>(steps);
        next.set(gear, step);
        return new TransmissionDesign(next, material);
    }

    public TransmissionDesign withMaterial(ResourceLocation m) {
        return new TransmissionDesign(steps, m);
    }

    /** Ingots of its material it takes to build. */
    public int ingots() {
        return gears() * FTuning.TRANSMISSION_INGOTS_PER_GEAR;
    }

    /** The block's weight: gears' metal times the material's density. */
    public Mu mass(Material m) {
        return new Mu(FTuning.TRANSMISSION_METAL_PER_GEAR_MB * gears() * m.density().muPerMb());
    }

    /** How much a shift at speed costs this material: softer wears faster, by the hardness the deformation table already holds. */
    public float wearPerRpm(Material m) {
        return FTuning.TRANSMISSION_WEAR_PER_RPM / Math.max(1f, m.hardness().value());
    }

    public static TransmissionDesign starter(ResourceLocation material) {
        return new TransmissionDesign(List.of(70, 40, 25, 16), material);
    }

    /** The spec sheet a screen and a tooltip show: free and exact, philosophy 8. */
    public List<Component> describe(@org.jetbrains.annotations.Nullable Material m) {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("transmission.feedback.settings", gears(),
                Component.translatable("material." + material.getNamespace() + "." + material.getPath())));
        StringBuilder ratios = new StringBuilder();
        for (int g = 1; g <= gears(); g++)
            ratios.append(g > 1 ? "  " : "").append(g).append(": ").append(String.format("%.2f", ratio(g)));
        out.add(Component.translatable("transmission.feedback.ratios", ratios.toString()));
        if (m == null) {
            out.add(Component.translatable("engine.feedback.unknown_material"));
            return out;
        }
        out.add(Component.translatable("transmission.feedback.mass", Math.round(mass(m).value())));
        return out;
    }

    /** Whether a transmission can be made of {@code m}: what an engine needs. One rule for both. */
    public static boolean buildable(ResourceLocation id, Material m) {
        return EngineDesign.buildable(id, m);
    }
}
