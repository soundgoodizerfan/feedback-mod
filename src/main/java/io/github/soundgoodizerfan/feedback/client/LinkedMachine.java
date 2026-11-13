// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.item.ReachProbeItem;
import io.github.soundgoodizerfan.feedback.machine.Reachable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The machine the recipe browser sets beside what a recipe asks for (philosophy 6, the diagnostic recipe screen): the one a carried
 * reach probe is linked to, else the thermal vessel the player last had open (JEI replaces that screen when it opens, so that link
 * outlives it for a minute of game time, then lapses). Client only.
 */
public final class LinkedMachine {

    /** How long a vessel stays linked after its screen was last open. */
    private static final long LINK_TICKS = 1200;

    /** A machine's name and its reach: an equipment spec, free like every spec. */
    public record Reach(Component name, float tempTu, float pressurePu, float rpm, float flowMbPerRpm, float gearLow, float gearHigh) {

        /** The figure for a requirement's unit, or {@link Reachable.Reach#UNSTATED}. */
        public float of(io.github.soundgoodizerfan.feedback.instrument.Quantity quantity) {
            return switch (quantity) {
                case TEMPERATURE -> tempTu;
                case PRESSURE -> pressurePu;
                case SPEED -> rpm;
                default -> Reachable.Reach.UNSTATED;
            };
        }
    }

    private static Reach screen;
    private static long seen;

    private LinkedMachine() {
    }

    /** A vessel's screen opened: its temperature ceiling is the only reach that screen states. */
    public static void link(Component name, float ceilingTu) {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;
        screen = new Reach(name, ceilingTu, Reachable.Reach.UNSTATED, Reachable.Reach.UNSTATED, Reachable.Reach.UNSTATED, Reachable.Reach.UNSTATED, Reachable.Reach.UNSTATED);
        seen = level.getGameTime();
    }

    /** The linked machine, or null when none is linked or the screen link has lapsed. */
    @Nullable
    public static Reach current() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null)
            return null;
        if (mc.player != null) {
            var inventory = mc.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                if (probe(inventory.getItem(slot)) != null)
                    return probe(inventory.getItem(slot));
        }
        return screen == null || level.getGameTime() - seen > LINK_TICKS || level.getGameTime() < seen ? null : screen;
    }

    @Nullable
    private static Reach probe(ItemStack stack) {
        if (!(stack.getItem() instanceof ReachProbeItem))
            return null;
        var link = stack.get(FDataComponents.REACH.get());
        return link == null ? null : new Reach(Component.translatable(link.nameKey()), link.tempTu(), link.pressurePu(), link.rpm(), link.flowMbPerRpm(), link.gearLow(), link.gearHigh());
    }
}
