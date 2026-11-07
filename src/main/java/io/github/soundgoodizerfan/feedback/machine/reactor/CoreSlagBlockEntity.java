// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.RadiationEmitter;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Slag from a breached core: a radiation source that decays slowly, worked out from the game time it was made (no ticking). */
public class CoreSlagBlockEntity extends BlockEntity implements RadiationEmitter {

    private float ru = FTuning.REACTOR_SLAG_RU;
    private long born = Long.MIN_VALUE;

    public CoreSlagBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CORE_SLAG.get(), pos, state);
    }

    public void stamp(float ru, long now) {
        this.ru = ru;
        this.born = now;
        setChanged();
    }

    @Override
    public float radiationRu() {
        if (level == null)
            return 0f;
        if (born == Long.MIN_VALUE)
            born = level.getGameTime();
        return ru * (float) Math.exp(-(level.getGameTime() - born) / (double) FTuning.REACTOR_SLAG_DECAY_TICKS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Ru", ru);
        tag.putLong("Born", born);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ru = tag.contains("Ru") ? tag.getFloat("Ru") : FTuning.REACTOR_SLAG_RU;
        born = tag.contains("Born") ? tag.getLong("Born") : Long.MIN_VALUE;
    }
}
