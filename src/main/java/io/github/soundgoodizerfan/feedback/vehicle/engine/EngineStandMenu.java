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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Engine Stand's paper and sample slots and the hotbar, plus the stand's position and its design at open. */
public class EngineStandMenu extends AbstractContainerMenu {

    public static final int PAPER_X = 186, PAPER_Y = 212, SAMPLE_X = 74, SAMPLE_Y = 180, HOTBAR_X = 10, HOTBAR_Y = 212;

    private final Container container;
    private final BlockPos pos;
    private final EngineDesign opened;
    private final io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign openedTransmission;
    private final boolean openedMode;
    private final Player player;

    /** Client-side open: position and design come in the open packet. */
    public EngineStandMenu(int id, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(2), buf.readBlockPos(), EngineDesign.STREAM_CODEC.decode(buf),
                io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.STREAM_CODEC.decode(buf), buf.readBoolean());
    }

    /** Server-side open. */
    public EngineStandMenu(int id, Inventory inventory, EngineStandBlockEntity stand) {
        this(id, inventory, stand, stand.getBlockPos(), stand.design(), stand.transmission(), stand.transmissionMode());
    }

    private EngineStandMenu(int id, Inventory inventory, Container container, BlockPos pos, EngineDesign opened,
                            io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign transmission, boolean mode) {
        super(FMenus.ENGINE_STAND.get(), id);
        this.player = inventory.player;
        this.container = container;
        this.pos = pos;
        this.opened = opened;
        this.openedTransmission = transmission;
        this.openedMode = mode;
        addSlot(new Slot(container, 0, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FTags.BLUEPRINT_PAPER);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, EngineStandBlockEntity.SAMPLE, SAMPLE_X, SAMPLE_Y) {
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

    /** Whether {@code stack} names a metal an engine can be made of, as this side knows the materials. */
    public boolean isSample(ItemStack stack) {
        return EngineDesign.sampleMaterial(io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(player.level()), stack).isPresent();
    }

    public BlockPos getPos() {
        return pos;
    }

    public EngineDesign opened() {
        return opened;
    }

    public io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign openedTransmission() {
        return openedTransmission;
    }

    public boolean openedMode() {
        return openedMode;
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
