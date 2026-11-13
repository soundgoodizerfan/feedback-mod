// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The machine the player last had open, so the recipe browser can set that machine's reach beside what a
 * recipe asks for (philosophy 6, the diagnostic recipe screen). Client only; JEI replaces the machine's
 * screen when it opens, so the link outlives the screen for a minute of game time, then lapses.
 */
public final class LinkedMachine {

    /** How long a machine stays linked after its screen was last open. */
    private static final long LINK_TICKS = 1200;

    /** A machine's name and the hottest it can run, in Tu: an equipment spec, free like every spec. */
    public record Reach(Component name, float ceilingTu) {
    }

    private static Reach reach;
    private static long seen;

    private LinkedMachine() {
    }

    public static void link(Component name, float ceilingTu) {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;
        reach = new Reach(name, ceilingTu);
        seen = level.getGameTime();
    }

    /** The linked machine, or null when none is open or the link has lapsed. */
    @Nullable
    public static Reach current() {
        var level = Minecraft.getInstance().level;
        return reach == null || level == null || level.getGameTime() - seen > LINK_TICKS || level.getGameTime() < seen ? null : reach;
    }
}
