// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** A supply at any band and current: the band is picked by index (0 LV up to 5 Trunk), the amps are a free number. */
public class CreativeElectricBlockEntity extends ElectricNode implements CreativeSource {

    private boolean on = true;
    private float band = VoltageBand.LV.ordinal(), amps = 4f;

    public CreativeElectricBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_ELECTRIC.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public VoltageBand sourceBand() {
        return on ? VoltageBand.values()[Math.round(band)] : null;
    }

    @Override
    public float sourceAmps() {
        return on ? amps : 0f;
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(
                new Param("band", band, 1f, 0f, VoltageBand.values().length - 1),
                new Param("amps", amps, 1f, 0f, 10000f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        List<Param> p = params();
        this.on = on;
        this.band = CreativeSource.clamp(p.get(0), values[0]);
        this.amps = CreativeSource.clamp(p.get(1), values[1]);
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("SetBand", band);
        tag.putFloat("SetAmps", amps);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        band = tag.getFloat("SetBand");
        amps = tag.getFloat("SetAmps");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
