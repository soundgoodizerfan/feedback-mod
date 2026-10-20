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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * The widgets and drawing helpers every skinned screen shares: buttons and toggle switches drawn
 * by a {@link GuiSkin}, easing, traced outlines, the drafting board, the stamp, flashing readouts
 * and the HUD card. Drawn with fills, not textures, so there is nothing to license.
 */
public final class GuiStyle {

    private GuiStyle() {}

    public static Button button(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h) {
        return new FButton(skin, false, null, 1f, x, y, w, h, label, press);
    }

    /** A button with its own sound, at a pitch: a stepper's detent rises one way and falls the other. */
    public static Button button(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h,
                                Supplier<SoundEvent> sound, float pitch) {
        return new FButton(skin, false, sound, pitch, x, y, w, h, label, press);
    }

    /** The one button on a screen that commits: drawn heavier, by the skin. */
    public static Button primary(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h,
                                 Supplier<SoundEvent> sound) {
        return new FButton(skin, true, sound, 1f, x, y, w, h, label, press);
    }

    /** Fast out, slow in: for anything arriving. */
    public static float easeOut(float t) {
        float u = 1 - Mth.clamp(t, 0, 1);
        return 1 - u * u * u;
    }

    /**
     * Part of a rectangle's outline: {@code len} pixels clockwise from {@code from} pixels past its
     * top-left corner. A whole outline drawn by a growing {@code len} is one being traced; short
     * runs stepped along it are a marching dashed line.
     */
    public static void perimeter(GuiGraphics g, int x0, int y0, int x1, int y1, int from, int len, int color) {
        int w = x1 - x0 - 1, h = y1 - y0 - 1, p = 2 * (w + h);
        if (p <= 0 || len <= 0)
            return;
        from = Math.floorMod(from, p);
        while (len > 0) {
            int run;
            if (from < w) { // top, rightward
                run = Math.min(len, w - from);
                g.fill(x0 + from, y0, x0 + from + run, y0 + 1, color);
            } else if (from < w + h) { // right, downward
                int d = from - w;
                run = Math.min(len, h - d);
                g.fill(x1 - 1, y0 + d, x1, y0 + d + run, color);
            } else if (from < 2 * w + h) { // bottom, leftward
                int d = from - w - h;
                run = Math.min(len, w - d);
                g.fill(x1 - 1 - d - run + 1, y1 - 1, x1 - d, y1, color);
            } else { // left, upward
                int d = from - 2 * w - h;
                run = Math.min(len, h - d);
                g.fill(x0, y1 - 1 - d - run + 1, x0 + 1, y1 - d, color);
            }
            len -= run;
            from = (from + run) % p;
        }
    }

