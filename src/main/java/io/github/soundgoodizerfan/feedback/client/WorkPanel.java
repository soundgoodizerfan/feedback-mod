// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One kind of work a {@link BenchScreen} hosts in place ({@code docs/gui/gui_bench.md}): the forging rhythm. The panel draws around the stock on
 * the bench and covers nothing; the bench is the frame and offers each panel its clicks, keys and an overlay drawn over the bench while it
 * is {@link #busy}. Every hook is optional.
 */
interface WorkPanel {

    /** Once per tick: advance anything running. */
    default void tick() { }

    /** A click; true if taken. Offered before the bench's own handling, so a running panel can swallow everything. */
    default boolean click(double mouseX, double mouseY, int button) {
        return false;
    }

    /** A key; true if taken. */
    default boolean key(int key, int scan, int modifiers) {
        return false;
    }

    /** Whether the panel is mid-work and the bench should hold still: the fan goes dim and nothing on it takes a click. */
    default boolean busy() {
        return false;
    }

    /** Drawn over the bench on a raised layer while {@link #busy}. */
    default void overlay(GuiGraphics g) { }
}
