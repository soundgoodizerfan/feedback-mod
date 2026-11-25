// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An aisle's drive. Its shaft's speed is the carriage's. The bare carriage is a steady load and what it carries is drag,
 * which rises with speed, so a heavy trip on a weak shaft runs slowly and never stalls it.
 */
public class ShuttleHeadBlockEntity extends RotationNode {

    private float carried;

    public ShuttleHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FStore.HEAD_ENTITY.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return new Su(StoreTuning.SHUTTLE_BASE_LOAD_SU);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return new Drag(StoreTuning.SHUTTLE_DRAG_PER_MU * carried);
    }

    /** What the carriage now carries, set by the dock when a trip loads or unloads. */
    void carry(float mu) {
        if (mu == carried)
            return;
        carried = mu;
        if (getNetwork() != null)
            getNetwork().recalculate();
    }
}
