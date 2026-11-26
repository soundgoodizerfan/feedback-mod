// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.net.StoreViewPayload;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A warehouse screen's menu: no slots. The server looks at its block now and then and sends the whole stock list once,
 * then only the lines that changed; the client keeps the list and searches it itself.
 */
public abstract class StoreMenu extends AbstractContainerMenu {

    private static final int LOOK_TICKS = 10;

    /** What a menu's block shows at one moment, stock keyed by stack. */
    protected record Snapshot(Map<ItemStack, StoreView.Line> stock, List<StoreView.Queued> queue, List<StoreView.Source> sources,
            StoreView.State state) {
    }

    protected final BlockPos pos;
    protected final Player player;

    /** Server: what the client has been told. */
    @Nullable
    private Snapshot sent;
    private int wait;

    /** Client: the list as built up from the server's words, and a count of them. */
    private final Map<ItemStack, StoreView.Line> stock = lines();
    private StoreView view = new StoreView(true, List.of(), List.of(), List.of(), StoreView.State.READY);
    private int version;

    protected StoreMenu(MenuType<?> type, int id, Player player, BlockPos pos) {
        super(type, id);
        this.player = player;
        this.pos = pos;
    }

    public static Map<ItemStack, StoreView.Line> lines() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
    }

    /** The block as it stands, or null once it is gone. */
    @Nullable
    protected abstract Snapshot look();

    /** Asks for {@code count} of {@code item}; false when a queue had no place for it. */
    protected abstract boolean ask(ItemStack item, int count);

    /** Drops the queue line named {@code key}. */
    protected abstract void drop(int key);

    public void request(ItemStack item, int count) {
        if (!ask(item, count) && player instanceof ServerPlayer sp)
            HudReadoutPayload.send(sp, Component.translatable("message.feedback.dock.queue_full"), 60);
        wait = 0;
    }

    public void cancel(int key) {
        drop(key);
        wait = 0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || --wait > 0)
            return;
        wait = LOOK_TICKS;
        Snapshot now = look();
        if (now == null)
            return;
        List<StoreView.Line> changed = new ArrayList<>();
        for (Map.Entry<ItemStack, StoreView.Line> e : now.stock.entrySet()) {
            StoreView.Line old = sent == null ? null : sent.stock.get(e.getKey());
            if (old == null || !old.shares().equals(e.getValue().shares()))
                changed.add(e.getValue());
        }
        if (sent != null)
            for (Map.Entry<ItemStack, StoreView.Line> e : sent.stock.entrySet())
                if (!now.stock.containsKey(e.getKey()))
                    changed.add(new StoreView.Line(e.getValue().item(), 0f, List.of()));
        if (sent != null && changed.isEmpty() && sent.state == now.state && sent.sources.equals(now.sources) && same(sent.queue, now.queue))
            return;
        PacketDistributor.sendToPlayer(sp, new StoreViewPayload(containerId,
                new StoreView(sent == null, changed, now.queue, now.sources, now.state)));
        sent = now;
    }

    private static boolean same(List<StoreView.Queued> a, List<StoreView.Queued> b) {
        if (a.size() != b.size())
            return false;
        for (int i = 0; i < a.size(); i++)
            if (!a.get(i).same(b.get(i)))
                return false;
        return true;
    }

    // --- the client's side ------------------------------------------------------------------

    public void accept(StoreView next) {
        if (next.full())
            stock.clear();
        for (StoreView.Line line : next.stock())
            if (line.shares().isEmpty())
                stock.remove(line.item());
            else
                stock.put(line.item(), line);
        view = next;
        version++;
    }

    /** The last word: queue, sources and state (its stock is only what changed; read {@link #stock}). */
    public StoreView view() {
        return view;
    }

    public Collection<StoreView.Line> stock() {
        return stock.values();
    }

    public int version() {
        return version;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }
}
