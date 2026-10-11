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

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a material becomes when it is taken from hot to cold faster than it wanted to go.
 *
 * <h2>The first requirement in the slice that is not a number to hit</h2>
 * Everything before this is satisfied by getting a value right: enough Fu, a temperature inside a
 * band. Quenching is satisfied by a <em>rate</em>, and the same ingot at the same final
 * temperature is a different item depending on how it got there. Nothing about the end state
 * distinguishes the two, which is philosophy 7's history-sensitivity arriving as a thing the
 * player does rather than a paragraph they read. It is also real metallurgy -- the mod noticed
 * this rule rather than inventing it (§2).
 *
 * <h2>Why the rate is not a field</h2>
 * The slice specifies {@code cooling rate >= 200 Tu/t}, and there is exactly one thing in the game
 * that cools an ingot that fast: putting it in water. Air is two orders of magnitude slower. So a
 * stored figure would be compared against a number that is always either far above it or far
 * below it, and it would read as a tuning knob over a decision that has no middle. What is stored
 * instead is the floor the workpiece must still be above when it goes in -- which is the part the
 * player can actually get wrong, because they are racing the ingot's own cooling to the water.
 *
 * @param input          what is being quenched.
 * @param minTemperature how hot it must still be when it hits the water, in Tu. Below this the
 *                       quench does nothing at all and the workpiece is merely cold -- no loss, no
 *                       progress, which is the safe direction again (§6).
 * @param result         what it becomes.
 */
public record Quench(Ingredient input, Tu minTemperature, ItemStack result) {

    public static final Codec<Quench> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(Quench::input),
            Units.codec(Tu::new).fieldOf("min_temperature").forGetter(Quench::minTemperature),
            ItemStack.CODEC.fieldOf("result").forGetter(Quench::result)
    ).apply(instance, Quench::new));

}
