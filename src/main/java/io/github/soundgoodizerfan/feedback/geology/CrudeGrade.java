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
package io.github.soundgoodizerfan.feedback.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a crude oil is (petrochemistry spec 2.4): one fluid, the grade rides on the stack as the
 * {@code feedback:crude_grade} component, the way {@code FluidHeat} carries temperature. A column
 * reads the grade it was fed and never asks which basin it came from.
 *
 * @param density       Mu per mB (water is 1): heavy crude is dense and pumps poorly.
 * @param sulfur        mass fraction, 0 to 1: sour crude fouls catalyst and its fumes are toxic.
 * @param lightFraction mass fraction that distils off light, 0 to 1: more naphtha, lower flash point.
 */
public record CrudeGrade(float density, float sulfur, float lightFraction) {

    public static final CrudeGrade DEFAULT = new CrudeGrade(0.85f, 0.01f, 0.30f);
    /** What a seep gives: the volatile fraction has evaporated (spec 3.4). */
    public static final CrudeGrade WEATHERED = new CrudeGrade(0.93f, 0.02f, 0.05f);

    public static final Codec<CrudeGrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("density").forGetter(CrudeGrade::density),
            Codec.FLOAT.fieldOf("sulfur").forGetter(CrudeGrade::sulfur),
            Codec.FLOAT.fieldOf("light_fraction").forGetter(CrudeGrade::lightFraction)
    ).apply(i, CrudeGrade::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrudeGrade> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CrudeGrade::density, ByteBufCodecs.FLOAT, CrudeGrade::sulfur,
            ByteBufCodecs.FLOAT, CrudeGrade::lightFraction, CrudeGrade::new);

    /** The stamp on {@code stack}, or {@link #DEFAULT} if it has none. */
    public static CrudeGrade of(FluidStack stack) {
        return stack.getOrDefault(FDataComponents.CRUDE_GRADE.get(), DEFAULT);
    }

    /** {@code stack} stamped with {@code grade} (a copy). */
    public static FluidStack stamp(FluidStack stack, CrudeGrade grade) {
        FluidStack out = stack.copy();
        if (!out.isEmpty())
            out.set(FDataComponents.CRUDE_GRADE.get(), grade);
        return out;
    }

    /**
     * The one merge rule, sibling of {@code FluidHeat.merge}: density by volume, the two mass fractions
     * by mass (volume times density), because a fraction of mass is what a tank of mixed crude holds.
     */
    public static CrudeGrade merge(CrudeGrade a, int mbA, CrudeGrade b, int mbB) {
        int total = mbA + mbB;
        if (total <= 0)
            return a;
        float massA = mbA * a.density, massB = mbB * b.density, mass = massA + massB;
        return new CrudeGrade((a.density * mbA + b.density * mbB) / total,
                mass <= 0 ? a.sulfur : (a.sulfur * massA + b.sulfur * massB) / mass,
                mass <= 0 ? a.lightFraction : (a.lightFraction * massA + b.lightFraction * massB) / mass);
    }
}
