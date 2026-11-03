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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

/**
 * The brewing stand's menu: a water slot, six ingredient slots (the path runs in the order they were filled), three bottle slots, and the player's inventory. Items go in by
 * moving them here, never by clicking the block with them. What the slots will take is read off the data slots (water, how far the stirring has
 * got), so the client's drag agrees with the server. The marker and the map are not menu state: they ride {@code StandStatePayload}.
 */
public class BrewingStandMenu extends AbstractContainerMenu {

    /** Layout relative to the panel; the screen draws sockets and labels at the same places. */
    public static final int W = 346, H = 272, WATER_X = 282, WATER_Y = 30, ING_X = 280, ING_Y = 62, BOT_X = 280, BOT_Y = 132, INV_X = 92, INV_Y = 188;

    /** The grinding bowl's well, relative to the panel (bottom left, under the Tip out and Pour buttons, left of the inventory). */
    public static final int BOWL_X = 8, BOWL_Y = 180, BOWL_W = 80, BOWL_H = 84;

    /** Plants the bowl holds at once. */
    public static final int MORTAR_SLOTS = 6;

    /** Menu slot ranges: the player's inventory, then the bowl (its slots are inactive: the screen draws them as loose objects, never as sockets). */
    public static final int INV_START = BrewingStandBlockEntity.SLOTS, INV_END = INV_START + 36, MORTAR_START = INV_END;

    private final BlockPos pos;
    private final BrewingStandBlockEntity stand;
    private final Container container;
    /** The grinding bowl's plants, one item each: server owned, synced as slots so the client sees what is in the bowl. */
    private final Container mortar = new SimpleContainer(MORTAR_SLOTS);
    private final ContainerData data;
    private final Player player;

    /** Client side: the position rides in the open packet, the slots arrive synced. */
    public BrewingStandMenu(int id, Inventory inventory, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null);
    }

    public BrewingStandMenu(int id, Inventory inventory, BrewingStandBlockEntity stand) {
        this(id, inventory, stand.getBlockPos(), stand);
    }

    private BrewingStandMenu(int id, Inventory inventory, BlockPos pos, BrewingStandBlockEntity stand) {
        super(FMenus.BREWING_STAND.get(), id);
        this.pos = pos;
        this.stand = stand;
        this.player = inventory.player;
        this.container = stand != null ? stand.container() : new SimpleContainer(BrewingStandBlockEntity.SLOTS);
        this.data = stand == null ? new SimpleContainerData(3) : new ContainerData() {
            @Override
            public int get(int i) {
                return i == 0 ? stand.water() : i == 1 ? stand.front() : stand.started() ? 1 : 0;
            }

            @Override
            public void set(int i, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
        addDataSlots(data);
        addSlot(new Slot(container, BrewingStandBlockEntity.WATER_SLOT, WATER_X, WATER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return data.get(0) <= 0 && bottlesEmpty() && stack.getItem() instanceof BucketItem bucket && bucket.content.is(FluidTags.WATER);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < BrewingStandBlockEntity.INGREDIENTS; i++) {
            int n = i;
            addSlot(new Slot(container, BrewingStandBlockEntity.FIRST_INGREDIENT + i, ING_X + (i % 2) * 18, ING_Y + (i / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !locked(n) && data.get(0) > 0 && steers(stack);
                }

                @Override
                public boolean mayPickup(Player p) {
                    return !locked(n);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < BrewingStandBlockEntity.BOTTLES; i++)
            addSlot(new Slot(container, BrewingStandBlockEntity.FIRST_BOTTLE + i, BOT_X + i * 18, BOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        for (int i = 0; i < MORTAR_SLOTS; i++)
            addSlot(new Slot(mortar, i, 0, 0) {
                // Never clicked, hovered or drawn as a slot; only the bowl's objects stand for it, and the payloads move its items.
                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }
            });
    }

    /** One plant from the inventory slot {@code menuSlot} (or from the cursor if -1) into the first free place in the bowl. Server side. */
    public void mortarPut(int menuSlot) {
        ItemStack from = menuSlot == -1 ? getCarried() : menuSlot >= INV_START && menuSlot < INV_END ? slots.get(menuSlot).getItem() : ItemStack.EMPTY;
        if (from.isEmpty() || !steers(from))
            return;
        for (int i = 0; i < MORTAR_SLOTS; i++)
            if (mortar.getItem(i).isEmpty()) {
                mortar.setItem(i, from.split(1));
                if (menuSlot != -1)
                    slots.get(menuSlot).setChanged();
                return;
            }
    }

    /**
     * The player took bowl place {@code place} out, crushed {@code steps} twentieths of the way. The client is not trusted beyond this clamp and the 5% steps:
     * a cheated fraction only skips some grinding, which costs nothing important, and is accepted.
     */
    public void mortarTake(int place, int steps) {
        if (place < 0 || place >= MORTAR_SLOTS || mortar.getItem(place).isEmpty())
            return;
        ItemStack out = mortar.removeItemNoUpdate(place);
        float fraction = Math.max(0, Math.min(20, steps)) / 20f;
        if (fraction > 0f)
            out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CRUSHED_FRACTION.get(), fraction);
        player.getInventory().placeItemBackInInventory(out);
        broadcastChanges();
    }

    /** Closing returns whatever is left in the bowl, as it was (the client sends each plant's fraction first, with a take). */
    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide)
            clearContainer(p, mortar);
    }

    private boolean bottlesEmpty() {
        for (int i = 0; i < BrewingStandBlockEntity.BOTTLES; i++)
            if (!container.getItem(BrewingStandBlockEntity.FIRST_BOTTLE + i).isEmpty())
                return false;
        return true;
    }

    /** Only the ingredient being stirred in right now is locked: part of it is already in the water. */
    public boolean locked(int ingredient) {
        return ingredient == data.get(1) && data.get(2) != 0;
    }

    /** Whether the stack's material carries a path for the map: by material property, never by what the item is. */
    public boolean steers(ItemStack stack) {
        return BrewingStandBlockEntity.potionOf(player.level(), stack).isPresent();
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        int inventoryStart = BrewingStandBlockEntity.SLOTS;
        if (index < inventoryStart ? !moveItemStackTo(stack, inventoryStart, INV_END, true) : !moveItemStackTo(stack, 0, BrewingStandBlockEntity.FIRST_BOTTLE, false))
            return ItemStack.EMPTY;
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (stack.getCount() == before.getCount())
            return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player p) {
        return stand == null || !stand.isRemoved() && p.level().getBlockEntity(pos) == stand && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
