// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout helpers for screens and windows that must reflow when their size changes. A layout is worked out from a size, never from
 * absolute screen coordinates: a {@link Rect} is cut into slices, inset, split into columns, or has a child anchored in it, and
 * a {@link Flow} stacks children along a row or column. Everything is integer pixels and allocation-light, so a host recomputes it
 * on resize (not per frame).
 */
public final class Layout {

    private Layout() {
    }

    /** Where a child sits in its parent. The offsets of {@link Rect#at} push the child inward from the edges it is anchored to. */
    public enum Anchor {
        TOP_LEFT, TOP, TOP_RIGHT, LEFT, CENTRE, RIGHT, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT;

        boolean left() {
            return this == TOP_LEFT || this == LEFT || this == BOTTOM_LEFT;
        }

        boolean right() {
            return this == TOP_RIGHT || this == RIGHT || this == BOTTOM_RIGHT;
        }

        boolean top() {
            return this == TOP_LEFT || this == TOP || this == TOP_RIGHT;
        }

        boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM || this == BOTTOM_RIGHT;
        }
    }

    /** A rectangle. Sizes never go negative. */
    public record Rect(int x, int y, int w, int h) {

        public Rect {
            w = Math.max(0, w);
            h = Math.max(0, h);
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public Rect offset(int dx, int dy) {
            return new Rect(x + dx, y + dy, w, h);
        }

        public Rect inset(int m) {
            return inset(m, m, m, m);
        }

        public Rect inset(int left, int top, int right, int bottom) {
            return new Rect(x + left, y + top, w - left - right, h - top - bottom);
        }

        /** The {@code n}-wide strip along the left edge. */
        public Rect left(int n) {
            return new Rect(x, y, Math.min(n, w), h);
        }

        /** What is left once the {@code n}-wide left strip is taken. */
        public Rect afterLeft(int n) {
            return new Rect(x + Math.min(n, w), y, w - n, h);
        }

        public Rect right(int n) {
            int m = Math.min(n, w);
            return new Rect(x + w - m, y, m, h);
        }

        public Rect beforeRight(int n) {
            return new Rect(x, y, w - n, h);
        }

        /** The {@code n}-tall strip along the top edge. */
        public Rect top(int n) {
            return new Rect(x, y, w, Math.min(n, h));
        }

        public Rect afterTop(int n) {
            return new Rect(x, y + Math.min(n, h), w, h - n);
        }

        public Rect bottom(int n) {
            int m = Math.min(n, h);
            return new Rect(x, y + h - m, w, m);
        }

        public Rect beforeBottom(int n) {
            return new Rect(x, y, w, h - n);
        }

        /** A {@code cw} x {@code ch} child anchored in this rectangle, pushed {@code dx}, {@code dy} inward from the edges it is anchored to. */
        public Rect at(Anchor a, int cw, int ch, int dx, int dy) {
            int cx = a.left() ? x + dx : a.right() ? x + w - cw - dx : x + (w - cw) / 2 + dx;
            int cy = a.top() ? y + dy : a.bottom() ? y + h - ch - dy : y + (h - ch) / 2 + dy;
            return new Rect(cx, cy, cw, ch);
        }

        public Rect at(Anchor a, int cw, int ch) {
            return at(a, cw, ch, 0, 0);
        }

        /** {@code n} equal columns {@code gap} apart (the last takes the remainder of the division). */
        public List<Rect> columns(int n, int gap) {
            List<Rect> out = new ArrayList<>(n);
            int each = n <= 0 ? 0 : (w - gap * (n - 1)) / n;
            for (int i = 0; i < n; i++)
                out.add(new Rect(x + i * (each + gap), y, i == n - 1 ? w - i * (each + gap) : each, h));
            return out;
        }

        /** {@code n} equal rows {@code gap} apart. */
        public List<Rect> rows(int n, int gap) {
            List<Rect> out = new ArrayList<>(n);
            int each = n <= 0 ? 0 : (h - gap * (n - 1)) / n;
            for (int i = 0; i < n; i++)
                out.add(new Rect(x, y + i * (each + gap), w, i == n - 1 ? h - i * (each + gap) : each));
            return out;
        }
    }

    /** Hands out children one after another along a row (left to right, or right to left) or down a column. */
    public static final class Flow {
        private final Rect area;
        private final int gap;
        private final boolean vertical, reverse;
        private int cursor;

        private Flow(Rect area, int gap, boolean vertical, boolean reverse) {
            this.area = area;
            this.gap = gap;
            this.vertical = vertical;
            this.reverse = reverse;
            this.cursor = vertical ? area.y : reverse ? area.right() : area.x;
        }

        public static Flow row(Rect area, int gap) {
            return new Flow(area, gap, false, false);
        }

        /** A row that starts at the right edge and runs leftward: the first child is the rightmost. */
        public static Flow rowFromRight(Rect area, int gap) {
            return new Flow(area, gap, false, true);
        }

        public static Flow column(Rect area, int gap) {
            return new Flow(area, gap, true, false);
        }

        /** The next child, {@code w} x {@code h}, centred across the flow's other axis. */
        public Rect next(int w, int h) {
            Rect r;
            if (vertical) {
                r = new Rect(area.x, cursor, w, h);
                cursor += h + gap;
            } else if (reverse) {
                r = new Rect(cursor - w, area.y + (area.h - h) / 2, w, h);
                cursor -= w + gap;
            } else {
                r = new Rect(cursor, area.y + (area.h - h) / 2, w, h);
                cursor += w + gap;
            }
            return r;
        }

        /** What is left along the flow's axis after the children handed out so far. */
        public Rect remaining() {
            if (vertical)
                return new Rect(area.x, cursor, area.w, area.bottom() - cursor);
            return reverse ? new Rect(area.x, area.y, cursor - area.x, area.h) : new Rect(cursor, area.y, area.right() - cursor, area.h);
        }
    }
}
