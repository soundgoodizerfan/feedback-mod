// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.net.DockViewPayload;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** The dock's screen: no slots. The server sends a {@link DockView} when what it shows changes. */
public class DockMenu extends AbstractContainerMenu {

    private static final int LOOK_TICKS = 10;

    private final BlockPos pos;
    private final Player player;
    private DockView view = DockView.EMPTY;
    private DockView sent;
    private int wait;

    public DockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public DockMenu(int id, Inventory inv, BlockPos pos) {
        super(FStore.DOCK_MENU.get(), id);
        this.pos = pos;
        this.player = inv.player;
    }

    public DockView view() {
        return view;
    }

    public void accept(DockView next) {
        view = next;
    }

    @org.jetbrains.annotations.Nullable
    private DockBlockEntity dock() {
        return player.level().getBlockEntity(pos) instanceof DockBlockEntity d ? d : null;
    }

    /** A retrieval asked for from the screen. */
    public void request(ItemStack item, int count) {
        DockBlockEntity d = dock();
        if (d != null) {
            if (!d.request(item, count) && player instanceof ServerPlayer sp)
                io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(sp,
                        net.minecraft.network.chat.Component.translatable("message.feedback.dock.queue_full"), 60);
            wait = 0;
        }
    }

    public void cancel(int index) {
        DockBlockEntity d = dock();
        if (d != null) {
            d.cancel(index);
            wait = 0;
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || --wait > 0)
            return;
        wait = LOOK_TICKS;
        DockBlockEntity d = dock();
        if (d == null)
            return;
        DockView now = view(d);
        if (sent == null || !now.same(sent)) {
            sent = now;
            PacketDistributor.sendToPlayer(sp, new DockViewPayload(containerId, now));
        }
    }

    private static DockView view(DockBlockEntity d) {
        List<DockView.Line> stock = new ArrayList<>();
        for (Lot l : d.stock())
            stock.add(new DockView.Line(l.item, l.count));
        List<DockView.Line> queue = new ArrayList<>();
        for (DockBlockEntity.Request r : d.requests())
            queue.add(new DockView.Line(r.item, r.remaining));
        Aisle aisle = d.aisle();
        float speed = d.speed();
        float far = aisle.racks().isEmpty() ? 0f : aisle.racks().get(aisle.racks().size() - 1).distance();
        int trip = speed <= 0f ? 0 : Math.round(2f * far / speed);
        return new DockView(stock, queue, d.state(),aisle.racks().size(), trip);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide || dock() != null && p.distanceToSqr(pos.getCenter()) < 64.0;
    }
}
