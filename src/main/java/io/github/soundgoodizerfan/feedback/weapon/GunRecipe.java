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
import net.minecraft.resources.ResourceLocation;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A Gun Blueprint and its machined parts make that Gun; the Blueprint is kept (spec 12). The
 * blueprint says which parts by form ({@link GunDesign#assembly}); any item that is that form of
 * a gun metal fills the place, read off the material system, never an id. The gun is what was
 * put in: each part's material comes off its item and the barrel's real shape off its
 * {@link BarrelGeometry}, so the figures are baked from the parts, not the paper -- lazily, by
 * {@link GunItem}, on the server's own material table.
 */
public class GunRecipe extends CustomRecipe {

    public GunRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return assemble(input, ClientMaterials.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        GunDesign built = assemble(input, MaterialTable.get().materials());
        if (built == null)
            return ItemStack.EMPTY;
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), built);
        // A named design names every gun built from it.
        ItemStack blueprint = blueprint(input);
        if (blueprint != null && blueprint.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
            gun.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, blueprint.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        return gun;
    }

    /** The design the grid builds, with the parts' materials and the barrel's shape, or null if it is not that gun's parts. */
    @Nullable
    private static GunDesign assemble(CraftingInput input, Map<ResourceLocation, Material> materials) {
        ItemStack blueprint = blueprint(input);
        if (blueprint == null)
            return null;
        GunDesign design = blueprint.get(FWeapons.GUN_DESIGN.get());
        Map<String, Integer> wanted = design.assembly();
        // A coil's winding is consolidated redstone, wound from blocks of it, on top of the parts.
        Item winding = design.windingBlocks() > 0 ? GunDesign.windingItem(materials).orElse(null) : null;
        if (design.windingBlocks() > 0 && winding == null)
            return null;
        Map<String, Integer> found = new HashMap<>();
        Map<String, ResourceLocation> metal = new HashMap<>();
        BarrelGeometry barrel = null;
        GunDesign.SecondBarrel second = null;
        ResourceLocation wrap = null;
        int windings = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty() || s == blueprint)
                continue;
            if (winding != null && s.is(winding)) {
                windings++;
                continue;
            }
            var located = Material.find(materials, s.getItem());
            var part = located.filter(l -> wanted.containsKey(l.form())
                    && (l.form().equals("grip") ? GunDesign.gripMaterial(l.material()) : GunDesign.buildable(l.material())));
            // One more thing in the grid may be a wrap for the grip: leather, cloth, honey, by property.
            if (part.isEmpty() && wrap == null && located.filter(l -> GunDesign.wrapMaterial(l.material())).isPresent()) {
                wrap = located.get().id();
                continue;
            }
            if (part.isEmpty())
                return null;
            found.merge(part.get().form(), 1, Integer::sum);
            metal.putIfAbsent(part.get().form(), part.get().id());
            if (part.get().form().equals("gun_barrel")) {
                // A double's barrels are each their own: the first fills the design, the second its pair.
                BarrelGeometry shape = s.getOrDefault(FWeapons.BARREL_GEOMETRY.get(), BarrelGeometry.STOCK);
                if (barrel == null)
                    barrel = shape;
                else
                    second = new GunDesign.SecondBarrel(part.get().id(), shape);
            }
        }
        if (!found.equals(wanted) || windings != design.windingBlocks())
            return null;
        ResourceLocation receiver = metal.get("receiver");
        ResourceLocation action = design.actionPart().map(metal::get).orElse(receiver);
        var feed = java.util.Optional.ofNullable(metal.getOrDefault("cylinder", metal.get("magazine_tube")));
        return design.withMaterial(metal.get("gun_barrel"))
                .withParts(new GunDesign.Parts(action, metal.get("trigger_group"), metal.get("grip"), receiver, feed,
                        java.util.Optional.ofNullable(second), java.util.Optional.ofNullable(wrap)))
                .withBuilt(barrel);
    }

    /** The one blueprint in the grid that carries a design, or null if there is none or more than one. */
    @Nullable
    private static ItemStack blueprint(CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.is(FWeapons.GUN_BLUEPRINT.get()) && s.has(FWeapons.GUN_DESIGN.get())) {
                if (found != null)
                    return null;
                found = s;
            }
        }
        return found;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FWeapons.GUN_BLUEPRINT.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.GUN.get();
    }
}
