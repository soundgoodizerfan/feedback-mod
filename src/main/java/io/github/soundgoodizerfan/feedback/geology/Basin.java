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

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;

/**
 * One basin kind, from {@code data/<ns>/basin/*.json} (petrochemistry spec 2.3). A region record,
 * not a block pattern: a basin is data held in {@link BasinState}, and only its seeps are blocks.
 * Tag fields are plain ids, never {@code "#..."} (same as {@code OreVein}).
 *
 * @param kind          sedimentary, nether_soul or deep_dark; informational until those exist.
 * @param radius        horizontal reach (blocks); at most {@link BasinFeature#MAX_RADIUS}.
 * @param thickness     vertical reach (blocks).
 * @param minY          lowest Y the basin may be centred at.
 * @param maxY          highest Y the basin may be centred at.
 * @param weight        share of the dimension's basin slots this kind takes.
 * @param dimensions    dimensions it occurs in.
 * @param biomes        biome tag the surface seeps are limited to; absent means any biome.
 * @param volumeMb      oil in place, mB.
 * @param drivePu       initial reservoir pressure, Pu.
 * @param exponent      k in {@code P = P0 * (V / V0) ^ k}.
 * @param crude         the grade at the heart of the basin.
 * @param gradeSpread   per-basin wobble around {@code crude}, a fraction, rolled once from the seed.
 * @param indicator     the surface block placed above it (a seep).
 */
public record Basin(String kind, int radius, int thickness, int minY, int maxY, int weight,
                    List<ResourceLocation> dimensions, Optional<TagKey<Biome>> biomes, int volumeMb, float drivePu,
                    float exponent, CrudeGrade crude, float gradeSpread, Optional<ResourceLocation> indicator) {

    public static final Codec<Basin> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("kind", "sedimentary").forGetter(Basin::kind),
            Codec.intRange(1, BasinFeature.MAX_RADIUS).fieldOf("radius").forGetter(Basin::radius),
            Codec.intRange(1, 64).fieldOf("thickness").forGetter(Basin::thickness),
            Codec.INT.fieldOf("min_y").forGetter(Basin::minY),
            Codec.INT.fieldOf("max_y").forGetter(Basin::maxY),
            ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(Basin::weight),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dimension_filter",
                    List.of(ResourceLocation.withDefaultNamespace("overworld"))).forGetter(Basin::dimensions),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(Basin::biomes),
            ExtraCodecs.POSITIVE_INT.fieldOf("volume_mb").forGetter(Basin::volumeMb),
            Codec.FLOAT.fieldOf("drive_pu").forGetter(Basin::drivePu),
            Codec.FLOAT.optionalFieldOf("pressure_exponent", 1f).forGetter(Basin::exponent),
            CrudeGrade.CODEC.fieldOf("crude").forGetter(Basin::crude),
            Codec.floatRange(0f, 1f).optionalFieldOf("grade_spread", 0f).forGetter(Basin::gradeSpread),
            ResourceLocation.CODEC.optionalFieldOf("indicator").forGetter(Basin::indicator)
    ).apply(i, Basin::new));
}
