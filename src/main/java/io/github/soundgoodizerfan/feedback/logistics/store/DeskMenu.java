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

/** The catalogue desk's screen: the stock of every linked dock as one list, and the shares of its orders. */
public class DeskMenu extends StoreMenu {

    public DeskMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public DeskMenu(int id, Inventory inv, BlockPos pos) {
        super(FStore.DESK_MENU.get(), id, inv.player, pos);
    }

    @Nullable
    private DeskBlockEntity desk() {
        return player.level().getBlockEntity(pos) instanceof DeskBlockEntity d ? d : null;
    }

    @Override
    protected boolean ask(ItemStack item, int count) {
        DeskBlockEntity d = desk();
        return d == null || d.order(item, count);
    }

    @Override
    protected void drop(int key) {
        DeskBlockEntity d = desk();
        if (d == null || d.down() != null)
            return;
        for (DeskBlockEntity.Linked linked : d.docks())
            if (linked.dock() != null)
                linked.dock().cancel(key);
    }

    @Override
    protected Snapshot look() {
        DeskBlockEntity d = desk();
        if (d == null)
            return null;
        StoreView.State down = d.down();
        if (down != null)
            return new Snapshot(lines(), List.of(), List.of(), down);
        List<DeskBlockEntity.Linked> shown = d.docks();
        List<List<Lot>> stocks = DeskBlockEntity.stocks(shown);
        Map<ItemStack, StoreView.Line> stock = lines();
        List<StoreView.Queued> queue = new ArrayList<>();
        List<StoreView.Source> sources = new ArrayList<>();
        for (int s = 0; s < shown.size(); s++) {
            DeskBlockEntity.Linked linked = shown.get(s);
            DockBlockEntity dock = linked.dock();
            Component name = dock != null && dock.customName() != null ? dock.customName()
                    : Component.translatable("screen.feedback.desk.dock", s + 1, linked.distance());
            if (dock == null) {
                sources.add(new StoreView.Source(name, StoreView.State.AWAY, 0, 0, List.of()));
                continue;
            }
            sources.add(dock.source(name));
            for (Lot l : stocks.get(s)) {
                StoreView.Line line = stock.get(l.item);
                if (line == null) {
                    line = new StoreView.Line(l.item, Goods.unitMass(l.item), new ArrayList<>(1));
                    stock.put(l.item, line);
                }
                line.shares().add(new StoreView.Share(s, l.count, l.marks));
            }
            for (DockBlockEntity.Request r : dock.requests())
                queue.add(new StoreView.Queued(r.item, r.remaining, s, r.order, r.id));
        }
        return new Snapshot(stock, queue, sources, shown.isEmpty() ? StoreView.State.NO_DOCKS : StoreView.State.READY);
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide || desk() != null && p.distanceToSqr(pos.getCenter()) < 64.0;
    }
}
