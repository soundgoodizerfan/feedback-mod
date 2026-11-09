// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

/**
 * A pixel-scrolled box inside a window: a sunken area, a clip to its inside, a scrollbar along its right edge, and the wheel. A window
 * keeps one per scrolling thing. Usage each frame: {@code int top = area.begin(ui, x, y, w, h)}, draw from {@code top} (the content's
 * y, already offset by the scroll), then {@code area.end(ui, contentHeight)}; the content height is known only after drawing, and the
 * scroll is clamped to it the next frame.
 */
public final class ScrollArea {

    private int scroll, content, x, y, w, h;

    /** Draws the backing and clips to it; returns the y, in window coordinates, that the content's first pixel row sits at. */
    public int begin(Ui ui, int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, content - (h - 4))));
        ui.inset(x, y, w, h);
        ui.clip(x + 1, y + 1, w - 2, h - 2);
        return y + 2 - scroll;
    }

    /** Ends the clip and draws the bar for a content {@code contentHeight} pixels tall. */
    public void end(Ui ui, int contentHeight) {
        ui.unclip();
        content = contentHeight;
        ui.scrollbar(x + w - 4, y + 2, h - 4, contentHeight, h - 4, scroll);
    }

    /** Width available to content, leaving the bar's lane. */
    public int innerWidth() {
        return w - 10;
    }

    public boolean contains(double lx, double ly) {
        return lx >= x && lx < x + w && ly >= y && ly < y + h;
    }

    /** The wheel, one notch = three lines; true if the pointer was over the area. */
    public boolean scrolled(double lx, double ly, double wheel) {
        if (!contains(lx, ly))
            return false;
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(wheel) * 3 * Ui.LINE, Math.max(0, content - (h - 4))));
        return true;
    }

    /** Scrolls to the end the next time it is drawn: a log following its newest line. */
    public void toEnd() {
        scroll = Integer.MAX_VALUE;
    }

    public void reset() {
        scroll = 0;
    }
}
