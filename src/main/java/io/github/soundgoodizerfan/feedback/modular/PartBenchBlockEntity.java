// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

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
 * What a part-making bench holds: the hand tool it works with, the stock a part is shaped from in the centre slot, the stock beside
 * it, and at the Prep Table the slots a dish is put together from and the seasoning that finishes it. It is all kept when the screen
 * closes -- a table is a workbench, not a crafting grid -- and dropped when the block is broken.
 *
 * <p>The tool is the table's: nearly every act here costs it a point ({@link PartBenchMenu}),
 * which is the hand tool's accepted wear (CLAUDE.md), and no tool means no work.
 */
public abstract class PartBenchBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int HAMMER = 0, ASSEMBLY = 1, STOCK = 1 + ModularAssembly.DISH_SLOTS, TOOL = STOCK + 9, PLAN = TOOL + 1,
            SIZE = PLAN + 1;
    /** Cells of stock the table holds: a chestplate's plate is the most any shape takes (eight). */
    public static final int STOCK_SLOTS = 9;

    private final NonNullList<ItemStack> items;

    /** What every bench is built on; each has its own block entity type. */
    protected PartBenchBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    /** Whether {@code stack} is something the hammer slot takes: a hand hammer, by what it is and not which one. */
    public static boolean isHammer(ItemStack stack) {
        return ModularHandTool.strikes(stack);
    }

    /** Whether {@code stack} is something the Prep Table's tool slot takes: a hand knife. */
    public static boolean isKnife(ItemStack stack) {
        return stack.getItem() instanceof ModularHandTool && !ModularHandTool.strikes(stack) && !isSaw(stack);
    }

    /** Whether {@code stack} is something the stonecutter's tool slot takes: a sawblade. */
    public static boolean isSaw(ItemStack stack) {
        return stack.is(io.github.soundgoodizerfan.feedback.registry.FModular.SAWBLADE.get());
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
