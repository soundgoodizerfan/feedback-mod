/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.crank;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
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
        return turningTicks > 0 ? FTuning.HAND_CRANK_RPM : ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return FTuning.HAND_CRANK_CAPACITY_SU;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.HAND_CRANK_INERTIA;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TurningTicks", turningTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        turningTicks = tag.getInt("TurningTicks");
    }
}
