// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the Engineering Table holds, every tab's slots in one container: it is a workbench, so closing the screen keeps all of it, and
 * breaking the block drops it. The output is the menu's, never stored. Tabs share nothing but the binder slot, which the Crafting and
 * Treatise tabs both show.
 */
public class EngineeringTableBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** Crafting tab: the 3x3 grid (row by row), the blueprint, four hand tools, three fasteners, the treatise binder. */
    public static final int GRID = 0, BLUEPRINT = GRID + 9, TOOLS = BLUEPRINT + 1, FASTENERS = TOOLS + 4, BINDER = FASTENERS + 3;
    /** Fit tab: the finished item and the 4x4 attachments laid on it (row by row). */
    public static final int FIT_ITEM = BINDER + 1, ATTACHMENTS = FIT_ITEM + 1;
    /** Repair tab: the item and the 3x3 of part stock a swap or mend draws on. */
    public static final int REPAIR_ITEM = ATTACHMENTS + 16, REPAIR_STOCK = REPAIR_ITEM + 1;
    /** Treatise tab: the blank treatise and the specimen taken apart into it (the binder is the Crafting tab's). */
    public static final int BLANK = REPAIR_STOCK + 9, SPECIMEN = BLANK + 1;
    public static final int SIZE = SPECIMEN + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public EngineeringTableBlockEntity(BlockPos pos, BlockState state) {
        super(FModular.ENGINEERING_TABLE_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.engineering_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EngineeringTableMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
