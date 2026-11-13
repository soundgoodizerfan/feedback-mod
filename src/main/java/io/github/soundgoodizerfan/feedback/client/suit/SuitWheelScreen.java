// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.suit;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.SuitPayload;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.suit.Ability;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The suit's ability wheel, opened by holding {@link SuitKeys#WHEEL}: ten segments round a hexagonal ring, one per {@link Ability} (a pulse fires when chosen rather than switching).
 * Letting the key go over a segment throws that ability's switch. A segment shows its state in its rim (cyan and breathing when it is on and
 * live, amber and stuttering when it is on but the suit cannot feed it, dark when it is off, hatched when no fitting provides it), and the
 * middle carries two dials: charge on the left, heat on the right. The dials read in words or figures as the suit's gauges allow
 * ({@link SuitReadout}). The ring is cut once into horizontal runs, so drawing is a few hundred fills and never a per-pixel test (the cast radial's
 * method). It only asks; the server decides ({@link SuitPayload}).
 */
final class SuitWheelScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.SUIT;
    /** The hexagon's apothem for the outer and inner edge, the gap between wedges and the lit rim, all in pixels. */
    private static final int R_OUT = 92, R_IN = 44, GAP = 3, RIM = 2;
    private static final double COS30 = Math.cos(Math.PI / 6);

    /** The ring cut into runs for {@code n} segments; one per segment count, built when first asked for. */
    private static final class Ring {
        final int n;
        final int[] runY, runX0, runX1, runCode;
        final int[] slotFrom;

        Ring(int n) {
            this.n = n;
            slotFrom = new int[n + 1];
            var runs = new java.util.ArrayList<int[]>();
            int span = (int) Math.ceil(R_OUT / COS30) + 1;
            for (int dy = -span; dy <= span; dy++) {
                int prev = -1, start = 0;
                for (int dx = -span; dx <= span + 1; dx++) {
                    int code = dx > span ? -1 : code(dx, dy);
                    if (code != prev) {
                        if (prev >= 0)
                            runs.add(new int[] { dy, start, dx, prev });
                        start = dx;
                        prev = code;
                    }
                }
            }
            runs.sort(java.util.Comparator.comparingInt(r -> r[3] >> 1));
            runY = runs.stream().mapToInt(r -> r[0]).toArray();
            runX0 = runs.stream().mapToInt(r -> r[1]).toArray();
            runX1 = runs.stream().mapToInt(r -> r[2]).toArray();
            runCode = runs.stream().mapToInt(r -> r[3]).toArray();
            for (int r = runCode.length - 1; r >= 0; r--)
                slotFrom[(runCode[r] >> 1) + 1] = Math.max(slotFrom[(runCode[r] >> 1) + 1], r + 1);
        }

        private int code(int dx, int dy) {
            double px = dx + 0.5, py = dy + 0.5, u = hex(px, py);
            if (u > R_OUT || u < R_IN)
                return -1;
            double at = Math.atan2(py, px), r = Math.sqrt(px * px + py * py);
            int slot = nearest(at);
            double off = Math.abs(wrap(at - angle(slot)));
            if (r * Math.sin(Math.PI / n - off) < GAP / 2.0)
                return -1;
            return slot * 2 + (u > R_OUT - RIM || u < R_IN + RIM ? 1 : 0);
        }

        double angle(int i) {
            return -Math.PI / 2 + i * 2 * Math.PI / n;
        }

        int nearest(double at) {
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
    }

    private static final Ring[] RINGS = new Ring[32];

    private static Ring ring(int n) {
        if (RINGS[n] == null)
            RINGS[n] = new Ring(n);
        return RINGS[n];
    }

    /** Distance from the middle in hexagon units: the projection on the nearest edge's normal, so equal values lie on one hexagon. */
    private static double hex(double px, double py) {
        double r = Math.sqrt(px * px + py * py), theta = Math.atan2(py, px) + Math.PI / 2; // a vertex points up
        double phi = Mth.positiveModulo(theta, Math.PI / 3);
        return r * Math.cos(phi - Math.PI / 6);
    }

    private static double wrap(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    /** The segments on the wheel: the five the armour's fittings give, then one for each module ability fitted. */
    private final Ability[] order = SuitClient.shown();
    private final int N = order.length;
    private final Ring ring = ring(N);

    private static final long OPEN_MS = 200;
    private final long openedAt = Util.getMillis();
    private final float[] pop = new float[N];
    private long lastMs = openedAt;
    private int lastHover = -1;

    /** The crosshair has no business under the ring. */
    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    static final class HideCrosshair {
        @SubscribeEvent
        static void onLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && net.minecraft.client.Minecraft.getInstance().screen instanceof SuitWheelScreen)
                event.setCanceled(true);
        }
    }

    SuitWheelScreen() {
        super(Component.translatable("key.feedback.suit_wheel"));
    }

    private static int fade(int argb, float a) {
        return (int) ((argb >>> 24) * a) << 24 | argb & 0xFFFFFF;
    }

    private int hovered(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (hex(dx, dy) < R_IN)
            return -1;
        return ring.nearest(Math.atan2(dy, dx));
    }

    private void choose(double mx, double my) {
        int h = hovered(mx, my);
        if (h >= 0 && SuitClient.can(order[h])) {
            PacketDistributor.sendToServer(new SuitPayload(SuitPayload.TOGGLE, order[h].ordinal()));
            GuiSkin.play(SKIN.click(), SuitClient.on(order[h]) ? 0.8f : 1.2f, 0.7f);
        }
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

    /** A rim colour for one ability's state, breathing or stuttering with the clock. */
    private int rim(Ability a, long now) {
        if (!SuitClient.can(a))
            return 0x40 << 24 | SKIN.dim() & 0xFFFFFF;
        if (!SuitClient.on(a))
            return 0xA0 << 24 | 0x2E7F94;
        if (SuitClient.live(a))
            return fade(SKIN.accent() | 0xFF000000, 0.7f + 0.3f * (float) Math.sin(now / 160.0));
        boolean flick = (now / 90) % 3 != 0;
        return fade(SKIN.warn() | 0xFF000000, flick ? 1f : 0.35f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        SuitSyncPayload s = SuitClient.state();
        long now = Util.getMillis();
        float t = GuiStyle.easeOut(Math.min(1f, (now - openedAt) / (float) OPEN_MS)), dt = Math.min(100, now - lastMs);
        lastMs = now;
        int hov = hovered(mx, my);
        boolean hovCan = hov >= 0 && SuitClient.can(order[hov]);
        if (hovCan && hov != lastHover)
            GuiSkin.play(SKIN.click(), 1.6f, 0.4f);
        lastHover = hovCan ? hov : -1;
        int ink = SKIN.ink() | 0xFF000000, dim = SKIN.dim() | 0xFF000000;

        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        // the ring unfolds: it scales up and turns the last few degrees into place
        float sc = 0.7f + 0.3f * t;
        g.pose().scale(sc, sc, 1);
        if (!ClientSettings.reduceMotion())
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((1 - t) * -14f));
        for (int i = 0; i < N; i++) {
            Ability a = order[i];
            boolean can = SuitClient.can(a), on = SuitClient.on(a);
            pop[i] += ((i == lastHover ? 1f : 0f) - pop[i]) * Math.min(1f, dt / 70f);
            int rim = rim(a, now);
            int face = !can ? 0x70 << 24 | 0x0A0E10 : on && SuitClient.live(a) ? GuiSkin.lerp(0xC0101A1E, 0xC02A5A68, 0.3f + 0.5f * pop[i])
                    : GuiSkin.lerp(0xB0141A1E, 0xC02A3A42, pop[i]);
            if (can && i == lastHover)
                rim = GuiSkin.lerp(rim, 0xFFFFFFFF, 0.35f * pop[i]);
            float push = pop[i] * 5f, dx = (float) Math.cos(ring.angle(i)) * push, dy = (float) Math.sin(ring.angle(i)) * push;
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            for (int r = ring.slotFrom[i]; r < ring.slotFrom[i + 1]; r++) {
                g.fill(ring.runX0[r], ring.runY[r], ring.runX1[r], ring.runY[r] + 1, fade((ring.runCode[r] & 1) == 0 ? face : rim, t));
                if (!can && (ring.runY[r] & 3) == 0 && (ring.runCode[r] & 1) == 0) // hatching: nothing fitted provides it
                    g.fill(ring.runX0[r], ring.runY[r], ring.runX1[r], ring.runY[r] + 1, fade(0x30 << 24 | 0x7C99A6, t));
            }
            int lr = (R_OUT + R_IN) / 2;
            int lx = (int) Math.round(Math.cos(ring.angle(i)) * lr), ly = (int) Math.round(Math.sin(ring.angle(i)) * lr);
            Component name = Component.translatable("gui.feedback.suit.ability." + a.key());
            Component state = Component.translatable(!can ? "gui.feedback.suit.state.none" : !on ? "gui.feedback.suit.state.off"
                    : SuitClient.live(a) ? "gui.feedback.suit.state.on" : "gui.feedback.suit.state.starved");
            int nameColour = !can ? dim : on ? SKIN.accent() | 0xFF000000 : ink;
            g.drawCenteredString(font, name, lx, ly - 9, fade(nameColour, t));
            g.drawCenteredString(font, state, lx, ly + 2, fade(!can ? dim : on && !SuitClient.live(a) ? SKIN.warn() | 0xFF000000 : dim, t));
            g.pose().popPose();
        }
        // the two dials in the middle: charge on the left, heat on the right
        dial(g, 150, 210, SuitReadout.shown(s.charge(), s), s.charge() < 0.25f ? SKIN.warn() | 0xFF000000 : SKIN.accent() | 0xFF000000, t, now);
        float heat = SuitReadout.shown(SuitReadout.heatShare(s), s);
        dial(g, -30, 30, heat, heat > 0.75f || s.cut() ? SKIN.warn() | 0xFF000000 : GuiSkin.lerp(SKIN.accent() | 0xFF000000, SKIN.warn() | 0xFF000000, heat), t, now);
        g.drawCenteredString(font, Component.translatable("gui.feedback.suit.dial.charge"), -24, -24, fade(dim, t));
        g.drawCenteredString(font, SuitReadout.charge(s), -24, -12, fade(ink, t));
        g.drawCenteredString(font, Component.translatable("gui.feedback.suit.dial.heat"), 24, 6, fade(dim, t));
        g.drawCenteredString(font, SuitReadout.heat(s), 24, 18, fade(s.cut() ? SKIN.warn() | 0xFF000000 : ink, t));
        g.pose().popPose();
        // the hovered ability named beneath the ring, with what a release would do
        int shown = hov >= 0 ? hov : -1;
        if (shown >= 0) {
            Ability a = order[shown];
            Component line = Component.translatable("gui.feedback.suit.hint." + (!SuitClient.can(a) ? "none" : a.pulse() ? "fire" : SuitClient.on(a) ? "off" : "on"),
                    Component.translatable("gui.feedback.suit.ability." + a.key()));
            g.drawCenteredString(font, line, width / 2, height / 2 + R_OUT + 14, fade(SKIN.accent() | 0xFF000000, t));
        }
    }

    /** A dial of forty ticks on an arc of radius 31 between two angles in degrees (clockwise from the right), filled from its start to {@code value}. */
    private void dial(GuiGraphics g, int from, int to, float value, int colour, float t, long now) {
        for (int k = 0; k < 40; k++) {
            float f = k / 39f, a = (float) Math.toRadians(Mth.lerp(f, from, to));
            int x = Math.round(Mth.cos(a) * 31), y = Math.round(Mth.sin(a) * 31);
            boolean lit = f <= value + 1e-3f;
            g.fill(x - 1, y - 1, x + 1, y + 1, fade(lit ? colour : 0x50 << 24 | 0x7C99A6, t));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        choose(mx, my);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (SuitKeys.WHEEL.matches(key, scan)) {
            var mouse = minecraft.mouseHandler;
            choose(mouse.xpos() * width / minecraft.getWindow().getScreenWidth(), mouse.ypos() * height / minecraft.getWindow().getScreenHeight());
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void tick() {
        // A release that landed before the screen existed would never reach keyReleased.
        var k = SuitKeys.WHEEL.getKey();
        if (k.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), k.getValue()))
            onClose();
        if (!SuitClient.worn())
            onClose();
    }
}
