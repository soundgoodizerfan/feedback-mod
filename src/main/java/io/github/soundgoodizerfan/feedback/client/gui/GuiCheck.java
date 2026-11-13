// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;

/**
 * A screen check for {@link GuiShot}: re-renders the screen that is open through a graphics object that
 * records every string drawn (its box, after the pose's translate and scale), reports each pair of strings whose boxes overlap, and
 * lists each string with where it sits relative to the screen's top-left. It needs the game running with the screen open; it is not headless.
 */
public final class GuiCheck {

    private GuiCheck() {
    }

    /** One string as drawn: its text and its box in screen pixels. */
    record Box(String text, float x0, float y0, float x1, float y1) {
        boolean overlaps(Box o) {
            return Math.min(x1, o.x1) - Math.max(x0, o.x0) >= 2f && Math.min(y1, o.y1) - Math.max(y0, o.y0) >= 3f;
        }
    }

    /** A graphics object that notes every string it is asked to draw, then draws it. */
    static final class Recording extends GuiGraphics {
        final List<Box> boxes = new ArrayList<>();

        Recording(Minecraft mc) {
            super(mc, mc.renderBuffers().bufferSource());
        }

        private void note(Font font, String text, float x, float y) {
            if (text == null || text.isBlank())
                return;
            var m = pose().last().pose();
            org.joml.Vector3f a = m.transformPosition(x, y, 0, new org.joml.Vector3f());
            org.joml.Vector3f b = m.transformPosition(x + font.width(text), y + 8, 0, new org.joml.Vector3f());
            boxes.add(new Box(text, Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y)));
        }

        @Override
        public int drawString(Font font, String text, float x, float y, int color, boolean shadow) {
            note(font, text, x, y);
            return super.drawString(font, text, x, y, color, shadow);
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow) {
            StringBuilder b = new StringBuilder();
            text.accept((i, style, cp) -> {
                b.appendCodePoint(cp);
                return true;
            });
            note(font, b.toString(), x, y);
            return super.drawString(font, text, x, y, color, shadow);
        }
    }

    /** The pairs of strings whose boxes overlap. */
    static List<String> overlaps(List<Box> boxes, float originX, float originY) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++)
            for (int j = i + 1; j < boxes.size(); j++) {
                Box a = boxes.get(i), b = boxes.get(j);
                if (!a.text().equals(b.text()) && a.overlaps(b))
                    out.add(String.format(java.util.Locale.ROOT, "\"%s\" at (%.0f,%.0f) overlaps \"%s\" at (%.0f,%.0f)",
                            a.text(), a.x0() - originX, a.y0() - originY, b.text(), b.x0() - originX, b.y0() - originY));
            }
        return out;
    }

    /** Re-renders {@code screen} once through a recording graphics and returns the report lines: a summary, then one line per overlap and per string. */
    static List<String> report(Minecraft mc, Screen screen, int mx, int my) {
        Recording g = new Recording(mc);
        screen.render(g, mx, my, 0f);
        // A string parked off the window (a screen hides a label that way) is not on screen, so it overlaps nothing.
        float w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        g.boxes.removeIf(b -> b.x1() < 0 || b.y1() < 0 || b.x0() > w || b.y0() > h);
        List<String> found = overlaps(g.boxes, 0, 0);
        List<String> out = new ArrayList<>();
        out.add(screen.getClass().getSimpleName() + ": " + g.boxes.size() + " strings, " + found.size() + " overlaps");
        for (String line : found)
            out.add("overlap: " + line);
        for (Box b : g.boxes)
            out.add(String.format(java.util.Locale.ROOT, "string: \"%s\" (%.0f,%.0f)-(%.0f,%.0f)", b.text(), b.x0(), b.y0(), b.x1(), b.y1()));
        return out;
    }
}
