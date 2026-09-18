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
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/**
 * One metal's acceptable share of a crucible's total melt, as a fraction of the whole
 * (0.08-0.12, not 8-12) -- see {@link AlloyMix#fraction}.
 *
 * <h2>Prior art</h2>
 * The ratio-window shape -- an alloy defined by acceptable composition ranges rather than a
 * fixed recipe -- is adapted from TerraFirmaCraft's {@code AlloyRange}/{@code AlloyRecipe}
 * (EUPL-1.2, cleared for reading per {@code THIRD-PARTY-LICENSES.md}). What is taken is the
 * shape of the idea, not the code: this class is written fresh against Feedback's own
 * {@link ThermalProcess}-style table conventions (a plain {@code Codec}, no networking, since
 * {@link AlloyTable} is server-only like {@link CastingTable}) rather than adapted line for line.
 */
public record AlloyRange(Fluid fluid, float min, float max) {

    /** Matches {@link AlloyMix}'s own tolerance -- see its doc for why one is needed at all. */
    private static final float EPSILON = 1e-4f;

    public static final Codec<AlloyRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(AlloyRange::fluid),
            Codec.FLOAT.fieldOf("min").forGetter(AlloyRange::min),
            Codec.FLOAT.fieldOf("max").forGetter(AlloyRange::max)
    ).apply(instance, AlloyRange::new));

    public boolean isIn(float fraction) {
        return fraction >= min - EPSILON && fraction <= max + EPSILON;
    }
}
