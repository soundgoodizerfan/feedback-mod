// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.RadioSetItem;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.SightCell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The battery a radio set transmits on: the same battery stack the gun's powered sights run on ({@link SightCell}, whose cell reader and writer this
 * reuses), loaded by a right click on the set with the battery on the cursor. A keyed set draws {@link RadioTuning#KEYED_DRAW_EU} Eu a second; with
 * no charge it cannot key at all. Listening is free: the ear is the antenna.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RadioCell {

    private RadioCell() {
    }

    /** Whether the set has a cell with charge in it. */
    public static boolean charged(ItemStack set) {
        CompoundTag state = BatteryStack.state(SightCell.cell(set));
        return state != null && BatteryStack.charge(state) > 0f;
    }

    /** Loads or takes out the cell: the same click as {@link SightCell#load}. False when the stack is not a set or the cursor holds something else. */
    public static boolean load(ItemStack set, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!(set.getItem() instanceof RadioSetItem) || action != ClickAction.SECONDARY || !slot.allowModification(player))
            return false;
        if (other.isEmpty()) {
            ItemStack held = SightCell.cell(set);
            if (held.isEmpty())
                return false;
            SightCell.setCell(set, ItemStack.EMPTY);
            access.set(held);
            player.playSound(FSounds.GUN_EGUN_CELL_OUT.get(), 0.8f, 1f);
            return true;
        }
        if (!BatteryStack.is(other))
            return false;
        ItemStack old = SightCell.cell(set);
        SightCell.setCell(set, other);
        access.set(old);
        player.playSound(FSounds.GUN_EGUN_CELL_IN.get(), 0.8f, 1f);
        return true;
    }

    /** Once a second a keyed set takes its draw from its cell; a set whose cell has run flat stops transmitting. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || !RadioNet.keyed(player.getUUID()))
            return;
        ItemStack set = RadioSets.held(player);
        if (set.isEmpty() || !charged(set)) {
            RadioNet.drop(player.getUUID());
            return;
        }
        ItemStack cell = SightCell.cell(set);
        CompoundTag next = BatteryStack.state(cell).copy();
        BatteryStack.draw(next, RadioTuning.KEYED_DRAW_EU);
        cell.set(FDataComponents.BATTERY.get(), next);
        SightCell.setCell(set, cell);
    }
}
