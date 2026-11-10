// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A vent: drains the gas cells it touches, as fast as it is open. Open by hand or by a controller card
 * (analog, channel 0); the higher of the two wins. Idle (shut) it does nothing but return.
 */
public class VentValveBlockEntity extends AnalogPartBlockEntity {

    private boolean hand;
    private float card;
    private float owed;

    public VentValveBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.VENT_VALVE_ENTITY.get(), pos, state);
    }

    public boolean handOpen() {
        return hand;
    }

    public void toggleHand() {
        hand = !hand;
        setChanged();
    }

    /** How far open, 0 to 1. */
    public float opening() {
        return Math.max(hand ? 1f : 0f, card);
    }

    @Override
    public void setLevel(int channel, float level) {
        card = Math.clamp(level, 0f, 1f);
    }

    @Override
    public String[] channelKeys() {
        return new String[] {"feedback.analog.vent"};
    }

    public void tickServer() {
        float open = opening();
        if (open <= 0f || !(level instanceof ServerLevel server))
            return;
        owed += FTuning.VENT_MB_PER_TICK * open;
        int want = (int) owed;
        if (want <= 0)
            return;
        owed -= want;
        for (Direction d : Direction.values())
            if (server.getBlockEntity(worldPosition.relative(d)) instanceof GasCellBlockEntity cell
                    && !cell.getTank().isEmpty()) {
                cell.getTank().drain(want, IFluidHandler.FluidAction.EXECUTE);
                net.minecraft.world.phys.Vec3 w = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
                if (dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(this) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel body)
                    w = body.logicalPose().transformPosition(w);
                server.sendParticles(ParticleTypes.CLOUD, w.x, w.y + 0.6, w.z, 1, 0.1, 0.1, 0.1, 0.02);
                return;
            }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Hand", hand);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hand = tag.getBoolean("Hand");
    }
}
