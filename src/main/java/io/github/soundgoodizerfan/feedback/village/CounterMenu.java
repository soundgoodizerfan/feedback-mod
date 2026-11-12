// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The two sides of a Counter. <b>Buy</b> (front or top click): the shelf with the keeper's price on each slot; a click pays emeralds from
 * the player's inventory into the till and takes the goods. <b>Keeper</b> (back click): stock the shelf, set a price for each slot, take the
 * till and the returns. Which one opens is decided by the face clicked; nothing asks who the player is.
 */
public class CounterMenu extends AbstractContainerMenu {

    public static final int BUY = 0, KEEPER = 1;
    /** Button ids: {@code slot * 4 + k} moves a price by -1, +1, -8, +8 (keeper); {@code BUY_ONE + slot} and {@code BUY_STACK + slot} buy. */
    public static final int BUY_ONE = 40, BUY_STACK = 50;
    public static final int SHELF_X = 8, SHELF_Y = 20, PRICE_BTN_Y = 40, PRICE_TEXT_Y = 52;
    public static final int TILL_LABEL_Y = 64, TILL_Y = 74, RETURN_LABEL_Y = 94, RETURN_Y = 104;

    private final int mode;
    @Nullable
    private final CounterBlockEntity counter;
    private final ContainerData data;
    private final BlockPos pos;
    private final Player player;
    private final int shelfSlots;
    private final int invY;

    /** Client side: the mode and position ride in the open packet. */
    public CounterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readVarInt(), buf.readBlockPos());
    }

    public CounterMenu(int id, Inventory inv, int mode, BlockPos pos) {
        super(FMenus.COUNTER.get(), id);
        this.mode = mode;
        this.pos = pos;
        this.player = inv.player;
        this.counter = inv.player.level().getBlockEntity(pos) instanceof CounterBlockEntity c && !inv.player.level().isClientSide ? c : null;
        this.data = counter != null ? counter.data() : new SimpleContainerData(CounterBlockEntity.SHELF * 2);
        ItemStackHandler shelf = counter != null ? counter.shelf() : new ItemStackHandler(CounterBlockEntity.SHELF);
        ItemStackHandler till = counter != null ? counter.tillSlots() : new ItemStackHandler(9);
        ItemStackHandler returns = counter != null ? counter.returnSlots() : new ItemStackHandler(9);
        for (int i = 0; i < CounterBlockEntity.SHELF; i++) {
            int x = SHELF_X + i * 18;
            addSlot(mode == BUY ? new SlotItemHandler(shelf, i, x, SHELF_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }
            } : new SlotItemHandler(shelf, i, x, SHELF_Y));
        }
        shelfSlots = slots.size();
        if (mode == KEEPER) {
            for (int i = 0; i < 9; i++)
                addSlot(new SlotItemHandler(till, i, SHELF_X + i * 18, TILL_Y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            for (int i = 0; i < 9; i++)
                addSlot(new SlotItemHandler(returns, i, SHELF_X + i * 18, RETURN_Y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        }
        invY = mode == KEEPER ? 134 : 96;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, invY + 58));
        addDataSlots(data);
    }

    public int mode() {
        return mode;
    }

    public int invY() {
        return invY;
    }

    public int shelfSlotCount() {
        return shelfSlots;
    }

    public int imageHeight() {
        return invY + 58 + 18 + 8;
    }

    /** The keeper's price for a shelf slot, 0 when unset. */
    public int price(int slot) {
        return data.get(slot);
    }

    /** The market price of what stands in a shelf slot, 0 when no villager pool wants it. */
    public int market(int slot) {
        return data.get(CounterBlockEntity.SHELF + slot);
    }

    /** What a player pays each in a slot: the keeper's price, else the market's. */
    public int shown(int slot) {
        return price(slot) > 0 ? price(slot) : Math.max(1, market(slot));
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (counter == null || counter.isRemoved())
            return false;
        if (mode == KEEPER && id >= 0 && id < CounterBlockEntity.SHELF * 4) {
            int slot = id / 4;
            int step = switch (id % 4) {
                case 0 -> -1;
                case 1 -> 1;
                case 2 -> -8;
                default -> 8;
            };
            counter.price(slot, counter.price(slot) + step);
            return true;
        }
        if (mode == BUY && id >= BUY_ONE && id < BUY_STACK + CounterBlockEntity.SHELF) {
            boolean stack = id >= BUY_STACK;
            buy(p, id - (stack ? BUY_STACK : BUY_ONE), stack);
            return true;
        }
        return false;
    }

    private void buy(Player p, int slot, boolean stack) {
        ItemStack on = counter.shelf().getStackInSlot(slot);
        if (on.isEmpty())
            return;
        int each = counter.playerPrice(slot);
        int n = stack ? on.getCount() : 1;
        // A stack buy takes what the player can pay for.
        int have = 0;
        for (ItemStack s : p.getInventory().items)
            if (s.is(Items.EMERALD))
                have += s.getCount();
        n = Math.min(n, have / each);
        if (n <= 0)
            return;
        if (!counter.payTill(n * each))
            return; // a full till refuses the sale, as it stalls a villager
        VillageEconomy.takeEmeralds(p, n * each);
        ItemStack goods = counter.shelf().extractItem(slot, n, false);
        p.getInventory().placeItemBackInInventory(goods);
        p.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (mode == BUY)
            return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = slots.size() - 36;
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, shelfSlots, false)) {
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
        return p.level().isClientSide || (counter != null && !counter.isRemoved() && p.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
