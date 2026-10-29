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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The widgets and drawing helpers every skinned screen shares: buttons, toggle switches, check
 * boxes, steppers, sliders and tab strips drawn by a {@link GuiSkin}, easing, traced outlines, the drafting board, the stamp, flashing readouts
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
     * The motion clock in ticks: game time plus partial tick, so motion holds still while paused;
     * wall clock (20/s) with no world. A screen stores this when an event happens and passes it back.
     */
    public static float time() {
        var mc = Minecraft.getInstance();
        if (mc.level == null)
            return net.minecraft.Util.getMillis() / 50f;
        return mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    /** Ticks a shake lasts. */
    private static final float SHAKE_TICKS = 8;

    /**
     * Sideways pixels to draw a rejecting slot or control at, {@code start} being {@link #time()} when it
     * was rejected: a few pixels either way, dying out over 8 ticks. Always 0 under reduce motion or when
     * {@code start} is not a real time (pass {@link Float#NEGATIVE_INFINITY} for never).
     */
    public static int shake(float start, int amplitude) {
        float age = time() - start;
        if (age < 0 || age >= SHAKE_TICKS || ClientSettings.reduceMotion())
            return 0;
        return Math.round(Mth.sin(age * 2.6f) * amplitude * (1 - age / SHAKE_TICKS));
    }

    /**
     * Draws the cover of a stage change over the rectangle (x, y, w, h), in the skin's own plates, and
     * returns progress 0..1. The cover closes over the first half and opens over the second, so a screen
     * shows the old stage while the result is under 0.5 and the new one after. {@code start} is {@link
     * #time()} at the event, {@code move} what {@link GuiSkin#cue} returned. Returns 1 and draws nothing
     * when idle, {@link GuiSkin.Move#NONE}, or under reduce motion. Draw it after the content, before
     * widgets that should stay above. Allocation free.
     */
    public static float transition(GuiGraphics g, GuiSkin skin, GuiSkin.Move move, float start,
                                   int x, int y, int w, int h) {
        float p = (time() - start) / move.ticks();
        if (p < 0 || p >= 1 || move.style() == GuiSkin.Style.NONE || ClientSettings.reduceMotion())
            return 1;
        float cover = easeOut(p < 0.5f ? p * 2 : (1 - p) * 2);
        int cw = Math.round(w * cover), ch = Math.round(h * cover);
        switch (move.style()) {
            case SHUTTERS -> { // two plates close from the top and bottom edges
                int hh = ch / 2;
                skin.plaque(g, x, y, w, hh);
                skin.plaque(g, x, y + h - hh, w, hh);
            }
            case FOLD -> { // two leaves fold in from the left and right
                int hw = cw / 2;
                skin.plaque(g, x, y, hw, h);
                skin.plaque(g, x + w - hw, y, hw, h);
            }
            case SLIDE, GRIND -> { // a board or slab pushed across from the left, edge lit
                skin.plaque(g, x, y, cw, h);
                g.fill(x + cw, y, x + cw + 1, y + h, 0x60000000);
            }
            case UNROLL -> { // the sheet unrolls from the left, the roll riding its edge
                skin.plaque(g, x, y, cw, h);
                if (cw > 0)
                    g.fill(Math.max(x, x + cw - 3), y, x + cw, y + h, skin.accent());
            }
            case DROP -> { // a plate drops from the top
                skin.plaque(g, x, y, w, ch);
                g.fill(x, y + ch, x + w, y + ch + 1, 0x60000000);
            }
            case SWEEP -> { // a translucent band sweeps across (no cover; the swap happens under it)
                int bw = Math.max(8, w / 5), bx = x + Math.round((w + bw) * p) - bw;
                int x0 = Math.max(x, bx), x1 = Math.min(x + w, bx + bw);
                if (x1 > x0)
                    g.fill(x0, y, x1, y + h, 0x70000000 | skin.accent() & 0xFFFFFF);
            }
            default -> { }
        }
        return p;
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

    /** A filled disc centred on (cx, cy), drawn a scanline at a time. */
    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** A ring of radius {@code r} and thickness {@code t}: a disc with the middle taken out. */
    public static void ring(GuiGraphics g, int cx, int cy, int r, int t, int color) {
        int in = r - t;
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            if (Math.abs(dy) >= in) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
                continue;
            }
            int inner = (int) Math.round(Math.sqrt(in * in - dy * dy + 0.25));
            g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
            g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
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

    /** A HUD card: vanilla's inventory grey with its bevel ({@link GuiSkin#VANILLA}'s look), a little translucent. */
    public static void hudCard(GuiGraphics g, int x0, int y0, int x1, int y1) {
        hudCard(g, x0, y0, x1, y1, 1f);
    }

    /** {@link #hudCard(GuiGraphics, int, int, int, int)} faded to {@code alpha} 0..1, for a card that comes and goes. */
    public static void hudCard(GuiGraphics g, int x0, int y0, int x1, int y1, float alpha) {
        int a = (int) (0xC4 * alpha) << 24; // a little see-through: the world shows behind the card
        g.fill(x0, y0, x1, y1, a);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, a | 0xC6C6C6);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, a | 0xFFFFFF);
        g.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 1, a | 0xFFFFFF);
        g.fill(x0 + 3, y1 - 3, x1 - 1, y1 - 1, a | 0x555555);
        g.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 1, a | 0x555555);
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


    /**
     * A value between two arrow buttons: {@code left} and {@code right} are the arrows' labels, {@code value}
     * the reading (flashing the accent when it changes), {@code step} told -1 or +1. With {@code plaque}
     * the reading sits on a stamped plate. Sounds are the skin's click unless {@code sound} gives one,
     * pitched down for the left arrow and up for the right, like a detent.
     */
    public static AbstractWidget stepper(GuiSkin skin, int x, int y, int w, int h, String left, String right,
                                         Supplier<String> value, IntConsumer step, boolean plaque, Supplier<SoundEvent> sound) {
        return new Stepper(skin, x, y, w, h, Component.literal(left), Component.literal(right), value, step, plaque, sound);
    }

    /** A check box with its label to the right. {@code state} is read every frame. */
    public static Button checkbox(GuiSkin skin, Component label, BooleanSupplier state, Button.OnPress press,
                                  int x, int y, int w) {
        return new Checkbox(skin, label, state, press, x, y, w);
    }

    /**
     * A slider over {@code min}..{@code max} that snaps to {@code step}. {@code value} is read every
     * frame; {@code set} is told each time the snapped value changes while dragging, and by Left/Right
     * (ten steps with Shift) when focused. Twelve high.
     */
    public static AbstractWidget slider(GuiSkin skin, int x, int y, int w, double min, double max, double step,
                                        DoubleSupplier value, DoubleConsumer set) {
        return new Slider(skin, x, y, w, min, max, step, value, set);
    }

    /** A strip of tabs sharing the width, {@code selected} read every frame; clicking another calls {@code select}. */
    public static AbstractWidget tabs(GuiSkin skin, int x, int y, int w, int h, List<Component> labels,
                                      IntSupplier selected, IntConsumer select) {
        return new Tabs(skin, x, y, w, h, labels, selected, select);
    }

    /** Eases a 0..1 hover glow toward {@code on}, {@code dt} seconds on. */
    private static float ease(float glow, boolean on, float dt) {
        return glow + Mth.clamp((on ? 1f : 0f) - glow, -dt * 8f, dt * 8f);
    }

    private static final class Stepper extends AbstractWidget {
        private static final int ARROW = 14;
        private final GuiSkin skin;
        private final Component left, right;
        private final Supplier<String> value;
        private final IntConsumer step;
        private final boolean plaque;
        private final Supplier<SoundEvent> sound;
        private final Flash flash = new Flash(1);
        private float glowLeft, glowRight;
        private long last;

        Stepper(GuiSkin skin, int x, int y, int w, int h, Component left, Component right, Supplier<String> value,
                IntConsumer step, boolean plaque, Supplier<SoundEvent> sound) {
            super(x, y, w, h, Component.empty());
            this.skin = skin;
            this.left = left;
            this.right = right;
            this.value = value;
            this.step = step;
            this.plaque = plaque;
            this.sound = sound;
        }

        private int side(double mouseX) {
            return mouseX < getX() + ARROW ? -1 : mouseX >= getX() + width - ARROW ? 1 : 0;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            int over = active && isHovered() ? side(mouseX) : 0;
            glowLeft = ease(glowLeft, over < 0, dt);
            glowRight = ease(glowRight, over > 0, dt);
            boolean pressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
            skin.button(g, getX(), getY(), ARROW, height, left, glowLeft, pressed && over < 0, active, false);
            skin.button(g, getX() + width - ARROW, getY(), ARROW, height, right, glowRight, pressed && over > 0, active, false);
            if (plaque)
                skin.plaque(g, getX() + ARROW + 1, getY(), width - 2 * ARROW - 2, height);
            String v = value.get();
            var font = Minecraft.getInstance().font;
            int room = width - 2 * ARROW - 4;
            String shown = font.width(v) <= room ? v : font.plainSubstrByWidth(v, room - font.width("...")) + "...";
            g.drawCenteredString(font, shown, getX() + width / 2, getY() + (height - 8) / 2 + 1,
                    flash.colour(0, v, skin.ink(), skin.accent()));
        }

        private void press(int dir) {
            GuiSkin.play(sound != null ? sound.get() : skin.click(), dir < 0 ? 0.9f : 1.1f);
            step.accept(dir);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int dir = side(mouseX);
            if (dir != 0)
                press(dir);
        }

        @Override
        public void playDownSound(SoundManager manager) {
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (active && isFocused() && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
                press(key == InputConstants.KEY_LEFT ? -1 : 1);
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.literal(value.get()));
        }
    }

    private static final class Checkbox extends Button {
        private final GuiSkin skin;
        private final BooleanSupplier state;
        private float glow;
        private long last;

        Checkbox(GuiSkin skin, Component label, BooleanSupplier state, OnPress press, int x, int y, int w) {
            super(x, y, w, 12, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            glow = ease(glow, active && isHoveredOrFocused(), dt);
            boolean on = state.getAsBoolean();
            skin.checkbox(g, getX(), getY() + 1, on, glow);
            int text = !active ? GuiSkin.lerp(skin.dim(), 0xFF000000, 0.35f) : GuiSkin.lerp(skin.dim(), skin.ink(), Math.max(glow, on ? 1 : 0));
            g.drawString(Minecraft.getInstance().font, getMessage(), getX() + 15, getY() + 2, text, false);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(skin.click(), state.getAsBoolean() ? 0.9f : 1.1f);
        }
    }

    private static final class Slider extends AbstractWidget {
        private final GuiSkin skin;
        private final double min, max, step;
        private final DoubleSupplier value;
        private final DoubleConsumer set;
        private float glow;
        private long last;
        private boolean dragging;

        Slider(GuiSkin skin, int x, int y, int w, double min, double max, double step, DoubleSupplier value, DoubleConsumer set) {
            super(x, y, w, 12, Component.empty());
            this.skin = skin;
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = value;
            this.set = set;
        }

        private float frac() {
            return (float) Mth.clamp((value.getAsDouble() - min) / (max - min), 0, 1);
        }

        private void put(double v) {
            v = Mth.clamp(Math.round((v - min) / step) * step + min, min, max);
            if (v == value.getAsDouble())
                return;
            GuiSkin.play(skin.click(), 0.8f + 0.4f * (float) ((v - min) / (max - min)));
            set.accept(v);
        }

        private void drag(double mouseX) {
            put(min + (max - min) * Mth.clamp((mouseX - getX() - 3) / (width - 6), 0, 1));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            glow = ease(glow, active && (dragging || isHoveredOrFocused()), dt);
            skin.slider(g, getX(), getY(), width, frac(), glow, active);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            dragging = true;
            drag(mouseX);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            drag(mouseX);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            dragging = false;
        }

        @Override
        public void playDownSound(SoundManager manager) {
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (active && isFocused() && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
                double s = step * (net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1);
                put(value.getAsDouble() + (key == InputConstants.KEY_LEFT ? -s : s));
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.literal(String.valueOf(value.getAsDouble())));
        }
    }

    private static final class Tabs extends AbstractWidget {
        private final GuiSkin skin;
        private final List<Component> labels;
        private final IntSupplier selected;
        private final IntConsumer select;
        private final float[] glow;
        private long last;

        Tabs(GuiSkin skin, int x, int y, int w, int h, List<Component> labels, IntSupplier selected, IntConsumer select) {
            super(x, y, w, h, Component.empty());
            this.skin = skin;
            this.labels = labels;
            this.selected = selected;
            this.select = select;
            this.glow = new float[labels.size()];
        }

        private int at(double mouseX) {
            return Mth.clamp((int) ((mouseX - getX()) * labels.size() / width), 0, labels.size() - 1);
        }

        /** Tab {@code i}'s left edge and width; the strip's width is shared out whole. */
        private int edge(int i) {
            return getX() + width * i / labels.size();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            int over = active && isHovered() ? at(mouseX) : -1, picked = selected.getAsInt();
            for (int i = 0; i < labels.size(); i++) {
                glow[i] = ease(glow[i], i == over, dt);
                skin.tab(g, edge(i), getY(), edge(i + 1) - edge(i), height, labels.get(i), i == picked, active, glow[i]);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int i = at(mouseX);
            if (i != selected.getAsInt())
                select.accept(i);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(skin.click(), 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, labels.get(Mth.clamp(selected.getAsInt(), 0, labels.size() - 1)));
        }
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
                g.fill(lx - 1, ly - 1, lx + 9, ly + 9, 0x40 << 24 | skin.accent() & 0xFFFFFF);
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
