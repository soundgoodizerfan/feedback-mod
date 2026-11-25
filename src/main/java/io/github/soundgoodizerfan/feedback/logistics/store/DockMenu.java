// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The dock's screen: its own aisle's stock and queue. */
public class DockMenu extends StoreMenu {

    public DockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public DockMenu(int id, Inventory inv, BlockPos pos) {
        super(FStore.DOCK_MENU.get(), id, inv.player, pos);
    }

    @Nullable
    private DockBlockEntity dock() {
        return player.level().getBlockEntity(pos) instanceof DockBlockEntity d ? d : null;
    }

    @Override
    protected boolean ask(ItemStack item, int count) {
        DockBlockEntity d = dock();
        return d == null || d.request(item, count, 0) >= 0;
    }

    @Override
    protected void drop(int key) {
        DockBlockEntity d = dock();
        if (d != null)
            d.cancel(key);
    }

    @Override
    protected Snapshot look() {
        DockBlockEntity d = dock();
        if (d == null)
            return null;
        Map<ItemStack, StoreView.Line> stock = lines();
        for (Lot l : d.stock())
            stock.put(l.item, new StoreView.Line(l.item, List.of(new StoreView.Share(0, l.count))));
        List<StoreView.Queued> queue = new ArrayList<>();
        for (DockBlockEntity.Request r : d.requests())
            queue.add(new StoreView.Queued(r.item, r.remaining, 0, r.order, r.id));
        // The one source's name is the dock's own, empty while it has none: the screen's name box shows it.
        return new Snapshot(stock, queue, List.of(d.source(d.customName() != null ? d.customName() : Component.empty())), d.state());
    }

    public void rename(String name) {
        DockBlockEntity d = dock();
        if (d != null)
            d.rename(name);
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide || dock() != null && p.distanceToSqr(pos.getCenter()) < 64.0;
    }
}
