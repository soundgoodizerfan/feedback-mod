// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A gauge linked to what it shows: the figure, the tick and the colour are read every frame, so a screen or HUD says
 * what the gauge is of and the skin ({@link GuiSkin#gauge}) decides how it looks. A temperature bar, a progress bar and
 * a stored-charge bar are the same widget with different suppliers.
 */
public final class Gauge {

    private final DoubleSupplier fill;
    private DoubleSupplier mark = () -> -1;
    private IntSupplier lead;
    private int ticks;
    private boolean vertical;

    private Gauge(DoubleSupplier fill) {
        this.fill = fill;
    }

    /** A gauge filled to {@code fill}, 0..1. */
    public static Gauge of(DoubleSupplier fill) {
        return new Gauge(fill);
    }

    /** A tick at {@code mark} 0..1 with the part past it drawn as over the limit; negative for none. */
    public Gauge mark(DoubleSupplier mark) {
        this.mark = mark;
        return this;
    }

    /** The fill colour, instead of the skin's ink. */
    public Gauge lead(IntSupplier lead) {
        this.lead = lead;
        return this;
    }

    /** Graduations along the side, one a band, for a gauge that steps rather than slides. */
    public Gauge ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    /** Filling upward rather than rightward. */
    public Gauge vertical() {
        vertical = true;
        return this;
    }

    /** Draws it in {@code skin}, its fill area at (x, y), {@code w} by {@code h} inside the well's rim. */
    public void render(GuiGraphics g, GuiSkin skin, int x, int y, int w, int h) {
        skin.gauge(g, x, y, w, h, vertical, (float) fill.getAsDouble(), (float) mark.getAsDouble(),
                lead == null ? skin.ink() : lead.getAsInt(), ticks);
    }
}
