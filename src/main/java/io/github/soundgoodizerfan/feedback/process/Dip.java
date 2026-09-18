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

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What one item becomes when dipped in a vessel holding this fluid -- the first content this
 * mod's own {@code machine/dip/Dipping} interaction reads. Ceramic glazing is the first entry
 * (molten glass, real ceramic glazes being glass coatings -- no invented material needed), and
 * the shape is written wide enough that electroplating, mentioned as the reason to build this
 * infrastructure now rather than later, is a second table entry away rather than a second system.
 *
 * <h2>Why this is not {@link Casting}</h2>
 * Casting is triggered by a mold's contents cooling below a threshold, with no item ingredient at
 * all -- the mold is generic, any mold. A dip is the opposite shape: it needs an item ingredient
 * (what is being dipped) and has no temperature gate of its own (contact is the whole event,
 * exactly like {@link Quench} treats water). Forcing either table to answer the other's question
 * would be the same "two things wearing one shape" mistake {@link Casting}'s own doc already
 * warns against for {@link ThermalProcess}.
 *
 * <h2>Vessel dips only -- world water and lava have no table entry</h2>
 * Dipping a held item in a vessel's tank only does something when a {@link Dip} entry actually
 * matches, so an unrelated click against, say, a Crucible mid-melt never steals the click from
 * {@code MoldItem}'s own fill interaction. Plain water and lava are handled separately, directly
 * in {@code Dipping}, as an unconditional heat change -- there being no recipe to author for
 * "cold" or "very hot" is the whole point of leaving them out of this table.
 *
 * <h2>A temperature band, the same shape a workpiece already reads elsewhere</h2>
 * A vessel's tank does not stop being water or molten glass just because the vessel itself has
 * cooled -- fluid sitting in a tank has no melting physics of its own, only what a mold casts
 * from it does ({@link Casting}). So "the glass is actually liquid enough to glaze with" is not
 * implied by the fluid's mere presence and has to be a real condition here, the same way {@link
 * Quench#minTemperature} gates hardening on the workpiece's own heat rather than assuming contact
 * with water is always enough.
 *
 * @param fluid          what the vessel must be holding, and how much.
 * @param input          what is being dipped -- exactly one, per dip.
 * @param minTemperature the vessel must be at or above this. Unset means no floor.
 * @param maxTemperature the vessel must be at or below this. Unset means no ceiling -- most
 *                        entries will only ever need a floor, but a coating that burns off past
 *                        some temperature is a real thing to be able to say.
 * @param result         what it becomes.
 */
public record Dip(SizedFluidIngredient fluid, Ingredient input, Tu minTemperature, Tu maxTemperature,
                   ItemStack result) {

    public static final Codec<Dip> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(Dip::fluid),
            Ingredient.CODEC.fieldOf("input").forGetter(Dip::input),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(-Float.MAX_VALUE)).forGetter(Dip::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Dip::maxTemperature),
            ItemStack.CODEC.fieldOf("result").forGetter(Dip::result)
    ).apply(instance, Dip::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Dip> STREAM_CODEC = StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC, Dip::fluid,
            Ingredient.CONTENTS_STREAM_CODEC, Dip::input,
            Units.streamCodec(Tu::new), Dip::minTemperature,
            Units.streamCodec(Tu::new), Dip::maxTemperature,
            ItemStack.STREAM_CODEC, Dip::result,
            Dip::new);

    public boolean matches(FluidStack contents, ItemStack held, Tu vesselTemperature) {
        return fluid.test(contents) && input.test(held)
                && vesselTemperature.value() >= minTemperature.value()
                && vesselTemperature.value() <= maxTemperature.value();
    }
}
