// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.net.GunOptionPayload;
import io.github.soundgoodizerfan.feedback.weapon.GunSwitch;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The gun option wheel, opened by holding the Gun option key on a weapon with two or more switches: one slice for every state of
 * every switch (a selector's three modes, a bipod's two), the state each stands at lit. Pointing at a slice and letting the key go sets
 * that exact state; letting it go over the middle changes nothing. Built on the same ring as the cast radial.
 */
public final class GunOptionWheel extends Screen {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int R_OUT = 88, R_IN = 36, GAP = 3, RIM = 2, LABEL_R = (R_OUT + R_IN) / 2;
    private static final long OPEN_MS = 180;

    private final boolean offHand;
    private final List<GunSwitch> switches;
    /** Per slice, the switch it belongs to and the state it sets. */
    private final int[] sliceSwitch, sliceState;
    private final int n;
    /** The ring cut into horizontal runs tagged {@code slice * 2 + rim}, sorted by slice, so drawing is a fill per run and never a per-pixel test. */
    private final int[] runY, runX0, runX1, runCode, sliceFrom;
    private final float[] pop;
    private final long openedAt = Util.getMillis();
    private long lastMs = openedAt;
    private int lastHover = -1;

    GunOptionWheel(boolean offHand, List<GunSwitch> switches) {
        super(Component.translatable("key.feedback.gun_option"));
        this.offHand = offHand;
        this.switches = switches;
        int total = switches.stream().mapToInt(s -> s.states().size()).sum();
        this.n = total;
        sliceSwitch = new int[total];
        sliceState = new int[total];
        int k = 0;
        for (int i = 0; i < switches.size(); i++)
            for (int s = 0; s < switches.get(i).states().size(); s++) {
                sliceSwitch[k] = i;
                sliceState[k++] = s;
            }
        pop = new float[total];
        var runs = new ArrayList<int[]>();
        for (int dy = -R_OUT; dy <= R_OUT; dy++) {
            int prev = -1, start = 0;
            for (int dx = -R_OUT; dx <= R_OUT + 1; dx++) {
                int code = dx > R_OUT ? -1 : code(dx, dy);
                if (code != prev) {
                    if (prev >= 0)
                        runs.add(new int[] { dy, start, dx, prev });
                    start = dx;
                    prev = code;
                }
            }
        }
        runs.sort(Comparator.comparingInt(r -> r[3] >> 1));
        runY = runs.stream().mapToInt(r -> r[0]).toArray();
        runX0 = runs.stream().mapToInt(r -> r[1]).toArray();
        runX1 = runs.stream().mapToInt(r -> r[2]).toArray();
        runCode = runs.stream().mapToInt(r -> r[3]).toArray();
        sliceFrom = new int[total + 1];
        for (int r = runCode.length - 1; r >= 0; r--)
            sliceFrom[(runCode[r] >> 1) + 1] = Math.max(sliceFrom[(runCode[r] >> 1) + 1], r + 1);
    }

    /** {@code slice * 2 + rim} for the pixel at this offset from the centre, or -1 outside the ring or in a gap. */
    private int code(int dx, int dy) {
        double px = dx + 0.5, py = dy + 0.5, r = Math.sqrt(px * px + py * py);
        if (r > R_OUT || r < R_IN)
            return -1;
        int slice = nearest(Math.atan2(py, px));
        double off = Math.abs(wrap(Math.atan2(py, px) - angle(slice)));
        if (r * Math.sin(Math.PI / n - off) < GAP / 2.0)
            return -1;
        return slice * 2 + (r > R_OUT - RIM || r < R_IN + RIM ? 1 : 0);
    }

