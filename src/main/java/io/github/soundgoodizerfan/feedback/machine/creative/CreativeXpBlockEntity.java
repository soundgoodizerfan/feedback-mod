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
 * #points} points, until the container stands at {@link #level()}. On an XP network it pins the whole run to its level. Operator tab only; the first thing built for XP, before any real producer (digest).
 */
public class CreativeXpBlockEntity extends BlockEntity implements CreativeSource, io.github.soundgoodizerfan.feedback.core.xp.XpNode {

    @Nullable
    private io.github.soundgoodizerfan.feedback.core.xp.XpNetwork network;
    private boolean unloaded;

    private boolean on = true;
    private float points = 100f;
    private float fillLevel = 30f;

    public CreativeXpBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_XP.get(), pos, state);
    }

    public int points() {
        return Math.round(points);
    }

    /** The level a container is filled up to. */
    public float level() {
        return fillLevel;
    }

    public XPu xpu() {
        return new XPu(points());
    }

    @Override
    public float pinnedLevel() {
        return on ? fillLevel : Float.NaN;
    }

    @Nullable
    @Override
    public io.github.soundgoodizerfan.feedback.core.xp.XpNetwork network() {
        return network;
    }

    @Override
    public void network(@Nullable io.github.soundgoodizerfan.feedback.core.xp.XpNetwork network) {
        this.network = network;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            io.github.soundgoodizerfan.feedback.core.xp.XpNetworks.of(level).queue(worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (unloaded)
                io.github.soundgoodizerfan.feedback.core.xp.XpNetworks.of(level).unloaded(this);
            else
                io.github.soundgoodizerfan.feedback.core.xp.XpNetworks.of(level).removed(this);
        }
        super.setRemoved();
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(new Param("xp", points, 10f, 0f, 100000f), new Param("level", fillLevel, 1f, 0f, 100000f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        this.on = on;
        this.points = CreativeSource.clamp(params().get(0), values[0]);
        this.fillLevel = CreativeSource.clamp(params().get(1), values[1]);
        setChanged();
        if (level != null && !level.isClientSide)
            io.github.soundgoodizerfan.feedback.core.xp.XpNetworks.of(level).queue(worldPosition);
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("Points", points);
        tag.putFloat("Level", fillLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        points = tag.contains("Points") ? tag.getFloat("Points") : 100f;
        fillLevel = tag.contains("Level") ? tag.getFloat("Level") : 30f;
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
