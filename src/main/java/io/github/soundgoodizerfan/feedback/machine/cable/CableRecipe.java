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
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.process.MaterialProperty;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Conductor of one metal, laid out by gauge, makes bare cable in that metal; an insulating row
 * under it (any material whose {@code electrical.insulation} is stated, all of one band, one
 * item under each column) coats it, and the band rides on the cable ({@code INSULATION}).
 *
 * <ul>
 *   <li>thin: three fine wires in a row;</li>
 *   <li>standard: three wires in a row;</li>
 *   <li>heavy: four wires in a 2x2;</li>
 *   <li>bus bar: one plate.</li>
 * </ul>
 * Each costs four times the copper of the one before (see {@link CableGauge}). The conductor is
 * "any item of that form whose material carries current", so no metal is named and a pack's new
 * conductor is accepted the day it has an {@code ampacity}. A recipe cannot say "the output is made
 * of whatever went in", which is why this is code and not JSON: the cells must agree, and the
 * result carries their material, the coat's band and the gauge.
 */
public class CableRecipe extends CustomRecipe {

    /** Any wire whose material states an ampacity at all. */
    public static final MaterialIngredient WIRE = MaterialIngredient.atLeast("wire", MaterialProperty.AMPACITY, 0f);
    public static final MaterialIngredient FINE_WIRE = MaterialIngredient.atLeast("fine_wire", MaterialProperty.AMPACITY, 0f);
    public static final MaterialIngredient PLATE = MaterialIngredient.atLeast("plate", MaterialProperty.AMPACITY, 0f);

    /** One gauge's conductor grid ({@code width} by {@code height}); the optional coat row is one more row of {@code width}. */
    public record Layout(CableGauge gauge, MaterialIngredient conductor, int width, int height) {}

    public static final List<Layout> LAYOUTS = List.of(
            new Layout(CableGauge.THIN, FINE_WIRE, 3, 1),
            new Layout(CableGauge.STANDARD, WIRE, 3, 1),
            new Layout(CableGauge.HEAVY, WIRE, 2, 2),
            new Layout(CableGauge.BUS, PLATE, 1, 1));

    public CableRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Read read = read(input);
        return read == null ? ItemStack.EMPTY
                : CableItem.of(read.material, read.gauge.yield(), read.insulation, read.gauge);
    }

    private record Read(ResourceLocation material, @Nullable VoltageBand insulation, CableGauge gauge) {}

    @Nullable
    private static Read read(CraftingInput input) {
        for (Layout layout : LAYOUTS) {
            Read read = read(input, layout);
            if (read != null)
                return read;
        }
        return null;
    }

    @Nullable
    private static Read read(CraftingInput input, Layout layout) {
        if (input.width() != layout.width || (input.height() != layout.height && input.height() != layout.height + 1))
            return null;
        boolean coated = input.height() > layout.height;
        ResourceLocation material = null;
        VoltageBand band = null;
        for (int x = 0; x < layout.width; x++) {
            for (int y = 0; y < layout.height; y++) {
                ItemStack item = input.getItem(x, y);
                if (!layout.conductor.test(item))
                    return null;
                ResourceLocation found = Material.find(MaterialIngredient.materialsNow(), item)
                        .map(Material.Located::id).orElse(null);
                if (found == null || (material != null && !material.equals(found)))
                    return null;
                material = found;
            }
            if (!coated)
                continue;
            VoltageBand coat = Material.find(MaterialIngredient.materialsNow(), input.getItem(x, layout.height))
                    .flatMap(l -> l.material().electrical().insulation()).orElse(null);
            if (coat == null || (band != null && band != coat))
                return null;
            band = coat;
        }
        return new Read(material, band, layout.gauge);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.CABLE.get();
    }
}
