// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.clutch;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A {@link DataNode} as well as a {@link Switchable} -- spec {@code docs/specs/feedback_controller_spec.md}
 * §3: "a {@code Switchable} block entity should also be a {@code DataNode}", so a Switch
 * card's stored {@link DataNodeRef} resolves to something real. Unsided, the same shape as the
 * Timer: a Clutch is a standalone block, not a sided fitting.
 * <p>
 * {@link #links} exists only because the interface requires a set to return -- nothing calls
 * {@code linkTo}/{@code unlink} on a Clutch through the controller feature. A Punch Card's
 * {@code Switch} card carries its own {@link DataNodeRef} directly (see
 * {@code control/program}), so this never needs to be the *other* end of a completed link.
 */
public class ClutchBlockEntity extends RotationNode implements Switchable, DataNode {

    private static final String LINKS = "Links";

    private final Set<DataNodeRef> links = new HashSet<>();

    public ClutchBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.CLUTCH.get(), pos, state);
    }

    /** For a clutch that is also something else -- see {@code OverspeedTripBlockEntity}. */
    protected ClutchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(ClutchBlock.ENGAGED);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || isEngaged() == engaged)
            return;

        Direction output = getBlockState().getValue(ClutchBlock.FACING);
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ClutchBlock.ENGAGED, engaged));

        // Both sides have to be rebuilt, and from each side separately: engaging must merge two
        // networks into one, disengaging must split one into two, and a rebuild only ever sees
        // the run it can reach from where it started.
        RotationPropagator.rebuildFrom(level, worldPosition);
        RotationPropagator.rebuildFrom(level, worldPosition.relative(output));

        level.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.WOODEN_BUTTON_CLICK_ON,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, engaged ? 0.9f : 0.6f);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.SHAFT_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.SHAFT_INERTIA;
    }

    // --- DataNode -----------------------------------------------------------------------------

    /** Targeted by a card, never linked -- see {@link DataNode#acceptsLinks}. */
    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
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
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
    }
}
