// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The villager screen's container: one delivery slot that takes a finished modular item, and the
 * player's inventory. What the screen shows is a {@link VillagerView} the server sends; the server
 * resolves the merchant from its entity id on every action and re-checks that it is still near.
 */
public class VillagerMenu extends AbstractContainerMenu {

    public static final int WIDTH = 300, HEIGHT = 246;
    public static final int SLOT_X = 140, SLOT_Y = 98, INV_X = (WIDTH - 162) / 2, INV_Y = 166, HOTBAR_Y = 224;

    private final Container delivery = new SimpleContainer(1);
    private final int entityId;
    private final Player player;
    private VillagerView view = VillagerView.EMPTY;
    private boolean slotLive = true;

    /** Client-side open. */
    public VillagerMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readVarInt());
    }

    public VillagerMenu(int id, Inventory inventory, int entityId) {
        super(FMenus.VILLAGER.get(), id);
        this.entityId = entityId;
        this.player = inventory.player;
        addSlot(new Slot(delivery, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public boolean isActive() {
                return slotLive;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y));
    }

    public ItemStack delivered() {
        return delivery.getItem(0);
    }

    public void consume() {
        delivery.setItem(0, ItemStack.EMPTY);
    }

    /** The merchant, or null if it is gone. */
    public AbstractVillager merchant() {
        Entity e = player.level().getEntity(entityId);
        return e instanceof AbstractVillager m && m.isAlive() ? m : null;
    }

    public VillagerView view() {
        return view;
    }

    public void accept(VillagerView next) {
        view = next;
    }

    /** Client side: whether the tab showing uses the delivery slot (Orders and Sell). */
    public void slotLive(boolean live) {
        slotLive = live;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (slots.get(0).mayPlace(stack) && !slots.get(0).hasItem()) {
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
    public boolean stillValid(Player player) {
        if (player.level().isClientSide)
            return true;
        AbstractVillager m = merchant();
        return m != null && m.distanceToSqr(player) < VillageTuning.REACH * VillageTuning.REACH;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && merchant() instanceof AbstractVillager m && m.getTradingPlayer() == player)
            m.setTradingPlayer(null);
        clearContainer(player, delivery);
    }
}
