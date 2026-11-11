// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.modular.ModularType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The Blueprint Table's test rig: a short loop that runs the drawn item and says its figures in motion. It is a
 * prediction, not a simulation: every number it moves by is a figure the drawing already has (the bars beside it),
 * so a swing is as fast as {@code SWING}, a blow cracks the block at the pace {@code DIG_SPEED} says, and a head
 * on a joint over its limit visibly works loose. A pick, a sword, a bow, a shield and a battery have one; any other
 * type shows none.
 */
final class BlueprintRig {

    /**
     * What the rig is run by. Fractions are 0 to 1 against a full bar of the sheet; {@code capacity} and {@code joint} are
     * the figures themselves (a joint of 1 is the limit).
     */
    record Inputs(ModularType.Family family, float swing, float reach, float impact, float dig, float damage, float joint,
                  float draw, float velocity, float capacity, float charge, int cells, int headRgb, int bodyRgb) {
    }

    private BlueprintRig() {
    }

    static boolean has(ModularType.Family family) {
        return switch (family) {
            case DIG, BLADE, HAFTED, BOW, SHIELD, CELL -> true;
            default -> false;
        };
    }

    private static int alpha(int colour, int a) {
        return a << 24 | colour & 0xFFFFFF;
    }

    /** A line {@code th} pixels thick (stacked across the run's shorter axis). */
    private static void thick(GuiGraphics g, int x0, int y0, int x1, int y1, int th, int colour) {
        boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        for (int o = 0; o < th; o++)
            GuiStyle.line(g, x0 + (steep ? o : 0), y0 + (steep ? 0 : o), x1 + (steep ? o : 0), y1 + (steep ? 0 : o), colour);
    }

    /** Draws the loop for {@code in} into the box, at {@code time} ticks (a still frame under reduce motion). */
    static void draw(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        g.enableScissor(x, y, x + w, y + h);
        g.fill(x, y + h - 7, x + w, y + h - 6, alpha(skin.ink(), 0x50)); // the bench top
        switch (in.family()) {
            case DIG, HAFTED -> dig(g, skin, in, x, y, w, h, time, still);
            case BLADE -> blade(g, skin, in, x, y, w, h, time, still);
            case BOW -> bow(g, skin, in, x, y, w, h, time, still);
            case SHIELD -> shield(g, skin, in, x, y, w, h, time, still);
            case CELL -> cell(g, skin, in, x, y, w, h, time, still);
            default -> {
            }
        }
        g.disableScissor();
    }

    /** A swung tool from {@code (px, py)}: handle, then the head across its end, the head lagging and slipping as the joint nears its limit. */
    private static void tool(GuiGraphics g, Inputs in, int px, int py, int len, float angle, float time, float headLen) {
        float loose = Mth.clamp((in.joint() - 0.7f) / 0.6f, 0f, 1f);
        float sway = (float) Math.sin(time * 0.9f) * loose * 0.35f;
        float hx = px + Mth.sin(angle) * len, hy = py - Mth.cos(angle) * len;
        thick(g, px, py, Math.round(hx), Math.round(hy), 2, 0xFF000000 | in.bodyRgb());
        float a = angle + sway;
        float slip = loose >= 1f ? 5f : loose * 2f; // a head past the limit rides off the end
        float cx = hx + Mth.sin(angle) * slip, cy = hy - Mth.cos(angle) * slip;
        float dx = Mth.cos(a) * headLen / 2, dy = Mth.sin(a) * headLen / 2;
        thick(g, Math.round(cx - dx), Math.round(cy - dy), Math.round(cx + dx), Math.round(cy + dy), 3, 0xFF000000 | in.headRgb());
    }

    private static float ease(float t) {
        return GuiStyle.easeOut(t);
    }

    // --- pick: strikes a block that cracks at a rate -------------------------------------------------

    private static final int[][] CRACKS = {{0, 0, 4, 5}, {4, 5, 9, 4}, {9, 4, 12, 9}, {4, 5, 2, 11}, {9, 4, 11, 0}, {12, 9, 15, 13}};

