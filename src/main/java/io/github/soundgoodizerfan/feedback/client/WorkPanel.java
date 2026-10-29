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
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One kind of work a {@link BenchScreen} hosts in place ({@code docs/gui_bench.md}): the forging rhythm. The panel draws around the stock on
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
