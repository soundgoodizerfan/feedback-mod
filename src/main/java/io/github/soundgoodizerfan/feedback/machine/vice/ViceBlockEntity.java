// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vice;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.item.ItemTuning;
import io.github.soundgoodizerfan.feedback.machine.ThreadScrew;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceHolder;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

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
 * A bench vice: a thread rod seated as its jaw screw closes the jaws on one piece, which then stays put while hand work is done on it
 * ({@link io.github.soundgoodizerfan.feedback.item.HandWork}). A clamped piece frees the player's other hand and steadies the work: how
 * steady depends on the screw ({@link ThreadScrew.Screw#looseness}, a cut thread has little play, a rolled one more). With no screw the
 * jaws do not close, so nothing is held. No tick: it only changes when a hand acts on it.
 */
public class ViceBlockEntity extends BlockEntity implements WorkpieceHolder {

    private ItemStack piece = ItemStack.EMPTY;
    private ItemStack screw = ItemStack.EMPTY;

    public ViceBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VICE.get(), pos, state);
    }

    public ItemStack getScrew() {
        return screw;
    }

    /** Whether the jaws have a thread rod to close on. */
    public boolean clamps() {
        return level != null && ThreadScrew.of(screw, level).isPresent();
    }

    /**
     * The share of every stroke a clamped piece loses to play in the jaws, the screw's looseness, less what clamping gains
     * ({@link ItemTuning#VICE_HOLD_GAIN}): negative, a clamped piece strikes harder than a held one ({@link ItemTuning#HAND_WOBBLE}).
     */
    public float wobble() {
        if (level == null)
            return ItemTuning.HAND_WOBBLE;
        return ThreadScrew.of(screw, level).map(s -> s.looseness() - ItemTuning.VICE_HOLD_GAIN).orElse(ItemTuning.HAND_WOBBLE);
    }

    @Override
    public ItemStack getWorkpiece() {
        return piece;
    }

    /** Clamps one of {@code stack}, if the jaws have a screw and are empty. */
    @Override
    public boolean insert(ItemStack stack) {
        if (!piece.isEmpty() || stack.isEmpty() || !clamps())
            return false;
        piece = stack.split(1);
        sync();
        return true;
    }

    @Override
    public ItemStack removeWorkpiece() {
        ItemStack taken = piece;
        piece = ItemStack.EMPTY;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    /** The piece after a stroke of hand work. */
    public void setWorkpiece(ItemStack next) {
        piece = next;
        sync();
    }

    /** Seats {@code stack} (a thread rod) as the jaw screw: one of it goes in, and the one it replaces comes back. */
    public ItemStack seatScrew(ItemStack stack) {
        ItemStack old = screw;
        screw = stack.split(1);
        sync();
        return old;
    }

    public ItemStack removeScrew() {
        ItemStack old = screw;
        screw = ItemStack.EMPTY;
        sync();
        return old;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Piece", piece.saveOptional(registries));
        tag.put("Screw", screw.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        piece = ItemStack.parseOptional(registries, tag.getCompound("Piece"));
        screw = ItemStack.parseOptional(registries, tag.getCompound("Screw"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
