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
package io.github.soundgoodizerfan.feedback.village;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.net.BoardStatePayload;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ledger board's container: one delivery slot (a made piece for a contract, or an illager banner for a raid's bounty) and the player's
 * inventory. What the screen shows is a {@link BoardView} the server sends; a delivery is a menu button naming the contract.
 */
public class BoardMenu extends AbstractContainerMenu {

    public static final int WIDTH = 270, HEIGHT = 246;
    public static final int SLOT_X = 14, SLOT_Y = 126, INV_X = (WIDTH - 162) / 2, INV_Y = 162, HOTBAR_Y = 220;
    /** Button ids: {@code 0..3} deliver to that contract, {@link #BANNER} delivers a banner for the bounty. */
    public static final int BANNER = 10;

    private final Container delivery = new SimpleContainer(1);
    private final BlockPos pos;
    private final Player player;
    private BoardView view = BoardView.EMPTY;
    private boolean slotLive = true;

    public BoardMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public BoardMenu(int id, Inventory inv, BlockPos pos) {
        super(FMenus.BOARD.get(), id);
        this.pos = pos;
        this.player = inv.player;
        addSlot(new Slot(delivery, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return slotLive;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, 9 + row * 9 + col, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
    }

    public BoardView view() {
        return view;
    }

    public void accept(BoardView next) {
        view = next;
    }

    /** Client side: whether the tab showing uses the delivery slot (Contracts only). */
    public void slotLive(boolean live) {
        slotLive = live;
    }

    @Nullable
    private LedgerBoardBlockEntity board() {
        return player.level().getBlockEntity(pos) instanceof LedgerBoardBlockEntity b ? b : null;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        LedgerBoardBlockEntity board = board();
        if (board == null || !(p.level() instanceof ServerLevel level) || !(p instanceof ServerPlayer sp))
            return false;
        ItemStack held = delivery.getItem(0);
        if (id == BANNER) {
            int paid = board.bounty(held);
            if (paid > 0)
                p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, paid));
            send(sp, board.view(level, p.getUUID()).withResult(paid > 0 ? 3 : -2, paid));
            return true;
        }
        if (held.isEmpty())
            return false;
        LedgerBoardBlockEntity.Delivered d = board.deliver(level, held, p.getUUID());
        if (d.paid() > 0)
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, d.paid()));
        if (!d.reward().isEmpty())
            p.getInventory().placeItemBackInInventory(d.reward());
        if (held.isEmpty())
            delivery.setItem(0, ItemStack.EMPTY);
        send(sp, board.view(level, p.getUUID()).withResult(d.fate() == LedgerBoardBlockEntity.Delivered.Fate.TAKEN ? d.grade() : -2, d.paid()));
        return true;
    }

    /** Sends the opening view. */
    public void open(ServerPlayer p) {
        LedgerBoardBlockEntity board = board();
        if (board != null && p.level() instanceof ServerLevel level)
            send(p, board.view(level, p.getUUID()));
    }

    private void send(ServerPlayer p, BoardView v) {
        PacketDistributor.sendToPlayer(p, new BoardStatePayload(containerId, v));
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!slots.get(0).hasItem()) {
            slots.get(0).setByPlayer(stack.split(stack.getCount()));
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide || (board() != null && p.distanceToSqr(pos.getCenter()) < 64.0);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        clearContainer(p, delivery);
    }
}
