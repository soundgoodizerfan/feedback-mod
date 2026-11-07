// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** A shaft source at any speed, capacity and inertia. Syncs through {@link RotationNode#sync()}. */
public class CreativeRotationBlockEntity extends RotationNode implements CreativeSource {

    private boolean on = true;
    private float rpm = 16f, capacity = 1000f, inertia = 10f;

    public CreativeRotationBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_ROTATION.get(), pos, state);
    }

    @Override
    public Rpm getGeneratedRpm() {
        return on ? new Rpm(rpm) : ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return new Su(capacity);
    }

    @Override
    public Inertia getInertia() {
        return new Inertia(inertia);
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(
                new Param("rpm", rpm, 4f, 0f, 1024f),
                new Param("capacity", capacity, 100f, 0f, 100000f),
                new Param("inertia", inertia, 5f, 0f, 10000f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        List<Param> p = params();
        this.on = on;
        this.rpm = CreativeSource.clamp(p.get(0), values[0]);
        this.capacity = CreativeSource.clamp(p.get(1), values[1]);
        this.inertia = CreativeSource.clamp(p.get(2), values[2]);
        sync();
        if (level != null)
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("SetRpm", rpm);
        tag.putFloat("SetCapacity", capacity);
        tag.putFloat("SetInertia", inertia);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        rpm = tag.getFloat("SetRpm");
        capacity = tag.getFloat("SetCapacity");
        inertia = tag.getFloat("SetInertia");
    }
}
