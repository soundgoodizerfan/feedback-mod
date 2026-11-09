// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Weakness;
import io.github.soundgoodizerfan.feedback.net.SizeUpPayload;

import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Size up's read, on screen: a lock-on bracket that snaps in around each creature you have read, a thin timer under it, and a
 * card beside it with its name, live health, armour and toughness pips and a chip for every opening it is giving away.
 * An exploit or soft spot armed on it pulses the bracket and marks the body band the soft spot will hit. The server sends
 * which creatures are read and their openings ({@link SizeUpPayload}); health and armour are read off the entity itself.
 * Creatures project through the camera matrices of the last level render, so a bracket follows its creature in the frame.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SizeUpHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int SNAP_MS = 180, CARD_W = 104;
    private static final Map<Integer, Snap> SNAPS = new HashMap<>();
    private static final Matrix4f VIEW = new Matrix4f();
    private static boolean haveView;

    private static final class Snap {
        SizeUpPayload.Entry entry;
        final long since = Util.getMillis();
        long at;
        int total;

        Snap(SizeUpPayload.Entry e) {
            entry = e;
            total = Math.max(1, e.left());
        }
    }

    private SizeUpHud() {
    }

    /** Client side: take the server's list of reads in. */
    public static void take(List<SizeUpPayload.Entry> reads) {
        Map<Integer, Snap> old = new HashMap<>(SNAPS);
        SNAPS.clear();
        long now = Util.getMillis();
        for (SizeUpPayload.Entry e : reads) {
            Snap s = old.getOrDefault(e.id(), null);
            if (s == null)
                s = new Snap(e);
            s.entry = e;
            s.at = now;
            s.total = Math.max(s.total, e.left());
            SNAPS.put(e.id(), s);
        }
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("size_up"), SizeUpHud::render);
    }

    @SubscribeEvent
    static void onLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
            return;
        VIEW.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        haveView = true;
    }

    /** Screen position of a world point, or null behind the camera. */
    private static float[] project(Vec3 world, Vec3 cam, int w, int h) {
        Vector4f v = new Vector4f((float) (world.x - cam.x), (float) (world.y - cam.y), (float) (world.z - cam.z), 1f);
        VIEW.transform(v);
        if (v.w <= 0.05f)
            return null;
        return new float[] {(v.x / v.w * 0.5f + 0.5f) * w, (0.5f - v.y / v.w * 0.5f) * h};
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (SNAPS.isEmpty() || !haveView || mc.player == null || mc.level == null || mc.options.hideGui)
            return;
        float pt = delta.getGameTimeDeltaPartialTick(false);
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        long now = Util.getMillis();
        boolean calm = ClientSettings.reduceMotion();
        for (Snap s : new ArrayList<>(SNAPS.values())) {
            if (!(mc.level.getEntity(s.entry.id()) instanceof LivingEntity t) || !t.isAlive())
                continue;
            // The remaining time counts down between the server's updates.
            float left = Math.max(0f, s.entry.left() - (now - s.at) / 50f);
            AABB box = t.getBoundingBox().move(t.getPosition(pt).subtract(t.position()));
            float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
            boolean ok = true;
            for (int i = 0; i < 8 && ok; i++) {
                float[] p = project(new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ),
                        cam, g.guiWidth(), g.guiHeight());
                if (p == null) {
                    ok = false;
                    break;
                }
                x0 = Math.min(x0, p[0]);
                y0 = Math.min(y0, p[1]);
                x1 = Math.max(x1, p[0]);
                y1 = Math.max(y1, p[1]);
            }
            if (!ok)
                continue;
            // Lock on: the bracket closes in from 24 px out, then holds; the last two seconds of the read it blinks.
            float snap = calm ? 1f : GuiStyle.easeOut(Mth.clamp((now - s.since) / (float) SNAP_MS, 0f, 1f));
            float out = (1 - snap) * 24f;
            if (left < 40 && !calm && (now / 150) % 2 == 0)
                continue;
            boolean armed = (s.entry.flags() & 1) != 0;
            int spot = (s.entry.flags() >> 1) & 7;
            int colour = armed ? SKIN.warn() : SKIN.accent();
            float pulse = armed && !calm ? 0.65f + 0.35f * Mth.sin(now / 90f) : 1f;
            int c = (Math.round(255 * Mth.clamp(snap, 0f, 1f) * pulse) << 24) | colour & 0xFFFFFF;
            int bx0 = Math.round(x0 - out - 3), by0 = Math.round(y0 - out - 3), bx1 = Math.round(x1 + out + 3), by1 = Math.round(y1 + out + 3);
            bracket(g, bx0, by0, bx1, by1, c);
            // The soft spot's band across the body: head, chest, legs, feet by height.
            if (spot > 0) {
                float top = new float[] {0f, 0.2f, 0.5f, 0.75f}[spot - 1], bottom = new float[] {0.2f, 0.5f, 0.75f, 1f}[spot - 1];
                int sy0 = Math.round(Mth.lerp(top, by0, by1)), sy1 = Math.round(Mth.lerp(bottom, by0, by1));
                g.fill(bx0 + 2, sy0, bx1 - 2, sy1, (0x38 << 24) | colour & 0xFFFFFF);
                g.fill(bx0 + 2, sy0, bx1 - 2, sy0 + 1, c);
                g.fill(bx0 + 2, sy1 - 1, bx1 - 2, sy1, c);
            }
            // The read's timer, a thin line under the bracket.
            int tw = Math.round((bx1 - bx0) * Mth.clamp(left / s.total, 0f, 1f));
            g.fill(bx0, by1 + 2, bx0 + tw, by1 + 3, c);
            card(g, mc, t, s, bx1 + 6, by0, snap);
        }
    }

    /** Four corner brackets. */
    private static void bracket(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        int l = Math.max(4, Math.min(10, (x1 - x0) / 3));
        g.fill(x0, y0, x0 + l, y0 + 1, c);
        g.fill(x0, y0, x0 + 1, y0 + l, c);
        g.fill(x1 - l, y0, x1, y0 + 1, c);
        g.fill(x1 - 1, y0, x1, y0 + l, c);
        g.fill(x0, y1 - 1, x0 + l, y1, c);
        g.fill(x0, y1 - l, x0 + 1, y1, c);
        g.fill(x1 - l, y1 - 1, x1, y1, c);
        g.fill(x1 - 1, y1 - l, x1, y1, c);
    }

    /** The card beside the bracket: name, health, armour and toughness pips, and a chip for each opening. */
    private static void card(GuiGraphics g, Minecraft mc, LivingEntity t, Snap s, int x, int y, float alpha) {
        if (x + CARD_W > g.guiWidth() - 4)
            x = Math.max(4, g.guiWidth() - CARD_W - 4);
        y = Mth.clamp(y, 4, g.guiHeight() - 60);
        List<Component> chips = new ArrayList<>();
        for (int i = 0; i < Weakness.OPENING_WORDS.size(); i++)
            if ((s.entry.mask() >> i & 1) != 0)
                chips.add(Component.translatable("hud.feedback.size_up.open." + i));
        // Chips flow left to right and wrap at the card's width, each as wide as its own text.
        int inner = CARD_W - 12;
        int[] cx = new int[chips.size()], cr = new int[chips.size()];
        int px0 = 0, row = 0;
        for (int i = 0; i < chips.size(); i++) {
            int w = mc.font.width(chips.get(i)) + 4;
            if (px0 > 0 && px0 + w > inner) {
                px0 = 0;
                row++;
            }
            cx[i] = px0;
            cr[i] = row;
            px0 += w + 3;
        }
        int rows = chips.isEmpty() ? 1 : row + 1;
        int h = 36 + rows * 12 + ((s.entry.flags() & 16) != 0 ? 10 : 0);
        GuiStyle.hudCard(g, x, y, x + CARD_W, y + h, alpha);
        int ink = (Math.round(255 * alpha) << 24) | SKIN.ink() & 0xFFFFFF, dim = (Math.round(255 * alpha) << 24) | SKIN.dim() & 0xFFFFFF;
        g.drawString(mc.font, mc.font.plainSubstrByWidth(t.getDisplayName().getString(), CARD_W - 12), x + 6, y + 5, ink, false);
        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        SKIN.bar(g, x + 6, y + 16, CARD_W - 12, 6, Mth.clamp(hp / max, 0f, 1f), -1f, 0xFFB03030);
        g.drawString(mc.font, Math.round(hp) + "/" + Math.round(max), x + 6, y + 24, dim, false);
        // Armour in pips of two points; toughness in diamonds.
        int armour = Math.min(10, Math.round(t.getArmorValue() / 2f)), tough = Math.min(6, Math.round((float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
        int px = x + CARD_W - 6 - armour * 5 - (tough > 0 ? tough * 4 + 4 : 0);
        for (int i = 0; i < armour; i++)
            g.fill(px + i * 5, y + 25, px + i * 5 + 4, y + 31, (Math.round(255 * alpha) << 24) | 0x6E7C8C);
        px += armour * 5 + 4;
        for (int i = 0; i < tough; i++)
            g.fill(px + i * 4, y + 26, px + i * 4 + 3, y + 30, (Math.round(255 * alpha) << 24) | 0xE0C060);
        int cy = y + 34;
        if (chips.isEmpty())
            g.drawString(mc.font, Component.translatable("hud.feedback.size_up.closed"), x + 6, cy, dim, false);
        for (int i = 0; i < chips.size(); i++) {
            int chipX = x + 6 + cx[i], ry = cy + cr[i] * 12;
            int w = mc.font.width(chips.get(i));
            g.fill(chipX, ry - 1, chipX + w + 4, ry + 9, (Math.round(200 * alpha) << 24) | SKIN.accent() & 0xFFFFFF);
            g.drawString(mc.font, chips.get(i), chipX + 2, ry, (Math.round(255 * alpha) << 24) | 0x101010, false);
        }
        if ((s.entry.flags() & 16) != 0)
            g.drawString(mc.font, Component.translatable("hud.feedback.size_up.hits", s.entry.hits()), x + 6, y + h - 12, dim, false);
    }
}
