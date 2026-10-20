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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * The look every Feedback screen shares: a beveled steel panel, inset sockets, recessed wells and
 * one button. Drawn with fills, not textures, so there is nothing to license and every screen
 * moves together when this file changes.
 */
public final class GuiStyle {

    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, EDGE = 0xFF1A1A1A,
            SOCKET = 0xFF8B8B8B, WELL = 0xFF202020, TEXT = 0xFF404040;

    private GuiStyle() {}

    /** Raised panel: dark outline, light top-left bevel, shaded bottom-right. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, PANEL); // corners: bevels meet cleanly
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, PANEL);
    }

    /** Sunken 18x18 slot socket around the 16x16 item at (x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        well(g, x - 1, y - 1, 18, 18, SOCKET);
    }

    /** A recessed rectangle: dark top-left, light bottom-right, as if cut into the panel. */
    public static void well(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, SHADE);
        g.fill(x, y, x + 1, y + h - 1, SHADE);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    public static void hotbar(GuiGraphics g, int x, int y) {
        for (int c = 0; c < 9; c++) slot(g, x + c * 18, y);
    }

    /** A flat button that brightens under the pointer and sinks when pressed. */
    public static Button button(Component label, Button.OnPress press, int x, int y, int w, int h) {
        return button(GuiSkin.STEEL, label, press, x, y, w, h);
    }

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
