// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * A plain view of the Engineering Table's crafting tab, with no menu behind it so recipes match it on the server and in JEI alike: the
 * 3x3 grid, up to {@value #TOOLS} tool stacks, up to {@value #FASTENERS} fastener stacks and the binder slot's stack. The stacks are the
 * live ones (a craft wears the tools and spends the fasteners in place); nothing here copies them.
 *
 * @param known the school-volume mask ({@link io.github.soundgoodizerfan.feedback.modular.treatise.Treatises#mask(net.minecraft.world.entity.player.Player, net.minecraft.world.inventory.ContainerLevelAccess)})
 *              the maker already holds elsewhere (inventory, a shelf beside the table); the binder is added to it by the recipe.
 */
public record EngineeringInput(List<ItemStack> grid, List<ItemStack> tools, List<ItemStack> fasteners, ItemStack binder, int known,
                               java.util.Map<String, Float> notes)
        implements RecipeInput {

    /** An input with no notes held (a plain view, a machine's): no treatise adds to the fit. */
    public EngineeringInput(List<ItemStack> grid, List<ItemStack> tools, List<ItemStack> fasteners, ItemStack binder, int known) {
        this(grid, tools, fasteners, binder, known, java.util.Map.of());
    }

    public static final int GRID = 9, TOOLS = 4, FASTENERS = 3;

    public EngineeringInput {
        if (grid.size() != GRID)
            throw new IllegalArgumentException("grid must be 3x3");
    }

    /** A view of just a grid, for a plain craft with no tools, fasteners, binder or knowledge. */
    public static EngineeringInput ofGrid(List<ItemStack> grid) {
        return new EngineeringInput(grid, List.of(), List.of(), ItemStack.EMPTY, 0);
    }

    /** The grid as vanilla's crafting input (3x3), which positions the shaped patterns trim. */
    public CraftingInput crafting() {
        return CraftingInput.of(3, 3, grid);
    }

    /** The non-empty grid stacks, in slot order. */
    public List<ItemStack> gridStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : grid)
            if (!s.isEmpty())
                out.add(s);
        return out;
    }

    @Override
    public ItemStack getItem(int index) {
        return grid.get(index);
    }

    @Override
    public int size() {
        return GRID;
    }
}
