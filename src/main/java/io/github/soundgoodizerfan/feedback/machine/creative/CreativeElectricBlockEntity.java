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
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** A supply at any voltage and current. Volts are a free number here; the run still bands them. */
public class CreativeElectricBlockEntity extends ElectricNode implements CreativeSource {

    private boolean on = true;
    private float volts = FTuning.ELECTRIC_LV_VOLTS, amps = 4f;

    public CreativeElectricBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_ELECTRIC.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public float sourceVolts() {
        return on ? volts : 0f;
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
                new Param("volts", volts, FTuning.ELECTRIC_LV_VOLTS, 0f, 1000f),
                new Param("amps", amps, 1f, 0f, 10000f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        List<Param> p = params();
        this.on = on;
        this.volts = CreativeSource.clamp(p.get(0), values[0]);
        this.amps = CreativeSource.clamp(p.get(1), values[1]);
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("SetVolts", volts);
        tag.putFloat("SetAmps", amps);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        volts = tag.getFloat("SetVolts");
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
