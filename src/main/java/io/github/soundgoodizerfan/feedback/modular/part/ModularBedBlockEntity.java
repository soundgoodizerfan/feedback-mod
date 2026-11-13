// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bed a placed {@link ModularBedBlock} half was built as: the whole stack, so its design, figures and wear survive placing and breaking
 * (the engine's rule, kept as the stack itself rather than picked components). Both halves hold it; the head is the one that drops it.
 */
public class ModularBedBlockEntity extends BlockEntity {

    private ItemStack bed = ItemStack.EMPTY;

    public ModularBedBlockEntity(BlockPos pos, BlockState state) {
        super(FModular.BED_ENTITY.get(), pos, state);
    }

    public ItemStack bed() {
        return bed;
    }

    public void setBed(ItemStack stack) {
        bed = stack.copyWithCount(1);
        io.github.soundgoodizerfan.feedback.modular.attunement.MachineAura.forget(this);
        changed();
    }

    /** Saves and sends the bed to watching clients (its tint reads the design). */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Bed", bed.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bed = ItemStack.parseOptional(registries, tag.getCompound("Bed"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
