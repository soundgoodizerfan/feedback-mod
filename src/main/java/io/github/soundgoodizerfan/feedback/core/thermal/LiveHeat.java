// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * What a stack still inside a heated holder is at, as the client sees it. Such a stack carries no heat stamp (it is stamped as it
 * leaves, so it stacks like any other while it soaks), so the holder's menu sends each slot's temperature and the client notes it
 * against the stack it draws. {@link ItemHeat#get} answers with it, so tint and tooltip need nothing of their own. The server's
 * figure is sent and not simulated: the fire's roll, the air blown and the heat exchanged are known there only. Never written to
 * the stack. Client levels only, on the client thread.
 */
public final class LiveHeat {

    /** A reading not renewed for this many ticks is dropped: the screen that gave it has closed or the stack has left the slot. */
    private static final long LASTS_TICKS = 2L;

    private record Reading(float tu, long at) {
    }

    /** Keyed by the stack object itself: {@code ItemStack} does not override {@code equals}. */
    private static final Map<ItemStack, Reading> SHOWN = Collections.synchronizedMap(new WeakHashMap<>());

    private LiveHeat() {
    }

    /** The menu data a holder sends: one temperature per slot, whole Tu, for its first {@code slots} slots. */
    public static ContainerData readings(ThermalBody body, int slots) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return Math.round(body.slotTemperature(index).value());
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return slots;
            }
        };
    }

    /** Note {@code data}'s readings against the stacks in the menu's leading slots; a screen calls this before drawing them. */
    public static void show(List<Slot> slots, ContainerData data, Level level) {
        if (level == null || !level.isClientSide)
            return;
        for (int i = 0; i < data.getCount(); i++) {
            ItemStack stack = slots.get(i).getItem();
            // Zero is a slot whose reading has not arrived yet.
            if (!stack.isEmpty() && data.get(i) > 0)
                SHOWN.put(stack, new Reading(data.get(i), level.getGameTime()));
        }
    }

    /** The reading held for this stack, or null; always null off the client. */
    static @Nullable Tu of(ItemStack stack, Level level) {
        if (!level.isClientSide)
            return null;
        Reading reading = SHOWN.get(stack);
        return reading == null || level.getGameTime() - reading.at() > LASTS_TICKS ? null : new Tu(reading.tu());
    }
}
