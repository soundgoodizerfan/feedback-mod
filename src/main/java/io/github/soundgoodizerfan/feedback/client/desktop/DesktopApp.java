// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;

/**
 * A program the desktop can run: what it is called, its pictogram, whether the launcher menu lists it, how to make its window, and whether
 * it is {@code available} now (a Computer's disk apps are, while their disk is in a bay). An unavailable app is not in the menu, cannot be
 * opened, and has its window closed. The window's size and first position are asked of it ({@link DesktopWindow#DesktopWindow}), not of
 * the registry. One window per app: opening an app that is already open raises it.
 */
public record DesktopApp(String id, Component name, Glyph glyph, boolean inMenu, Supplier<DesktopWindow> factory, BooleanSupplier available) {

    /** An app that is always there. */
    public DesktopApp(String id, Component name, Glyph glyph, boolean inMenu, Supplier<DesktopWindow> factory) {
        this(id, name, glyph, inMenu, factory, () -> true);
    }
}
