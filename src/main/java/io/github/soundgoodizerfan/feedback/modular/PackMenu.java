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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A worn pack's contents: the chest layout, rows of fixed slots and, under them, a row of typed store slots. Every slot has a kind
 * ({@link ModularBackpack#accepts}); the kinds ride in the open packet so the client's own slots refuse what the server would,
 * and a pack never appears to go into a pack. A slot no fitting gives (kind -1) is hidden and locked.
 */
public class PackMenu extends ChestMenu {

    private final int[] kinds;

    /** The kind of slot {@code i} (0 fixed, positive a store, -1 locked). */
    public int kindOf(int i) {
        return i < kinds.length ? kinds[i] : 0;
    }

    /** Server: over the real container. */
    public PackMenu(int id, Inventory inv, Container container, int rows, int[] kinds) {
        super(FMenus.PACK.get(), id, inv, container, rows);
        this.kinds = kinds;
        for (int i = 0; i < kinds.length; i++) {
            Slot old = slots.get(i);
            Slot slot = new PackSlot(container, i, old.x, old.y, kinds[i]);
            slot.index = i;
            slots.set(i, slot);
        }
    }

    /** Client: an empty container of the sent shape. */
    public static PackMenu client(int id, Inventory inv, FriendlyByteBuf buf) {
        int rows = buf.readVarInt();
        int[] kinds = new int[rows * 9];
        for (int i = 0; i < kinds.length; i++)
            kinds[i] = buf.readVarInt() - 1;
        return new PackMenu(id, inv, new SimpleContainer(kinds.length), rows, kinds);
    }

    /** What {@link #client} reads. */
    public static void write(FriendlyByteBuf buf, int rows, int[] kinds) {
        buf.writeVarInt(rows);
        for (int k : kinds)
            buf.writeVarInt(k + 1);
    }

    private static final class PackSlot extends Slot {
        private final int kind;

        PackSlot(Container container, int index, int x, int y, int kind) {
            super(container, index, x, y);
            this.kind = kind;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ModularBackpack.accepts(kind, stack);
        }

        @Override
        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
            return kind >= 0;
        }

        @Override
        public boolean isActive() {
            return kind >= 0;
        }
    }
}
