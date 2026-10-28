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
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * A fluid changing phase ({@code feedback_mechanics.md} 5.9): the liquid boils to the gas above its
 * boiling point, spending {@code latent} Work per mB, and the gas condenses back below it, giving
 * the same Work back. The fluid ids say only the phase; the heat rides on the stack. Boiler and
 * condenser read this and name neither water nor steam.
 *
 * @param liquid       what boils, by tag or id.
 * @param gas          what it boils to (and what condenses), by tag or id.
 * @param liquidFluid  the fluid made when the gas condenses.
 * @param gasFluid     the fluid made when the liquid boils.
 * @param boil         the boiling point.
 * @param latent       Work per mB of the change.
 * @param lightShare   for a crude's cut: the share of the feed's light fraction this cut carries. Present
 *                     marks the boil as a distillation cut rather than a pure liquid's.
 */
public record PhaseChange(FluidIngredient liquid, FluidIngredient gas, ResourceLocation liquidFluid,
                          ResourceLocation gasFluid, Tu boil, float latent, Optional<Float> lightShare) {

    public static final Codec<PhaseChange> CODEC = RecordCodecBuilder.create(i -> i.group(
            FluidIngredient.CODEC.fieldOf("liquid").forGetter(PhaseChange::liquid),
            FluidIngredient.CODEC.fieldOf("gas").forGetter(PhaseChange::gas),
            ResourceLocation.CODEC.fieldOf("liquid_fluid").forGetter(PhaseChange::liquidFluid),
            ResourceLocation.CODEC.fieldOf("gas_fluid").forGetter(PhaseChange::gasFluid),
            Units.codec(Tu::new).fieldOf("boil").forGetter(PhaseChange::boil),
            Codec.FLOAT.fieldOf("latent").forGetter(PhaseChange::latent),
            Codec.FLOAT.optionalFieldOf("light_share").forGetter(PhaseChange::lightShare)
    ).apply(i, PhaseChange::new));

    public boolean boils(FluidStack stack) {
        return liquid.test(stack);
    }

    public boolean condenses(FluidStack stack) {
        return gas.test(stack);
    }
}
