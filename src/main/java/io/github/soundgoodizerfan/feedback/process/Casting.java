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

import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What a filled mold becomes once it has cooled enough to solidify.
 *
 * <h2>Why this is not a {@link ThermalProcess}</h2>
 * Melting collapsed into {@link ThermalProcess} because it turned out to be the same question
 * with the same shape -- a temperature threshold gating an item's transformation, authored the
 * same way. Casting genuinely is not: its input is not an {@link net.minecraft.world.item.crafting.Ingredient}-matchable
 * {@link ItemStack} at all (a mold currently holding molten copper, not "any copper ingot"), and
 * its trigger runs the opposite direction -- it fires on <em>cooling below</em> a threshold,
 * where every other thermal entry in the mod fires on rising through or past one. Forcing it into
 * {@code ThermalProcess} would mean overloading {@code inputs}/{@code inBand} to mean two
 * different things depending on which table is asking.
 *
 * <h2>Self-contained on purpose</h2>
 * {@link #solidifyTemperature} duplicates the melt point a {@link ThermalProcess} entry for the
 * same metal already states. That is a real, accepted redundancy rather than a derived lookup
 * across tables -- every other table in this mod (deformation, thermal process, quench, fuel) is
 * already independent of the others, and a datapack author writing "copper solidifies at 800 Tu"
 * twice is cheaper than this table reaching into {@code ThermalProcessTable} to find out.
 *
 * <h2>Where refrigeration plugs in for free, later</h2>
 * Nothing here asks <em>why</em> a mold cooled -- only whether {@link
 * io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat} currently reads below this
 * threshold, computed on demand the same way every other workpiece reading is. A future
 * refrigeration actuator that pulls a vessel below ambient needs no change here at all.
 *
 * @param fluid               what the mold must be holding, and how much.
 * @param solidifyTemperature at or below this the mold's contents are solid and may be extracted.
 * @param result              what comes out.
 */
public record Casting(SizedFluidIngredient fluid, Tu solidifyTemperature, ItemStack result) {

    public static final Codec<Casting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(Casting::fluid),
            Units.codec(Tu::new).fieldOf("solidify_temperature").forGetter(Casting::solidifyTemperature),
            ItemStack.CODEC.fieldOf("result").forGetter(Casting::result)
    ).apply(instance, Casting::new));

    public boolean matches(FluidStack contents) {
        return fluid.test(contents);
    }

    public boolean isSolidAt(Tu tu) {
        return tu.value() <= solidifyTemperature.value();
    }
}
