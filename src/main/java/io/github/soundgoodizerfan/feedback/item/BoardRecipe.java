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
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The three things done to a board by hand (mechanics 6.20), told apart by what is in the grid and
 * never by name:
 * <ul>
 * <li>a substrate and two fine wires of one metal: an <b>etched</b> board;</li>
 * <li>a substrate and a dust of a {@code paste} material: a <b>printed</b> board;</li>
 * <li>a board with no solder and a solder ingot ({@code solder} trait): the board <b>pasted</b>;</li>
 * <li>a board with no solder and one part ({@link Circuit#part}: a rectifying crystal, a relay, a
 * coherer, a ringing plate): the part <b>mounted</b>, up to {@link FTuning#CIRCUIT_MAX_PARTS}.</li>
 * </ul>
 * Pasting spends the solder whether or not the joint will come out well; the joint is graded later,
 * by the iron or the oven.
 */
public class BoardRecipe extends CustomRecipe {

    public BoardRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Optional<ItemStack> make(CraftingInput input, Map<ResourceLocation, Material> materials) {
        ItemStack board = null;
        Material.Located substrate = null, paste = null, solder = null, wire = null;
        Board.Part mount = null;
        int wires = 0, others = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.has(FDataComponents.BOARD.get())) {
                if (board != null)
                    return Optional.empty();
                board = stack;
                continue;
            }
            Material.Located found = Material.find(materials, stack).orElse(null);
            if (found == null) {
                Board.Part tagged = Circuit.part(stack, null);
                if (tagged == null || mount != null)
                    return Optional.empty();
                mount = tagged;
                continue;
            }
            Material m = found.material();
            if (found.form().equals("substrate") && substrate == null)
                substrate = found;
            else if (found.form().equals("fine_wire") && (wire == null || wire.id().equals(found.id()))) {
                wire = found;
                wires++;
            } else if (found.form().equals("dust") && m.traits().contains(Material.Trait.PASTE) && paste == null)
                paste = found;
            else if (found.form().equals("ingot") && m.traits().contains(Material.Trait.SOLDER) && solder == null)
                solder = found;
            else if (mount == null && (mount = Circuit.part(stack, found)) != null)
                continue;
            else
                others++;
        }
        if (others > 0)
            return Optional.empty();
        if (board != null && mount != null && substrate == null && paste == null && wire == null && solder == null) {
            // Mounting: a part goes on before the paste; a board has room for so many.
            Board b = board.get(FDataComponents.BOARD.get());
            if (b.solder().isPresent() || b.parts().size() >= FTuning.CIRCUIT_MAX_PARTS)
                return Optional.empty();
            ItemStack out = board.copyWithCount(1);
            out.set(FDataComponents.BOARD.get(), b.mounted(mount));
            return Optional.of(out);
        }
        if (board != null && substrate == null && paste == null && wire == null && solder != null) {
            Board b = board.get(FDataComponents.BOARD.get());
            if (b.solder().isPresent())
                return Optional.empty();
            ItemStack out = board.copyWithCount(1);
            out.set(FDataComponents.BOARD.get(), b.pasted(solder.id()));
            return Optional.of(out);
        }
        if (board != null || solder != null || mount != null || substrate == null)
            return Optional.empty();
        ItemStack out = new ItemStack(FItems.TRACE_BOARD.get());
        if (wire != null && wires == 2 && paste == null)
            out.set(FDataComponents.BOARD.get(), new Board(Board.Trace.ETCHED, wire.id(), substrate.id(), Optional.empty(), Board.Joint.NONE, java.util.List.of()));
        else if (paste != null && wire == null)
            out.set(FDataComponents.BOARD.get(), new Board(Board.Trace.PRINTED, paste.id(), substrate.id(), Optional.empty(), Board.Joint.NONE, java.util.List.of()));
        else
            return Optional.empty();
        return Optional.of(out);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return make(input, ClientMaterials.on(level)).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return make(input, MaterialTable.get().materials()).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.BOARD.get();
    }
}