    private static double wrap(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    private int nearest(double at) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double d = Math.abs(wrap(at - angle(i)));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private double angle(int i) {
        return -Math.PI / 2 + i * 2 * Math.PI / n;
    }

    /** The slice the pointer is nearest, or -1 while it is still near the middle. */
    private int hovered(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (dx * dx + dy * dy < R_IN * R_IN)
            return -1;
        return nearest(Math.atan2(dy, dx));
    }

    private boolean lit(int slice) {
        return switches.get(sliceSwitch[slice]).state() == sliceState[slice];
    }

    private Component stateName(int slice) {
        return Component.translatable("message.feedback.state." + switches.get(sliceSwitch[slice]).states().get(sliceState[slice]));
    }

    private Component switchName(int slice) {
        return Component.translatable("gui.feedback.gun_option." + switches.get(sliceSwitch[slice]).id());
    }

    private static int fade(int argb, float a) {
        return (int) ((argb >>> 24) * a) << 24 | argb & 0xFFFFFF;
    }

    /** The crosshair has no business under the ring. */
    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    static final class HideCrosshair {
        @SubscribeEvent
        static void onLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && Minecraft.getInstance().screen instanceof GunOptionWheel)
                event.setCanceled(true);
        }
    }

    private void choose(double mx, double my) {
        int h = hovered(mx, my);
        if (h >= 0 && !lit(h))
            PacketDistributor.sendToServer(new GunOptionPayload(offHand, sliceSwitch[h], sliceState[h]));
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
        long now = Util.getMillis();
        float t = GuiStyle.easeOut(Math.min(1f, (now - openedAt) / (float) OPEN_MS)), dt = Math.min(100, now - lastMs);
        lastMs = now;
        int hov = hovered(mx, my);
        if (hov >= 0 && hov != lastHover)
            GuiSkin.play(SKIN.click(), 1.5f, 0.5f);
        lastHover = hov;
        float pulse = 0.75f + 0.25f * (float) Math.sin(now / 180.0);
        int ink = SKIN.ink() | 0xFF000000, dim = SKIN.dim() | 0xFF000000, accent = SKIN.accent() | 0xFF000000;

        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        float s = 0.75f + 0.25f * t;
        g.pose().scale(s, s, 1);
        for (int i = 0; i < n; i++) {
            boolean on = lit(i);
            pop[i] += ((i == hov ? 1f : 0f) - pop[i]) * Math.min(1f, dt / 70f);
            int face = on ? 0xC0 << 24 | GuiSkin.lerp(0xFF141414, accent, 0.35f + 0.1f * pulse) & 0xFFFFFF
                    : GuiSkin.lerp(0xB0181C20, 0xC0384048, pop[i]);
            int rim = on ? fade(accent, pulse) : GuiSkin.lerp(0x80 << 24 | dim & 0xFFFFFF, accent, pop[i]);
            float push = pop[i] * 5f, dx = (float) Math.cos(angle(i)) * push, dy = (float) Math.sin(angle(i)) * push;
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            for (int r = sliceFrom[i]; r < sliceFrom[i + 1]; r++)
                g.fill(runX0[r], runY[r], runX1[r], runY[r] + 1, fade((runCode[r] & 1) == 0 ? face : rim, t));
            int lx = (int) Math.round(Math.cos(angle(i)) * LABEL_R), ly = (int) Math.round(Math.sin(angle(i)) * LABEL_R);
            var lines = font.split(stateName(i), 52);
            for (int l = 0; l < Math.min(2, lines.size()); l++)
                g.drawCenteredString(font, lines.get(l), lx, ly - Math.min(2, lines.size()) * 5 + l * 10, fade(on ? accent : ink, t));
            g.pose().popPose();
        }
        // The middle names the switch the pointer is over.
        if (hov >= 0) {
            var lines = font.split(switchName(hov), 2 * R_IN - 14);
            for (int l = 0; l < Math.min(3, lines.size()); l++)
                g.drawCenteredString(font, lines.get(l), 0, -Math.min(3, lines.size()) * 5 + l * 10, fade(ink, t));
        }
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        choose(mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (GunClient.OPTION.matchesMouse(button)) {
            choose(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (GunClient.OPTION.matches(key, scan)) {
            var mouse = minecraft.mouseHandler;
            choose(mouse.xpos() * width / minecraft.getWindow().getScreenWidth(), mouse.ypos() * height / minecraft.getWindow().getScreenHeight());
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void tick() {
        // A release that landed before the screen existed would never reach keyReleased.
        var k = GunClient.OPTION.getKey();
        if (k.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), k.getValue()))
            onClose();
    }
}
