// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Several XP containers wired in parallel and shown on the network as one (roadmap X7): capacitances add, so the bank stands at the containers' common
 * level and holds their sum, like capacitors across one line. The stores live on the seated stacks, so taking one takes its share; nothing ticks.
 * Points the network adds or takes are settled back across the stacks by {@link XpStore#join}.
 */
public class CrystalBankBlockEntity extends BlockEntity implements XpNode {

    /** Containers a bank seats. */
    public static final int SLOTS = 4;

    private final NonNullList<ItemStack> seated = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    @Nullable
    private XpNetwork network;
    private boolean unloaded;

    public CrystalBankBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRYSTAL_BANK.get(), pos, state);
    }

    public NonNullList<ItemStack> seated() {
        return seated;
    }

    /** Seats {@code stack} (one item) in the first free slot if it stores XP. True if it went in. */
    public boolean seat(ItemStack stack) {
        if (XpItems.store(stack) == null)
            return false;
        for (int i = 0; i < SLOTS; i++)
            if (seated.get(i).isEmpty()) {
                seated.set(i, stack.copyWithCount(1));
                settle(stores());
                changed();
                return true;
            }
        return false;
    }

    /** Lets the seated containers settle to one level, conserving their points (the wire between them). */
    private void settle(List<XpStore> filled) {
        List<ItemStack> held = holders();
        List<XpStore> settled = XpStore.join(filled).stores();
        for (int i = 0; i < held.size(); i++)
            XpItems.set(held.get(i), settled.get(i));
    }

    /** Takes the last seated container out, or empty if the bank holds none. */
    public ItemStack take() {
        settle(stores());
        for (int i = SLOTS - 1; i >= 0; i--)
            if (!seated.get(i).isEmpty()) {
                ItemStack out = seated.get(i);
                seated.set(i, ItemStack.EMPTY);
                changed();
                return out;
            }
        return ItemStack.EMPTY;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            XpNetworks.of(level).queue(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The seated containers' stores, in slot order; the stacks they belong to are {@link #holders}. */
    private List<XpStore> stores() {
        List<XpStore> out = new ArrayList<>(SLOTS);
        for (ItemStack s : seated)
            if (!s.isEmpty())
                out.add(XpItems.store(s));
        return out;
    }

    private List<ItemStack> holders() {
        List<ItemStack> out = new ArrayList<>(SLOTS);
        for (ItemStack s : seated)
            if (!s.isEmpty())
                out.add(s);
        return out;
    }

    @Nullable
    @Override
    public XpStore xpStore() {
        List<XpStore> stores = stores();
        if (stores.isEmpty())
            return null;
        int points = 0, max = 0;
        float per = 0f;
        for (XpStore s : stores) {
            points += s.points();
            max += s.maxPoints();
            per += s.perLevel();
        }
        return new XpStore(points, per, max);
    }

    @Override
    public void xpSet(XpStore store) {
        // Put the new total in slot order, then let the containers settle to one level.
        int left = store.points();
        List<XpStore> filled = new ArrayList<>(SLOTS);
        for (XpStore s : stores()) {
            int put = Math.min(left, s.maxPoints());
            filled.add(s.with(put));
            left -= put;
        }
        settle(filled);
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        ContainerHelper.saveAllItems(tag, seated, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seated.clear();
        ContainerHelper.loadAllItems(tag, seated, registries);
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
