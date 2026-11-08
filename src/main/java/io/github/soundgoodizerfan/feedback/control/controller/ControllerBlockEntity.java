// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mechanical Controller: a {@link ControllerCore} that needs a turning shaft to evaluate, per the spec's tier table (no motive force,
 * no switching: a dead shaft freezes outputs, variables and delay clocks alike). Takes the Punch Card only; the other media need an
 * electric host.
 */
public class ControllerBlockEntity extends RotationNode implements ControllerHost {

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.MECHANICAL_CARDS));

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.CONTROLLER.get(), pos, state);
    }

    protected ControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ControllerCore core() {
        return core;
    }

    /** Whether there is motive force to switch with. */
    protected boolean powered() {
        return getRpm().value() != 0;
    }

    @Override
    public Su getLoadSu() {
        return FTuning.CONTROLLER_LOAD_SU;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        core.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core.load(tag, registries);
    }
}
