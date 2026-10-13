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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The press's four slots and the hotbar, plus its position and die set at open. */
public class ReloadingPressMenu extends AbstractContainerMenu {

    public static final int[][] SLOT_XY = {{26, 20}, {26, 44}, {26, 68}, {134, 44}};
    public static final int HOTBAR_X = 8, HOTBAR_Y = 142;

    private final Container container;
    private final BlockPos pos;
    private final int openedBore;
    private final Player player;

    public ReloadingPressMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(4), buf.readBlockPos(), buf.readVarInt());
    }

    public ReloadingPressMenu(int id, Inventory inventory, ReloadingPressBlockEntity press) {
        this(id, inventory, press, press.getBlockPos(), press.boreMm());
    }

    private ReloadingPressMenu(int id, Inventory inventory, Container container, BlockPos pos, int bore) {
        super(FMenus.RELOADING_PRESS.get(), id);
        this.player = inventory.player;
        this.container = container;
        this.pos = pos;
        this.openedBore = bore;
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            addSlot(new Slot(container, i, SLOT_XY[i][0], SLOT_XY[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ReloadingPressBlockEntity.fits(slot, stack, ClientMaterials.on(player.level()));
                }
            });
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int openedBore() {
        return openedBore;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index < 4) {
            if (!moveItemStackTo(held, 4, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            boolean placed = false;
            for (int s = 0; s < 3 && !placed; s++)
                if (ReloadingPressBlockEntity.fits(s, held, ClientMaterials.on(player.level())))
                    placed = moveItemStackTo(held, s, s + 1, false);
            if (!placed)
                return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
