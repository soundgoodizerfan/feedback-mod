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
import io.github.soundgoodizerfan.feedback.registry.FTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Gun Stand's paper and sample slots and the hotbar, plus the stand's position and its design at open. */
public class GunStandMenu extends AbstractContainerMenu {

    public static final int PAPER_X = 72, PAPER_Y = 118, SAMPLE_X = 140, SAMPLE_Y = 96, HOTBAR_X = 39, HOTBAR_Y = 214;

    private final Container container;
    private final BlockPos pos;
    private final GunDesign opened;
    private final Player player;

    /** Client-side open: position and design come in the open packet. */
    public GunStandMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(2), buf.readBlockPos(), GunDesign.STREAM_CODEC.decode(buf));
    }

    /** Server-side open. */
    public GunStandMenu(int id, Inventory inventory, GunStandBlockEntity stand) {
        this(id, inventory, stand, stand.getBlockPos(), stand.design());
    }

    private GunStandMenu(int id, Inventory inventory, Container container, BlockPos pos, GunDesign opened) {
        super(FMenus.GUN_STAND.get(), id);
        this.player = inventory.player;
        this.container = container;
        this.pos = pos;
        this.opened = opened;
        addSlot(new Slot(container, GunStandBlockEntity.PAPER, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FTags.BLUEPRINT_PAPER);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, GunStandBlockEntity.SAMPLE, SAMPLE_X, SAMPLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isSample(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
    }

    /** Whether {@code stack} names a metal a gun can be made of, as this side knows the materials. */
    public boolean isSample(ItemStack stack) {
        return GunDesign.sampleMaterial(ClientMaterials.on(player.level()), stack).isPresent();
    }

    public BlockPos getPos() {
        return pos;
    }

    public GunDesign opened() {
        return opened;
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
        if (index < 2) {
            if (!moveItemStackTo(held, 2, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.BLUEPRINT_PAPER)) {
            if (!moveItemStackTo(held, 0, 1, false))
                return ItemStack.EMPTY;
        } else if (isSample(held)) {
            if (!moveItemStackTo(held, 1, 2, false))
                return ItemStack.EMPTY;
        } else
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