    /** A straight line of any slope, one pixel at a time. */
    public static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1)
                return;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * A drawing on a {@link GuiSkin#BLUEPRINT} sheet: framed, rulers along the top and left, and
     * while {@code live} a T-square and set-square following the pointer, marked on the rulers.
     */
    public static void drawingBoard(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, boolean live) {
        int rule = 0x60E8F1FF, mark = GuiSkin.BLUEPRINT.accent();
        g.renderOutline(x, y, w, h, 0x70E8F1FF);
        for (int i = 6; i < w; i += 6)
            g.fill(x + i, y + 1, x + i + 1, y + 1 + (i % 24 == 0 ? 4 : 2), rule);
        for (int i = 6; i < h; i += 6)
            g.fill(x + 1, y + i, x + 1 + (i % 24 == 0 ? 4 : 2), y + i + 1, rule);
        if (live && mouseX > x && mouseX < x + w - 1 && mouseY > y && mouseY < y + h - 1) {
            g.fill(x + 1, mouseY, x + w - 1, mouseY + 1, 0x22FFFFFF);
            g.fill(mouseX, y + 1, mouseX + 1, y + h - 1, 0x22FFFFFF);
            g.fill(mouseX, y + 1, mouseX + 1, y + 7, mark);
            g.fill(x + 1, mouseY, x + 7, mouseY + 1, mark);
        }
    }

    /** A rubber stamp {@code age} ms after it came down at (cx, cy): lands big, settles, then fades. */
    public static void stamp(GuiGraphics g, Component text, float cx, float cy, long age, int rgb) {
        if (age < 0 || age > 1600)
            return;
        float scale = 1 + 0.8f * (1 - easeOut(Math.min(1, age / 110f)));
        int alpha = (int) (255 * Mth.clamp(1 - (age - 1200) / 400f, 0, 1));
        if (alpha < 8)
            return;
        var font = Minecraft.getInstance().font;
        int w = font.width(text) + 10, h = 15, colour = alpha << 24 | rgb & 0xFFFFFF;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 300);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
        pose.scale(scale, scale, 1);
        g.renderOutline(-w / 2, -h / 2, w, h, colour);
        g.renderOutline(-w / 2 + 2, -h / 2 + 2, w - 4, h - 4, colour);
        g.drawString(font, text, -font.width(text) / 2, -3, colour, false);
        pose.popPose();
    }

    /** A HUD card: the instrument panel's plate, translucent, with an amber lamp-strip down its left edge. */
    public static void hudCard(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, 0xC0181C1A);
        g.fill(x0, y0, x1, y0 + 1, 0x60D8DCD8);
        g.fill(x0, y0 + 1, x0 + 2, y1, 0xA0000000 | GuiSkin.PANEL.accent() & 0xFFFFFF);
    }

    /** A HUD sliding in from the right whenever what it shows for changes (a tool coming into hand). */
    public static final class Slide {
        private Object showing;
        private long since;

        /** How far right of its place to draw now, for {@code key} the thing it is shown for. */
        public int offset(Object key, int distance) {
            long now = net.minecraft.Util.getMillis();
            if (key != showing) {
                showing = key;
                since = now;
            }
            return Math.round((1 - easeOut((now - since) / 220f)) * distance);
        }
    }

    /** Readouts that flash the accent for a moment when their value changes. */
    public static final class Flash {
        private static final float MS = 450;
        private final String[] shown;
        private final long[] at;

        public Flash(int size) {
            shown = new String[size];
            at = new long[size];
        }

        /** The colour readout {@code i} draws {@code value} in now. */
        public int colour(int i, String value, int ink, int accent) {
            long now = net.minecraft.Util.getMillis();
            if (shown[i] != null && !shown[i].equals(value))
                at[i] = now;
            shown[i] = value;
            float f = Math.max(0, 1 - (now - at[i]) / MS);
            return f > 0 ? GuiSkin.lerp(ink, accent, f) : ink;
        }

        /** Forget what was shown, so the next values do not flash: the readouts now mean something else. */
        public void reset() {
            java.util.Arrays.fill(shown, null);
        }
    }

    /**
     * An instrument panel's toggle switch: a lamp, an engraved label, and a lever thrown up for on.
     * {@code state} is read every frame, so the switch shows what is set, not what was clicked.
     */
    public static Button toggle(GuiSkin skin, Component label, java.util.function.BooleanSupplier state, Button.OnPress press,
                                int x, int y, int w) {
        return new Toggle(skin, label, state, press, x, y, w);
    }

    private static final class Toggle extends Button {
        private final GuiSkin skin;
        private final java.util.function.BooleanSupplier state;
        private float lever = -1, glow;
        private long last;

        Toggle(GuiSkin skin, Component label, java.util.function.BooleanSupplier state, OnPress press, int x, int y, int w) {
            super(x, y, w, 16, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            boolean on = state.getAsBoolean();
            lever = lever < 0 ? (on ? 1 : 0) : lever + Mth.clamp((on ? 1 : 0) - lever, -dt * 14, dt * 14);
            glow += Mth.clamp((isHoveredOrFocused() ? 1f : 0f) - glow, -dt * 8, dt * 8);
            int x = getX(), y = getY(), w = getWidth();
            if (glow > 0)
                g.fill(x, y, x + w, y + 16, (int) (glow * 0x14) << 24 | 0xFFFFFF);
            // The lamp: dark glass, or lit with a halo.
            int lx = x + 3, ly = y + 4;
            if (on)
                g.fill(lx - 1, ly - 1, lx + 9, ly + 9, 0x40FFB347);
            g.fill(lx, ly, lx + 8, ly + 8, 0xFF0A0C0B);
            g.fill(lx + 1, ly + 1, lx + 7, ly + 7, on ? skin.accent() : 0xFF3A3020);
            g.fill(lx + 2, ly + 2, lx + 4, ly + 3, on ? 0xFFFFF0D0 : 0x30FFFFFF);
            var font = Minecraft.getInstance().font;
            g.drawString(font, getMessage(), x + 16, y + 4, lerp(skin.dim(), skin.ink(), Math.max(glow, on ? 1 : 0)), false);
            // The switch: a chrome nut, and the lever thrown from it.
            int nx = x + w - 12, ny = y + 5;
            g.fill(nx, ny, nx + 7, ny + 7, 0xFF8A908C);
            g.fill(nx + 1, ny + 1, nx + 6, ny + 6, 0xFF5A605C);
            int tip = Math.round(Mth.lerp(lever, 7, -2)), from = 3;
            int y0 = Math.min(ny + from, ny + tip), y1 = Math.max(ny + from, ny + tip) + 1;
            g.fill(nx + 2, y0, nx + 5, y1, 0xFFD8DCD8);
            g.fill(nx + 2, ny + tip, nx + 5, ny + tip + 2, 0xFFFFFFFF);
        }

        private static int lerp(int a, int b, float t) {
            return GuiSkin.lerp(a, b, t);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(FSounds.UI_TOGGLE.get(), state.getAsBoolean() ? 0.9f : 1.1f);
        }
    }

    private static final class FButton extends Button {
        private final GuiSkin skin;
        private final boolean primary;
        private final Supplier<SoundEvent> sound;
        private final float pitch;
        private float glow;
        private long last;

        FButton(GuiSkin skin, boolean primary, Supplier<SoundEvent> sound, float pitch, int x, int y, int w, int h,
                Component label, OnPress press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.primary = primary;
            this.sound = sound;
            this.pitch = pitch;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            float target = !active ? 0f : isHoveredOrFocused() ? 1f : 0f;
            glow += Mth.clamp(target - glow, -dt * 8f, dt * 8f);
            boolean down = active && isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
            skin.button(g, getX(), getY(), getWidth(), getHeight(), getMessage(), glow, down, active, primary);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(sound != null ? sound.get() : skin.click(), pitch);
        }
    }
}
