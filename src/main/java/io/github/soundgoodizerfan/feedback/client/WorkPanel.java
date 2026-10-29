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
 * One kind of work a {@link ToolTableScreen} can host ({@code docs/gui_flow_structure.md}): forging, cutting, preparing food,
 * assembling. The screen is the frame (strip, form list, slots, tooltips) and asks each panel its station lists
 * ({@link io.github.soundgoodizerfan.feedback.modular.Station#has}) to draw and handle what is its own. Every hook is optional.
 * Widgets are added in {@link #init} (the screen rebuilds them on every resize; the panel object and its state survive).
 */
interface WorkPanel {

    /** Add this panel's widgets, positioned from the host's panel rectangle. */
    default void init() { }

    /** Once per tick, after the frame has read the contents: show or enable widgets, advance anything running. */
    default void tick() { }

    /** Whether menu mode {@code mode} is this panel's tab: it then draws the left body and the slot labels. */
    default boolean owns(int mode) {
        return false;
    }

    /** The left column in a mode this panel owns. */
    default void body(GuiGraphics g, int mouseX, int mouseY) { }

    /** Slot labels and captions in a mode this panel owns. */
    default void labels(GuiGraphics g) { }

    /** A click; true if taken. Offered before the frame's own handling, so a running panel can swallow everything. */
    default boolean click(double mouseX, double mouseY, int button) {
        return false;
    }

    /** A key; true if taken. */
    default boolean key(int key, int scan, int modifiers) {
        return false;
    }

    /** Whether the panel is mid-work and the frame should hold still: tabs, search and other panels' buttons go dead. */
    default boolean busy() {
        return false;
    }

    /** Drawn over the whole screen on a raised layer while {@link #busy}. */
    default void overlay(GuiGraphics g) { }

    /** A tooltip for the pointer, drawn in place of the frame's; true if drawn. */
    default boolean tooltip(GuiGraphics g, int mouseX, int mouseY) {
        return false;
    }
}
