// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A lattice cell that seats one Nether star. The star is never consumed; the core reads whether one is seated. */
public class StarSeatBlockEntity extends BlockEntity {

    private ItemStack star = ItemStack.EMPTY;

    public StarSeatBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.STAR_SEAT.get(), pos, state);
    }

    public boolean seated() {
        return !star.isEmpty();
    }

    public boolean seat(ItemStack stack) {
        if (!star.isEmpty() || !stack.is(Items.NETHER_STAR))
            return false;
        star = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    public ItemStack take() {
        ItemStack out = star;
        star = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!star.isEmpty())
            tag.put("Star", star.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        star = tag.contains("Star") ? ItemStack.parseOptional(registries, tag.getCompound("Star")) : ItemStack.EMPTY;
    }
}
