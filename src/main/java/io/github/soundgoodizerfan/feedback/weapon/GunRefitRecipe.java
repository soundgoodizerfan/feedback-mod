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
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Refitting a built gun (spec 2: an attachment is a part of the design, and fitting one re-runs
 * the bake). A gun and one fitting part -- a suppressor, a brake, a relief vent, a shroud, of any
 * gun metal -- make that gun with the part fitted; a gun and one or two plates make it with a
 * reflex sight or a scope, as the Stand's sights are built. Whatever the new part displaced comes
 * back in the grid where the new part was: a brake for a suppressor, a scope's two plates for a
 * reflex sight's one. The gun keeps its rounds, heat and wear, and loses its figures, which
 * {@link GunItem} bakes again from the new design.
 */
public class GunRefitRecipe extends CustomRecipe {

    public GunRefitRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** What the grid holds: the gun, and the one kind of part going on it, at {@code slot}. */
    private record Refit(int gunSlot, ItemStack gun, GunDesign design, GunDesign refitted, int partSlot,
                         Optional<ItemStack> displaced) {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, ClientMaterials.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Refit r = read(input, MaterialTable.get().materials());
        if (r == null)
            return ItemStack.EMPTY;
        ItemStack out = r.gun().copyWithCount(1);
        out.set(FWeapons.GUN_DESIGN.get(), r.refitted());
        out.remove(FWeapons.GUN_FIGURES.get());
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        Refit r = read(input, MaterialTable.get().materials());
        if (r != null)
            r.displaced().ifPresent(s -> out.set(r.partSlot(), s));
        return out;
    }

    @Nullable
    private static Refit read(CraftingInput input, Map<ResourceLocation, Material> materials) {
        int gunSlot = -1, partSlot = -1, plates = 0;
        ResourceLocation plateMetal = null;
        GunDesign.Fitting fitting = null;
        ResourceLocation fittingMetal = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FWeapons.GUN.get()) && s.has(FWeapons.GUN_DESIGN.get())) {
                if (gunSlot >= 0)
                    return null;
                gunSlot = i;
                continue;
            }
            var part = Material.find(materials, s.getItem()).filter(l -> GunDesign.buildable(l.material()));
            if (part.isEmpty())
                return null;
            var f = GunDesign.Fitting.ofForm(part.get().form());
            if (f.isPresent() && fitting == null && plates == 0) {
                fitting = f.get();
                fittingMetal = part.get().id();
                partSlot = i;
            } else if (part.get().form().equals("plate") && fitting == null && plates < 2) {
                plates++;
                plateMetal = part.get().id();
                partSlot = partSlot < 0 ? i : partSlot;
            } else
                return null;
        }
        if (gunSlot < 0 || partSlot < 0)
            return null;
        ItemStack gun = input.getItem(gunSlot);
        GunDesign design = gun.get(FWeapons.GUN_DESIGN.get());
        if (fitting != null) {
            // A muzzle fitting displaces whichever muzzle fitting is on; the others only themselves.
            Optional<ItemStack> displaced = Optional.empty();
            for (var on : design.fittings().entrySet())
                if (on.getKey() == fitting || (on.getKey().muzzle() && fitting.muzzle()))
                    displaced = item(materials, on.getValue(), on.getKey().form(), 1);
            return new Refit(gunSlot, gun, design, design.withFitting(fitting, fittingMetal), partSlot, displaced);
        }
        GunDesign.Sight sight = plates == 1 ? GunDesign.Sight.REFLEX : GunDesign.Sight.SCOPE;
        if (sight == design.sight())
            return null;
        // A sight's plates were never told apart by metal; what comes off is the receiver's, which it sits on.
        Optional<ItemStack> displaced = design.sight().plates() == 0 ? Optional.empty()
                : item(materials, design.parts().receiver(), "plate", design.sight().plates());
        return new Refit(gunSlot, gun, design, design.withSight(sight), partSlot, displaced);
    }

    private static Optional<ItemStack> item(Map<ResourceLocation, Material> materials, ResourceLocation metal, String form, int count) {
        Material m = materials.get(metal);
        return m == null ? Optional.empty() : m.item(metal, form).map(i -> new ItemStack(i, count));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.GUN_REFIT.get();
    }
}