    private static void dig(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        int px = x + 24, py = y + h - 8, len = 30;
        float period = Mth.lerp(Mth.clamp(in.swing(), 0f, 1f), 70f, 22f);
        int blows = Math.round(Mth.lerp(Mth.clamp(in.dig(), 0f, 1f), 6f, 1f)); // hits to break: a faster dig breaks sooner
        float t = still ? period * 0.8f : time;
        int count = (int) Math.floor(t / period);
        float p = t / period - count;
        float impact = 0.72f;
        float angle = p < 0.55f ? Mth.lerp(p / 0.55f, 0.2f, -0.9f) : Mth.lerp(ease((p - 0.55f) / 0.17f), -0.9f, 1.0f);
        int phase = count % (blows + 2); // blows to crack, one empty swing while the block regrows
        int bx = x + 52, by = y + h - 7 - 22;
        boolean after = p >= impact;
        if (phase < blows || phase == blows && !after) {
            int level = phase < blows ? phase + (after ? 1 : 0) : blows;
            g.fill(bx, by, bx + 22, by + 22, alpha(skin.ink(), 0x50));
            g.renderOutline(bx, by, 22, 22, skin.ink());
            int shown = level * CRACKS.length / blows;
            for (int i = 0; i < shown; i++)
                GuiStyle.line(g, bx + 3 + CRACKS[i][0], by + 3 + CRACKS[i][1], bx + 3 + CRACKS[i][2], by + 3 + CRACKS[i][3], skin.dim());
        } else if (phase == blows) {
            float s = (p - impact) / (1 - impact);
            for (int i = 0; i < 4; i++) { // shards
                int sx = bx + 3 + i % 2 * 9 + Math.round((i % 2 * 2 - 1) * s * 10), sy = by + 3 + i / 2 * 9 + Math.round(s * s * 14);
                g.fill(sx, sy, sx + 6, sy + 5, alpha(skin.ink(), 0x60));
            }
        }
        tool(g, in, px, py, len, angle, time, 13);
    }

    // --- sword: an arc whose speed and reach are the figures ---------------------------------------

