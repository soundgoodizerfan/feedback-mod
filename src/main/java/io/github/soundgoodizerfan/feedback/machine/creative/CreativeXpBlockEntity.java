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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.unit.XPu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Experience, at any figure, with nothing to farm: sneak-use with a Reservoir-attuned tool fills it by {@link
 * #points} points. XP is an energy type with no network, so this is not a source on a line but a tap a part is
 * charged from. Operator tab only; the first thing built for XP, before any real producer (digest).
 */
public class CreativeXpBlockEntity extends BlockEntity implements CreativeSource {

    private boolean on = true;
    private float points = 100f;

    public CreativeXpBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_XP.get(), pos, state);
    }

    public int points() {
        return Math.round(points);
    }

    public XPu xpu() {
        return new XPu(points());
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(new Param("xp", points, 10f, 0f, 100000f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        this.on = on;
        this.points = CreativeSource.clamp(params().get(0), values[0]);
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("Points", points);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        points = tag.contains("Points") ? tag.getFloat("Points") : 100f;
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
