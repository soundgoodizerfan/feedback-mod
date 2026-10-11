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

import net.minecraft.world.item.crafting.Ingredient;

/**
 * How hot a thing burns, and for how long.
 *
 * <h2>Why fuel is data and not a constant</h2>
 * The firebox first had a single {@code FIRE_TU_CHARCOAL} in the tuning table, which quietly
 * asserted that charcoal is the only fuel anyone will ever put in it. Two things were wrong with
 * that. A fire's temperature is a property of what is burning, so it belongs to the fuel the same
 * way hardness belongs to the material -- and §15 already wanted compatibility authored as a
 * table rather than as code, which is the same argument arriving from the other end. A pack that
 * wants coke to burn hotter than charcoal should write a line of JSON, not a mixin.
 * <p>
 * The shape is TerraFirmaCraft's, which is the obvious shape and is also the one the mod's own
 * existing tables already have. Ideas, not code; see THIRD-PARTY-LICENSES.md for why that
 * distinction is stricter here than it was for Create.
 *
 * <h2>Temperature and duration are independent, deliberately</h2>
 * A hotter fuel is not a better fuel, and neither is a longer-lasting one. Charcoal burns hot and
 * briefly, wood cool and briefly, coal hot and long -- which makes "what do I feed it" a real
 * question with no dominant answer, and it costs one row each. A single "fuel quality" scalar
 * would have collapsed the pair into a ladder with a correct top (§3).
 *
 * @param ingredient what burns.
 * @param duration   ticks one item lasts.
 * @param temperature flame temperature, in Tu. This is a hard ceiling on anything it heats -- a
 *                   vessel approaches it and can never pass it, so a fuel too cool for a process
 *                   is a genuine impossibility rather than a slow version of it (§7).
 * @param spread     how much this fuel's flame temperature varies, as a fraction, rolled once per
 *                   item and held for its whole burn. This is where philosophy 8's irreducible
 *                   noise floor enters the game, and it enters as something true: charcoal is not
 *                   all the same, so the firebox wanders and timers drift. Rolling per burn rather
 *                   than per tick is what makes it learnable in aggregate rather than merely
 *                   jittery -- a batch runs hot or cool and the player can feel that.
 */
public record Fuel(Ingredient ingredient, int duration, Tu temperature, float spread) {

    public static final Codec<Fuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(Fuel::ingredient),
            Codec.INT.fieldOf("duration").forGetter(Fuel::duration),
            Units.codec(Tu::new).fieldOf("temperature").forGetter(Fuel::temperature),
            Codec.FLOAT.optionalFieldOf("spread", 0.06f).forGetter(Fuel::spread)
    ).apply(instance, Fuel::new));

}
