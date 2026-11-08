// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Electric Controller: the same {@link ControllerCore} as the mechanical one, powered as an electrical sink instead of off a shaft. It
 * evaluates while the line delivers at least half of what it wants, and takes every program medium: Punch Card, Circuit Board, Floppy
 * Disk and USB Drive.
 */
public class ElectricControllerBlockEntity extends ElectricNode implements ControllerHost {

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.PUNCH_CARDS));

    public ElectricControllerBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.ELECTRIC_CONTROLLER.get(), pos, state);
    }

    protected ElectricControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ControllerCore core() {
        return core;
    }

    protected boolean powered() {
        return ElectricRules.rate(getVolts(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.CONTROLLER_ELECTRIC_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
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
