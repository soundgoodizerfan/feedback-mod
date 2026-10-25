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

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.combat.cast.CastState;
import io.github.soundgoodizerfan.feedback.combat.cast.Casts;
import io.github.soundgoodizerfan.feedback.combat.skill.TreeHue;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The cast radial, opened by holding {@link CombatKeys#RADIAL}: the five cast slots round a ring. It only SELECTS (the
 * choice is client-side state, {@link #selected()}); casting is a tap of the radial key, which sends the ordinary
 * {@code Fire} the server checks. Letting the key go, or a click, takes the slot the pointer is over.
 */
final class CastRadialScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int R_OUT = 88, R_IN = 36, GAP = 3, RIM = 2, LABEL_R = (R_OUT + R_IN) / 2;

    /**
     * The ring cut once into horizontal runs, each tagged {@code slot * 2 + rim}, offsets from the centre. The gaps between
     * wedges are left out, so drawing is a fill per run (a few hundred) and never a per-pixel test.
     */
    private static final int[] RUN_Y, RUN_X0, RUN_X1, RUN_CODE;
    /** Runs are sorted by slot, so slot {@code i} owns {@code SLOT_FROM[i]} up to {@code SLOT_FROM[i + 1]}. */
    private static final int[] SLOT_FROM = new int[CastState.SLOTS + 1];

    static {
        var y = new java.util.ArrayList<int[]>();
        for (int dy = -R_OUT; dy <= R_OUT; dy++) {
            int prev = -1, start = 0;
            for (int dx = -R_OUT; dx <= R_OUT + 1; dx++) {
                int code = dx > R_OUT ? -1 : code(dx, dy);
                if (code != prev) {
                    if (prev >= 0)
                        y.add(new int[] { dy, start, dx, prev });
                    start = dx;
                    prev = code;
                }
            }
        }
        y.sort(java.util.Comparator.comparingInt(r -> r[3] >> 1));
        RUN_Y = y.stream().mapToInt(r -> r[0]).toArray();
        RUN_X0 = y.stream().mapToInt(r -> r[1]).toArray();
        RUN_X1 = y.stream().mapToInt(r -> r[2]).toArray();
        RUN_CODE = y.stream().mapToInt(r -> r[3]).toArray();
        for (int r = RUN_CODE.length - 1; r >= 0; r--)
            SLOT_FROM[(RUN_CODE[r] >> 1) + 1] = Math.max(SLOT_FROM[(RUN_CODE[r] >> 1) + 1], r + 1);
    }

    /** {@code slot * 2 + rim} for the pixel at this offset from the centre, or -1 outside the ring or in a gap. */
    private static int code(int dx, int dy) {
        double px = dx + 0.5, py = dy + 0.5, r = Math.sqrt(px * px + py * py);
        if (r > R_OUT || r < R_IN)
            return -1;
        int slot = nearest(Math.atan2(py, px));
        double off = Math.abs(wrap(Math.atan2(py, px) - angle(slot)));
        if (r * Math.sin(Math.PI / CastState.SLOTS - off) < GAP / 2.0)
            return -1;
        return slot * 2 + (r > R_OUT - RIM || r < R_IN + RIM ? 1 : 0);
    }

    private static double wrap(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    private static int nearest(double at) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < CastState.SLOTS; i++) {
            double d = Math.abs(wrap(at - angle(i)));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private static int selected = 0;

    private static final long OPEN_MS = 180;
    private final long openedAt = Util.getMillis();
    private final float[] pop = new float[CastState.SLOTS];
    private long lastMs = openedAt;
    private int lastHover = -1;

    /** The crosshair has no business under the ring. */
    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    static final class HideCrosshair {
        @SubscribeEvent
        static void onLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && net.minecraft.client.Minecraft.getInstance().screen instanceof CastRadialScreen)
                event.setCanceled(true);
        }
    }

    private static int fade(int argb, float a) {
        return (int) ((argb >>> 24) * a) << 24 | argb & 0xFFFFFF;
    }

    /** Each slot's highlight: the hue of the tree its technique is unlocked in, else the skin's accent. */
    private final int[] hue = new int[CastState.SLOTS];

    CastRadialScreen() {
        super(Component.translatable("key.feedback.cast_radial"));
        var mc = net.minecraft.client.Minecraft.getInstance();
        var slots = CastState.of(mc.player).slots();
        for (int i = 0; i < hue.length; i++) {
            hue[i] = TreeHue.ofTechnique(i < slots.size() ? slots.get(i) : "", SKIN.accent()) | 0xFF000000;
        }
    }

    /** The slot a tap of the radial key casts: the chosen one if it still holds a technique, else the first that does, else -1. */
    static int selected() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null)
            return -1;
        var slots = CastState.of(mc.player).slots();
        if (selected < slots.size() && !slots.get(selected).isEmpty())
            return selected;
        for (int i = 0; i < slots.size(); i++)
            if (!slots.get(i).isEmpty())
                return i;
        return -1;
    }

    private static double angle(int i) {
        return -Math.PI / 2 + i * 2 * Math.PI / CastState.SLOTS;
    }

    /** The slot whose direction the pointer is nearest, or -1 while it is still near the middle. */
    private int hovered(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (dx * dx + dy * dy < R_IN * R_IN)
            return -1;
        return nearest(Math.atan2(dy, dx));
    }

    private void choose(double mx, double my) {
        int h = hovered(mx, my);
        if (h >= 0 && !CastState.of(minecraft.player).slots().get(h).isEmpty())
            selected = h;
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // The world stays in view: no blur, no dimming.
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        var slots = CastState.of(minecraft.player).slots();
        long now = Util.getMillis();
        float t = GuiStyle.easeOut(Math.min(1f, (now - openedAt) / (float) OPEN_MS)), dt = Math.min(100, now - lastMs);
        lastMs = now;
        int hov = hovered(mx, my), sel = selected(), n = slots.size();
        boolean hovHas = hov >= 0 && Casts.find(slots.get(hov)) != null;
        if (hovHas && hov != lastHover)
            GuiSkin.play(SKIN.click(), 1.5f, 0.5f);
        lastHover = hovHas ? hov : -1;
        float pulse = 0.75f + 0.25f * (float) Math.sin(now / 180.0);
        int ink = SKIN.ink() | 0xFF000000, dim = SKIN.dim() | 0xFF000000;

        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        float s = 0.75f + 0.25f * t;
        g.pose().scale(s, s, 1);
        for (int i = 0; i < n; i++) {
            boolean has = Casts.find(slots.get(i)) != null;
            int accent = hue[i];
            pop[i] += ((i == lastHover ? 1f : 0f) - pop[i]) * Math.min(1f, dt / 70f);
            int face = i == sel ? 0xC0 << 24 | GuiSkin.lerp(0xFF141414, accent, 0.35f + 0.1f * pulse) & 0xFFFFFF
                    : has ? GuiSkin.lerp(0xB0181C20, 0xC0384048, pop[i]) : 0x80101214;
            int rim = i == sel ? fade(accent, pulse) : has ? GuiSkin.lerp(0x80 << 24 | dim & 0xFFFFFF, accent, pop[i]) : 0x40 << 24 | dim & 0xFFFFFF;
            float push = pop[i] * 5f, dx = (float) Math.cos(angle(i)) * push, dy = (float) Math.sin(angle(i)) * push;
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            for (int r = SLOT_FROM[i]; r < SLOT_FROM[i + 1]; r++)
                g.fill(RUN_X0[r], RUN_Y[r], RUN_X1[r], RUN_Y[r] + 1, fade((RUN_CODE[r] & 1) == 0 ? face : rim, t));
            String tech = slots.get(i);
            var cast = Casts.find(tech);
            Component name = cast == null ? Component.translatable("gui.feedback.cast_empty") : label(tech);
            int lx = (int) Math.round(Math.cos(angle(i)) * LABEL_R), ly = (int) Math.round(Math.sin(angle(i)) * LABEL_R);
            var lines = font.split(name, 52);
            for (int l = 0; l < Math.min(2, lines.size()); l++)
                g.drawCenteredString(font, lines.get(l), lx, ly - Math.min(2, lines.size()) * 5 + l * 10, fade(cast == null ? dim : i == sel ? accent : ink, t));
            g.pose().popPose();
        }
        // The middle names what a tap would cast, or what the pointer is over.
        int shown = hovHas ? hov : sel;
        if (shown >= 0) {
            var lines = font.split(label(slots.get(shown)), 2 * R_IN - 14);
            for (int l = 0; l < Math.min(3, lines.size()); l++)
                g.drawCenteredString(font, lines.get(l), 0, -Math.min(3, lines.size()) * 5 + l * 10, fade(shown == sel ? hue[shown] : ink, t));
        }
        g.pose().popPose();
    }

    private static Component label(String tech) {
        return Component.translatableWithFallback("skill.feedback.move." + tech, tech);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        choose(mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (CombatKeys.RADIAL.matchesMouse(button)) {
            choose(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (CombatKeys.RADIAL.matches(key, scan)) {
            var mouse = minecraft.mouseHandler;
            choose(mouse.xpos() * width / minecraft.getWindow().getScreenWidth(), mouse.ypos() * height / minecraft.getWindow().getScreenHeight());
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void tick() {
        // A release that landed before the screen existed would never reach keyReleased.
        var k = CombatKeys.RADIAL.getKey();
        if (k.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), k.getValue()))
            onClose();
    }
}
