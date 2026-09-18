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

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/**
 * A named alloy, defined by the composition window every one of its metals must sit in
 * simultaneously. See {@link AlloyRange} for what "window" means and where the shape came from.
 *
 * @param contents one range per metal the alloy is made of. A crucible's mix matches only if it
 *                  holds exactly these metals (no unlisted one tolerated) and every one's share
 *                  of the whole falls in its own range.
 * @param result   the fluid the mix becomes once matched.
 */
public record AlloyRecipe(List<AlloyRange> contents, Fluid result) {

    public static final Codec<AlloyRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AlloyRange.CODEC.listOf().fieldOf("contents").forGetter(AlloyRecipe::contents),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("result").forGetter(AlloyRecipe::result)
    ).apply(instance, AlloyRecipe::new));

    public boolean matches(Map<Fluid, Integer> content, int total) {
        if (total <= 0 || content.size() != contents.size())
            return false;
        for (AlloyRange range : contents) {
            Integer amount = content.get(range.fluid());
            if (amount == null || !range.isIn(amount / (float) total))
                return false;
        }
        return true;
    }
}