    private static void blade(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        int px = x + 22, py = y + h - 8;
        float len = 16 + 22 * Mth.clamp(in.reach(), 0f, 1f);
        float period = Mth.lerp(Mth.clamp(in.swing(), 0f, 1f), 64f, 20f);
        float t = still ? period * 0.5f : time;
        int count = (int) Math.floor(t / period);
        float p = t / period - count;
        float cut = 0.45f; // the stroke is the last of the loop; the rest is the draw back
        float angle = p < cut ? Mth.lerp(ease(p / cut), -0.7f, 1.1f) : Mth.lerp((p - cut) / (1 - cut), 1.1f, -0.7f);
        boolean stroke = p < cut;
        for (int i = 6; i >= 1 && stroke; i--) { // the arc trail
            float a = angle + i * 0.09f;
            thick(g, px, py, Math.round(px + Mth.sin(a) * len), Math.round(py - Mth.cos(a) * len), 1, alpha(skin.accent(), 0x14 * (7 - i)));
        }
        thick(g, px, py, Math.round(px + Mth.sin(angle) * len), Math.round(py - Mth.cos(angle) * len), 2, 0xFF000000 | in.headRgb());
        thick(g, px - 3, py, px + 3, py, 2, 0xFF000000 | in.bodyRgb());
        // the dummy takes the blow and keeps the dent as deep as the impact is
        int dx = x + 62, dent = 1 + Math.round(4 * Mth.clamp(in.impact(), 0f, 1f));
        boolean struck = p >= 0.38f && p < 0.75f;
        int shake = struck && !still ? Math.round(Mth.sin(time * 2.4f) * 2 * (1 - (p - 0.38f) / 0.37f)) : 0;
        g.fill(dx + shake, y + h - 7 - 26, dx + 10 + shake, y + h - 7, alpha(skin.ink(), 0x50));
        g.renderOutline(dx + shake, y + h - 7 - 26, 10, 26, skin.ink());
        if (struck)
            g.fill(dx + shake, y + h - 7 - 16, dx + dent + shake, y + h - 7 - 12, skin.warn());
        if (p >= 0.38f && p < 0.95f) {
            float s = (p - 0.38f) / 0.57f;
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, "-" + PartPresenter.num(in.damage()), dx - 6, y + h - 7 - 30 - Math.round(s * 8),
                    alpha(skin.warn(), Math.round(255 * (1 - s * 0.7f))), false);
        }
    }

    // --- bow: draws a distance and looses an arrow --------------------------------------------------

    private static void bow(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        int bx = x + 26, top = y + 4, bottom = y + h - 12, mid = (top + bottom) / 2;
        float drawTicks = Mth.lerp(Mth.clamp(in.draw(), 0f, 1f), 70f, 18f);
        float speed = Mth.lerp(Mth.clamp(in.velocity(), 0f, 1f), 1.5f, 6f); // pixels a tick
        float target = x + w - 14;
        float flight = (target - bx) / speed, stick = 14f, cycle = drawTicks + flight + stick;
        float t = still ? drawTicks : time % cycle;
        float pull = t < drawTicks ? 14f * t / drawTicks : 0f;
        float arrowX = t < drawTicks ? bx - pull : t < drawTicks + flight ? bx + (t - drawTicks) * speed : target;
        int limb = Math.round(6 + pull * 0.3f);
        for (int i = 0; i < 2; i++) { // limbs as two straight runs bending back with the draw
            int ty = i == 0 ? top : bottom;
            thick(g, bx - limb + 6, mid, bx, ty, 2, 0xFF000000 | in.headRgb());
        }
        int nock = Math.round(bx - pull - (t >= drawTicks && t < drawTicks + 6 && !still ? 2 : 0));
        GuiStyle.line(g, bx, top, nock, mid, skin.ink());
        GuiStyle.line(g, nock, mid, bx, bottom, skin.ink());
        int ax = Math.round(arrowX);
        thick(g, ax, mid, ax + 16, mid, 1, skin.ink());
        g.fill(ax + 16, mid - 1, ax + 19, mid + 2, skin.accent());
        g.fill(Math.round(target) + 8, mid - 14, Math.round(target) + 10, mid + 14, alpha(skin.ink(), 0x50));
    }

    // --- shield: absorbs a hit, then one that is too much ------------------------------------------

    private static void shield(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        float capacity = Math.max(0.5f, in.capacity());
        int strikes = Math.max(2, (int) Math.ceil(capacity * 1.6f));
        float period = 26f;
        float t = still ? period * 0.55f : time;
        int count = (int) Math.floor(t / period);
        float p = t / period - count;
        float damage = capacity * 1.8f * ((count % strikes) + 1) / strikes;
        boolean held = damage <= capacity;
        int sx = x + w / 2, mid = y + h - 7 - 14;
        float bolt = Mth.clamp(p / 0.5f, 0f, 1f);
        int bx = Math.round(Mth.lerp(bolt, x + 6, sx - 6));
        int dent = p >= 0.5f && held ? Math.round(3 * Mth.clamp(damage / capacity, 0f, 1f) * (1 - (p - 0.5f))) : 0;
        int body = 0xFF000000 | in.headRgb();
        g.fill(sx - dent, mid - 14, sx + 5 - dent, mid + 14, body);
        g.renderOutline(sx - dent, mid - 14, 5, 28, skin.ink());
        if (p < 0.5f)
            g.fill(bx, mid - 2, bx + 5, mid + 1, skin.warn());
        else if (!held) {
            int through = Math.round(Mth.lerp((p - 0.5f) / 0.5f, sx + 5, x + w - 6));
            g.fill(through, mid - 2, through + 5, mid + 1, skin.warn());
        } else
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "ok", sx - 10, mid - 24, skin.accent());
        g.drawString(net.minecraft.client.Minecraft.getInstance().font, PartPresenter.num(damage) + " / " + PartPresenter.num(capacity), x + 4, y + 3,
                held ? skin.dim() : skin.warn(), false);
    }

    // --- battery: fills and drains --------------------------------------------------------------------

    private static void cell(GuiGraphics g, GuiSkin skin, Inputs in, int x, int y, int w, int h, float time, boolean still) {
        int cells = Math.max(1, Math.min(8, in.cells()));
        float half = Mth.lerp(Mth.clamp(in.charge(), 0f, 1f), 30f, 120f);
        float t = still ? half * 0.7f : time % (2 * half);
        float level = t < half ? t / half : 2 - t / half; // 0 to 1 and back
        int cw = Math.max(6, Math.min(16, (w - 20) / cells)), bx = x + (w - cw * cells) / 2, by = y + 8, ch = h - 7 - 8 - 6;
        for (int i = 0; i < cells; i++) {
            int cx = bx + i * cw;
            g.renderOutline(cx, by, cw - 2, ch, skin.ink());
            float own = Mth.clamp(level * cells - i, 0f, 1f); // cells fill in turn
            int fill = Math.round((ch - 2) * own);
            g.fill(cx + 1, by + ch - 1 - fill, cx + cw - 3, by + ch - 1, alpha(0xFF000000 | in.headRgb(), 0xD0));
        }
        g.fill(bx + cw * cells - 1, by + ch / 3, bx + cw * cells + 2, by + ch * 2 / 3, skin.ink());
    }
}
