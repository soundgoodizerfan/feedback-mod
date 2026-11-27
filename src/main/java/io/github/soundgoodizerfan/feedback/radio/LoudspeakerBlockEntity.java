// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A loudspeaker: a sink on an electrical line tuned to a band, which plays out loud what a keyed set's microphone relays on that band ({@link RadioHearing}).
 * It has no antenna of its own, so its reach is the fixed {@link RadioTuning#SPEAKER_REACH_BLOCKS}; it plays only while power reaches it. Noise beside it cuts
 * the link as it does a set's. The loaded speakers are tracked in a weak set so the hearing code finds them without scanning the world.
 */
public class LoudspeakerBlockEntity extends ElectricNode {

    private static final Set<LoudspeakerBlockEntity> LOADED = Collections.synchronizedSet(java.util.Collections.newSetFromMap(new WeakHashMap<>()));

    private int band = 1;

    public LoudspeakerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LOUDSPEAKER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return RadioTuning.SPEAKER_AMPS;
    }

    public int band() {
        return band;
    }

    public void setBand(int band) {
        this.band = Math.max(1, Math.min(RadioTuning.BANDS, band));
        setChanged();
    }

    /** Whether power is reaching it. */
    public boolean powered() {
        return getAmps() > 0f;
    }

    /** A snapshot of the loaded speakers, for the hearing code. */
    public static java.util.List<LoudspeakerBlockEntity> loaded() {
        synchronized (LOADED) {
            return java.util.List.copyOf(LOADED);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Band", band);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        band = Math.max(1, Math.min(RadioTuning.BANDS, tag.getInt("Band")));
    }
}
