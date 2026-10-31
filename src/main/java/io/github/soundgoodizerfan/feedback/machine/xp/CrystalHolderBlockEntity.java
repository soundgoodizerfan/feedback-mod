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
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One slot for any XP storage device, shown on the network as that device's store. The store lives on the seated
 * stack, so taking the stack out takes the XP with it; nothing here ticks.
 */
public class CrystalHolderBlockEntity extends BlockEntity implements XpNode {

    private ItemStack seated = ItemStack.EMPTY;
    @Nullable
    private XpNetwork network;
    private boolean unloaded;

    public CrystalHolderBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRYSTAL_HOLDER.get(), pos, state);
    }

    public ItemStack seated() {
        return seated;
    }

    /** Seats {@code stack} (one item) if the slot is free and the item stores XP. True if it went in. */
    public boolean seat(ItemStack stack) {
        if (!seated.isEmpty() || XpItems.store(stack) == null)
            return false;
        seated = stack.copyWithCount(1);
        changed();
        return true;
    }

    public ItemStack take() {
        ItemStack out = seated;
        seated = ItemStack.EMPTY;
        changed();
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            XpNetworks.of(level).queue(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public XpStore xpStore() {
        return seated.isEmpty() ? null : XpItems.store(seated);
    }

    @Override
    public void xpSet(XpStore store) {
        XpItems.set(seated, store);
        setChanged();
    }

    @Nullable
    @Override
    public XpNetwork network() {
        return network;
    }

    @Override
    public void network(@Nullable XpNetwork network) {
        this.network = network;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!seated.isEmpty())
            tag.put("Seated", seated.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seated = tag.contains("Seated") ? ItemStack.parseOptional(registries, tag.getCompound("Seated")) : ItemStack.EMPTY;
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

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            XpNetworks.of(level).queue(worldPosition);
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
                XpNetworks.of(level).unloaded(this);
            else
                XpNetworks.of(level).removed(this);
        }
        super.setRemoved();
    }
}
