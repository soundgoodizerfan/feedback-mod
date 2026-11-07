// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.stoker;

import java.util.Collections;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.firebox.FireboxBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Feeds the firebox in front of it, one item at a time, at {@code RPM x drum x} {@link #ITEMS_PER_RPM_TICK}. It does not look at the fire:
 * a firebox that is full refuses the item and the feed stalls on it, holding the screw's load, until the fire eats room. A stoker left
 * turning against a full box wastes nothing but its own power.
 */
public class StokerBlockEntity extends RotationNode implements Switchable, DataNode {

    /** Invented. Items per tick per RPM of shaft, per drum size. */
    public static final float ITEMS_PER_RPM_TICK = 0.0008f;

    private final ItemStackHandler hopper = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private float owed;
    private boolean engaged = true;

    public StokerBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.STOKER_ENTITY.get(), pos, state);
    }

    public IItemHandler hopper(Direction side) {
        return side == getBlockState().getValue(StokerBlock.FACING) ? null : hopper;
    }

    @Override
    public Su getLoadSu() {
        return new Su(3f * getBlockState().getValue(StokerBlock.DRUM));
    }

    public void tickServer() {
        if (level == null || !engaged)
            return;
        owed += Math.abs(getRpm().value()) * getBlockState().getValue(StokerBlock.DRUM) * ITEMS_PER_RPM_TICK;
        if (owed < 1f)
            return;
        Direction front = getBlockState().getValue(StokerBlock.FACING);
        if (!(level.getBlockEntity(worldPosition.relative(front)) instanceof FireboxBlockEntity box)) {
            owed = Math.min(owed, 1f);
            return;
        }
        for (int i = 0; i < hopper.getSlots() && owed >= 1f; i++) {
            ItemStack s = hopper.getStackInSlot(i);
            if (s.isEmpty())
                continue;
            ItemStack one = s.copyWithCount(1);
            if (box.insertFuel(one)) {
                hopper.extractItem(i, 1, false);
                owed -= 1f;
            }
        }
        owed = Math.min(owed, 1f);
    }

    public void dropAll() {
        if (level == null)
            return;
        for (int i = 0; i < hopper.getSlots(); i++)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), hopper.getStackInSlot(i));
    }

    @Override
    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
        setChanged();
    }

    @Override
    public boolean isEngaged() {
        return engaged;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return Collections.emptySet();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Hopper", hopper.serializeNBT(registries));
        tag.putBoolean("Engaged", engaged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hopper.deserializeNBT(registries, tag.getCompound("Hopper"));
        engaged = !tag.contains("Engaged") || tag.getBoolean("Engaged");
    }
}
