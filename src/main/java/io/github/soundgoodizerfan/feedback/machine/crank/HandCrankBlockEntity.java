// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.crank;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.modular.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rotation for as long as somebody keeps clicking.
 * <p>
 * Slice 1: 12 Su. Its speed, and how long one click keeps it going, are invented -- see
 * {@link FTuning}.
 * <p>
 * A right click is a single event, but turning a crank is continuous, so each click buys a short
 * run of rotation and holding the button refreshes it before it lapses. The crank turns for
 * exactly as long as somebody is working it.
 */
public class HandCrankBlockEntity extends RotationNode {

    private int turningTicks;

    private final FigureStore figures = new FigureStore();

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.HAND_CRANK.get(), pos, state);
    }

    /** A click by a person: under load, a steady hand ({@code CRANK_BEAT}) keeps the run going longer per click, so a slow cadence holds the beat. */
    public void turn(net.minecraft.world.entity.player.Player who) {
        int beat = who instanceof net.minecraft.server.level.ServerPlayer && getNetworkLoadSu().value() > 0f
                ? io.github.soundgoodizerfan.feedback.combat.skill.Skills.crankBeat(who) : 0;
        if (beat > 0)
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.cranked((net.minecraft.server.level.ServerPlayer) who, worldPosition);
        turn(FTuning.HAND_CRANK_TICKS_PER_TURN + beat);
    }

    public void turn() {
        turn(FTuning.HAND_CRANK_TICKS_PER_TURN);
    }

    private void turn(int ticks) {
        if (level == null || level.isClientSide)
            return;
        boolean wasStopped = turningTicks <= 0;
        turningTicks = ticks;
        if (wasStopped)
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    public void tickServer() {
        if (turningTicks <= 0)
            return;
        turningTicks--;
        if (turningTicks == 0 && level != null)
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    @Override
    public Rpm getGeneratedRpm() {
        return turningTicks > 0 ? new Rpm(figures.get("Rpm", FTuning.HAND_CRANK_RPM.value())) : ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return new Su(figures.get("Power", FTuning.HAND_CRANK_CAPACITY_SU.value()));
    }

    @Override
    public Inertia getInertia() {
        return new Inertia(figures.get("Inertia", FTuning.HAND_CRANK_INERTIA.suTicksPerRpm()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putInt("TurningTicks", turningTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        turningTicks = tag.getInt("TurningTicks");
    }
}
