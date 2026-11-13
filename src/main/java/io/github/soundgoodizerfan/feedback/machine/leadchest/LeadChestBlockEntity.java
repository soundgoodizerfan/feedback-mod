// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.leadchest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.Shielded;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;

/** The lead chest's 27 slots: a plain chest that keeps what it holds from radiating, by its lead lining. */
public class LeadChestBlockEntity extends RandomizableContainerBlockEntity implements Shielded {

    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public LeadChestBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LEAD_CHEST.get(), pos, state);
    }

    @Override
    public ResourceLocation shieldMaterial() {
        return Feedback.id("lead");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.feedback.lead_chest");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag))
            ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag))
            ContainerHelper.loadAllItems(tag, items, registries);
    }
}
