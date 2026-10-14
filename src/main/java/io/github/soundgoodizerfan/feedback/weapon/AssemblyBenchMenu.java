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

import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A crafting table four slots square (spec 12): a coil with a scope and a tube needs more parts
 * than nine slots hold. It is the crafting grid, bigger, and nothing else -- it asks the same
 * recipe manager, so it assembles a gun from {@link GunRecipe} and makes anything a table makes.
 * Nothing is kept in the block: what is left in the grid comes back when it is closed.
 */
public class AssemblyBenchMenu extends AbstractContainerMenu {

    public static final int GRID = 4, GRID_X = 12, GRID_Y = 17, RESULT_X = 134, RESULT_Y = 44,
            INV_X = 8, INV_Y = 102, INV_START = 1 + GRID * GRID;

    private final CraftingContainer grid = new TransientCraftingContainer(this, GRID, GRID);
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public AssemblyBenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public AssemblyBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(FMenus.ASSEMBLY_BENCH.get(), id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new ResultSlot(player, grid, result, 0, RESULT_X, RESULT_Y));
        for (int row = 0; row < GRID; row++)
            for (int column = 0; column < GRID; column++)
                addSlot(new Slot(grid, column + row * GRID, GRID_X + column * 18, GRID_Y + row * 18));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
    }

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> update(level));
    }

    /** What the grid makes, asked of the recipe manager on the server, the crafting table's way. */
    private void update(Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer server))
            return;
        CraftingInput input = grid.asCraftInput();
        ItemStack out = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(h -> h.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        result.setItem(0, out);
        setRemoteSlot(0, out);
        server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, out));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, grid));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FWeapons.ASSEMBLY_BENCH.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index == 0) {
            if (!moveItemStackTo(held, INV_START, slots.size(), true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(held, moved);
        } else if (index < INV_START) {
            if (!moveItemStackTo(held, INV_START, slots.size(), false))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(held, 1, INV_START, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, held);
        if (index == 0)
            player.drop(held, false);
        return moved;
    }
}
