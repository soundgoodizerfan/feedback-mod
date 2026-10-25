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
package io.github.soundgoodizerfan.feedback.combat.gear;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Plates of one material and straps of one material, in any arrangement, make a piece of combat
 * gear baked from exactly those (the {@code TongsRecipe} pattern). Plate stock is whatever the material
 * table says is a {@code plate} form, a strap is a {@code leather} form -- no item is named
 * (philosophy 11), and sheet stock is plate, not ingot. The counts in {@link GearKind} pick the piece:
 * 2 plates and a strap a bracer, a plate and 2 straps a sash, 3 plates and a strap a back piece.
 *
 * <p>This is also the craft route for every relic of these slots (philosophy 14): a relic is a piece
 * with a perk or a {@code grants} list, and none exists that this table cannot make later.
 */
public class GearRecipe extends CustomRecipe {

    public GearRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Parts(GearKind kind, ResourceLocation shell, ResourceLocation strap) {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, ClientMaterials.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Parts parts = read(input, materials);
        if (parts == null)
            return ItemStack.EMPTY;
        return make(parts.kind(), parts.shell(), materials.get(parts.shell()), parts.strap(), materials.get(parts.strap()));
    }

    /** A baked piece; shared with the creative tab's sample. */
    public static ItemStack make(GearKind kind, ResourceLocation shellId, Material shell, ResourceLocation strapId, Material strap) {
        Item item = switch (kind) {
            case BRACER -> FItems.BRACER.get();
            case SASH -> FItems.SASH.get();
            case BACK -> FItems.BACK_PIECE.get();
        };
        ItemStack out = new ItemStack(item);
        out.set(FDataComponents.GEAR.get(), GearStats.bake(kind, shellId, shell, strapId, strap));
        return out;
    }

    @Nullable
    private static Parts read(CraftingInput input, Map<ResourceLocation, Material> materials) {
        ResourceLocation shell = null;
        ResourceLocation strap = null;
        int plates = 0, straps = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            Material.Located found = Material.find(materials, stack.getItem()).orElse(null);
            if (found == null)
                return null;
            switch (found.form()) {
                case "plate" -> {
                    if (shell != null && !shell.equals(found.id()))
                        return null;
                    shell = found.id();
                    plates++;
                }
                case "leather" -> {
                    if (strap != null && !strap.equals(found.id()))
                        return null;
                    strap = found.id();
                    straps++;
                }
                default -> {
                    return null;
                }
            }
        }
        for (GearKind kind : GearKind.values())
            if (kind.plates() == plates && kind.straps() == straps)
                return new Parts(kind, shell, strap);
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.GEAR.get();
    }
}
