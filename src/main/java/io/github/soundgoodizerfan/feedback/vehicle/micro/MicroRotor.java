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
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationMember;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One rotation voxel in a micro cell ({@code docs/specs/feedback_micro_spec.md}, <em>Micro rotation
 * nodes</em>): a member of a rotation network that is not a block entity. Held by its
 * {@link MicroCellBlockEntity}, one per voxel whose block is a {@link MicroRotation}; it generates
 * and demands nothing, and drags and weighs its full-size part scaled by
 * {@link FTuning#MICRO_ROTATION_DRAG_SCALE} and {@link FTuning#MICRO_ROTATION_INERTIA_SCALE}.
 * Speed and angle mirror {@link RotationNode}'s: the network's, scaled by the ratio, on the server;
 * the last synced, extrapolated, on the client.
 */
public final class MicroRotor implements RotationMember {

    private static final Rpm ZERO_RPM = new Rpm(0);
    private static final Su NO_SU = new Su(0);

    private final MicroCellBlockEntity cell;
    private final int index;
    private final BlockState state;

    private float ratio = 1f;
    /** A flywheel voxel's rim condition, 1 down to {@link FTuning#FLYWHEEL_CONDITION_FLOOR}; a strained rim carries less inertia. */
    private float condition = 1f;
    @Nullable
    private RotationNetwork network;
    /** Speed and angle as the client last heard them (and, with no network yet, the server's last). */
    private float syncedRpm;
    private float phase;
    /** Client: game time the synced figures are from; MIN_VALUE until first drawn. */
    private long syncTick = Long.MIN_VALUE;

    MicroRotor(MicroCellBlockEntity cell, int index, BlockState state) {
        this.cell = cell;
        this.index = index;
        this.state = state;
    }

    public MicroCellBlockEntity cell() {
        return cell;
    }

    public int index() {
        return index;
    }

    public BlockState state() {
        return state;
    }

    private MicroRotation part() {
        return (MicroRotation) state.getBlock();
    }

    public float condition() {
        return condition;
    }

    void setCondition(float condition) {
        this.condition = condition;
    }

    @Override
    public float getRatio() {
        return ratio;
    }

    @Override
    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    @Nullable
    @Override
    public RotationNetwork getNetwork() {
        return network;
    }

    @Override
    public void setNetwork(@Nullable RotationNetwork network) {
        this.network = network;
    }

    @Override
    public Rpm getGeneratedRpm() {
        return ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return NO_SU;
    }

    @Override
    public Su getLoadSu() {
        return NO_SU;
    }

    @Override
    public Drag getDragSuPerRpm() {
        return new Drag(part().getDragSuPerRpm().suPerRpm() * FTuning.MICRO_ROTATION_DRAG_SCALE);
    }

    @Override
    public Inertia getInertia() {
        var bearing = cell.bearing(index);
        // A turned body's inertia is the real body's: no micro scale on it.
        return new Inertia(part().getInertia().suTicksPerRpm() * FTuning.MICRO_ROTATION_INERTIA_SCALE * condition
                + (bearing == null ? 0 : bearing.runInertia()));
    }

    @Override
    public Rpm getRpm() {
        return new Rpm(liveRpm());
    }

    float liveRpm() {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || network == null)
            return syncedRpm;
        return network.getCurrentRpm().value() * ratio;
    }

    float livePhase() {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || network == null)
            return phase;
        return network.getPhase() * ratio;
    }

    /** A rotor shows no ledger readout yet, so the Su figures are not kept. */
    @Override
    public void onNetworkChanged(Su capacitySu, Su loadSu) {
    }

    /** As {@link RotationNode#onNetworkSpeedChanged}: sync only once the speed moved a whole RPM. */
    @Override
    public void onNetworkSpeedChanged() {
        float rpm = liveRpm();
        if (Math.abs(rpm - syncedRpm) >= 1f || (rpm == 0 && syncedRpm != 0))
            syncSpeedNow();
    }

    @Override
    public void syncSpeedNow() {
        syncedRpm = liveRpm();
        phase = livePhase();
        cell.syncSpin();
    }

    /** Figures from a saved or synced tag; the client restarts its extrapolation from them. */
    void load(float rpm, float phase) {
        this.syncedRpm = rpm;
        this.phase = phase;
        this.syncTick = Long.MIN_VALUE;
    }

    /**
     * Client: the angle in degrees at {@code partialTick}, extrapolated from the last sync at the
     * synced speed, with the same figure {@link RotationNode#getVisualAngle(float)} uses.
     */
    public float angle(long gameTime, float partialTick) {
        if (syncTick == Long.MIN_VALUE)
            syncTick = gameTime;
        // In double: a steady run syncs once and then extrapolates for as long as it turns.
        return (float) ((phase + (double) syncedRpm * RotationNode.DEGREES_PER_TICK_PER_RPM
                * (gameTime - syncTick + partialTick)) % 360.0);
    }

    @Nullable
    @Override
    public Level getLevel() {
        return cell.getLevel();
    }

    @Override
    public BlockPos getBlockPos() {
        return cell.getBlockPos();
    }

    @Override
    public boolean isLive() {
        Level level = cell.getLevel();
        return level != null && !cell.isRemoved() && cell.rotor(index) == this
                && level.getBlockEntity(cell.getBlockPos()) == cell;
    }
}
