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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The fan of the bench ({@code docs/gui_bench.md}): nodes on radial rings around the centre, floating (a slow drift and bob), glowing
 * under the cursor, pannable by dragging and zoomable about the cursor, as {@code SkillTreeScreen}'s web is, but warm and physical:
 * a dark orb with a rim, a shadow beneath it and the item in it. The fan knows nothing of menus; the screen hands it {@link Node}s and
 * asks what is under the pointer. All motion is driven by {@link GuiStyle#time()}, still when paused and removed under reduce motion.
 * Drawing allocates nothing.
 */
final class BenchFan {

    /** What a node does when it is clicked. */
    enum Kind { SHAPE, CUT, BREAK, FITTED, HELD, BUILD, ROLE, ALT, OFF, PREP, HUB }

    /**
     * One node. {@code group} picks the ring it lies on; {@code action} is the menu button it presses (or the screen's own code for
     * a {@link Kind#ROLE}, {@link Kind#HUB} or {@link Kind#PREP}); {@code index} is whatever the screen needs to find it again.
     */
    static final class Node {
        final Kind kind;
        final ItemStack icon;
        final Component label;
        final String glyph;
        final int action, group, index;
        /** Drawn faint: the stock cannot do it yet. A click says so. */
        boolean faint;
        /** The shape is played as a rhythm: an ember rim. */
        boolean forged;
        /** The role or hub chosen. */
        boolean selected;
        /** 0 to 1 how worn a part is, or -1. */
        float wear = -1f;
        /** A hub's members, shown around it while it is open. */
        List<Node> kids = List.of();
        Node parent;
        // layout (canvas units) and look
        float ang, rad, jx, jy, ph, px, py, glow;

        Node(Kind kind, ItemStack icon, Component label, String glyph, int action, int group, int index) {
            this.kind = kind;
            this.icon = icon;
            this.label = label;
            this.glyph = glyph;
            this.action = action;
            this.group = group;
            this.index = index;
        }
    }

    static final int R = 11, RING0 = 50, RING_STEP = 36;

    private final List<Node> top = new ArrayList<>();
    private final List<Node> all = new ArrayList<>();
    double camX, camY, zoom = 1;
    private float maxRad = 60;
    private Node over;

    List<Node> nodes() {
        return all;
    }

    Node over() {
        return over;
    }

    boolean isEmpty() {
        return top.isEmpty();
    }

    /** Replace the nodes, lay them out on rings by group, and (if {@code recentre}) bring the camera back to fit them. */
    void set(List<Node> nodes, boolean recentre, int halfW, int halfH) {
        top.clear();
        top.addAll(nodes);
        all.clear();
        int ring = 0, i = 0;
        while (i < top.size()) {
            int g = top.get(i).group, j = i;
            while (j < top.size() && top.get(j).group == g)
                j++;
            int remaining = j - i, at = i;
            while (remaining > 0) {
                float radius = RING0 + ring * RING_STEP;
                int cap = Math.max(4, (int) (Mth.TWO_PI * radius / 30f)), take = Math.min(remaining, cap);
                // Spread what is left evenly over the rings it needs, so no ring is left with one stray node.
                int rings = (remaining + cap - 1) / cap;
                take = Math.min(take, (remaining + rings - 1) / rings);
                float start = -Mth.HALF_PI + ring * 0.6f;
                for (int k = 0; k < take; k++) {
                    Node n = top.get(at + k);
                    int h = (n.index * 0x9E3779B1) ^ (n.action * 0x85EBCA6B) ^ (n.kind.ordinal() * 0x1B873593);
                    n.ang = start + Mth.TWO_PI * k / take + ((h & 0xFF) / 255f - 0.5f) * 0.10f;
                    n.rad = radius + (((h >>> 8) & 0xFF) / 255f - 0.5f) * 7f;
                    n.ph = ((h >>> 16) & 0xFF) / 255f * Mth.TWO_PI;
                    n.px = Mth.cos(n.ang) * n.rad;
                    n.py = Mth.sin(n.ang) * n.rad;
                    all.add(n);
                }
                at += take;
                remaining -= take;
                ring++;
            }
            i = j;
        }
        for (Node n : new ArrayList<>(all))
            place(n);
        float far = 0;
        for (Node n : all)
            far = Math.max(far, n.rad + R + (n.kids.isEmpty() ? 0 : 34));
        maxRad = Math.max(60, far);
        if (recentre) {
            camX = camY = 0;
            zoom = Mth.clamp(Math.min(halfW - 10, halfH - 6) / maxRad, 0.5, 1.4);
        }
    }

    /** A hub's members: an arc outward from the hub, a second row when many. Called when a hub opens or the fan is laid out. */
    private void place(Node hub) {
        int n = hub.kids.size();
        for (int k = 0; k < n; k++) {
            Node kid = hub.kids.get(k);
            kid.parent = hub;
            int row = k / 5, col = k % 5, inRow = Math.min(5, n - row * 5);
            float a = hub.ang + (col - (inRow - 1) / 2f) * 0.42f;
            kid.ang = a;
            kid.rad = hub.rad + 30 + row * 26;
            kid.px = Mth.cos(a) * kid.rad;
            kid.py = Mth.sin(a) * kid.rad;
            kid.ph = hub.ph + 1.3f + k;
        }
    }

    /** Where {@code n} floats now, in canvas units: its place plus a slow drift and bob. */
    private float fx(Node n, float t) {
        return n.px + (ClientSettings.reduceMotion() ? 0 : Mth.sin(t * 0.035f + n.ph) * 1.6f);
    }

    private float fy(Node n, float t) {
        return n.py + (ClientSettings.reduceMotion() ? 0 : Mth.cos(t * 0.05f + n.ph * 1.3f) * 2.2f);
    }

    // --- camera ------------------------------------------------------------------------------

    void pan(double dx, double dy) {
        camX = Mth.clamp(camX - dx / zoom, -maxRad - 40, maxRad + 40);
        camY = Mth.clamp(camY - dy / zoom, -maxRad - 40, maxRad + 40);
    }

    /** Zoom about the cursor: the canvas point under (mx, my) stays under it. {@code cx, cy} is the centre on screen. */
    void zoomAbout(double scroll, double mx, double my, int cx, int cy) {
        double fx = (mx - cx) / zoom + camX, fy = (my - cy) / zoom + camY;
        zoom = Mth.clamp(zoom * Math.pow(1.12, scroll), 0.45, 2.2);
        camX = fx - (mx - cx) / zoom;
        camY = fy - (my - cy) / zoom;
    }

    private int sx(float wx, int cx) {
        return Math.round((float) ((wx - camX) * zoom) + cx);
    }

    private int sy(float wy, int cy) {
        return Math.round((float) ((wy - camY) * zoom) + cy);
    }

    /** The node under (mx, my), if any; hubs' open members first. */
    Node at(double mx, double my, int cx, int cy) {
        float t = GuiStyle.time();
        Node best = null;
        double bestD = Double.MAX_VALUE;
        for (Node n : all) {
            best = nearer(n, mx, my, cx, cy, t, best);
            if (best != null)
                bestD = 0;
            for (Node k : n.kids)
                if (n.selected)
                    best = nearer(k, mx, my, cx, cy, t, best);
        }
        return best;
    }

    private Node nearer(Node n, double mx, double my, int cx, int cy, float t, Node best) {
        double dx = mx - sx(fx(n, t), cx), dy = my - sy(fy(n, t), cy), r = (R + 3) * zoom;
        double d = dx * dx + dy * dy;
        if (d > r * r)
            return best;
        if (best == null)
            return n;
        double bx = mx - sx(fx(best, t), cx), by = my - sy(fy(best, t), cy);
        return d < bx * bx + by * by ? n : best;
    }

    // --- drawing ----------------------------------------------------------------------------

    /**
     * Draw the fan with its centre at (cx, cy) on screen, clipped to the rectangle (x, y, w, h). {@code shake} is sideways pixels
     * of a rejected placement; {@code dim} fades the whole fan while the stock is being worked.
     */
    void draw(GuiGraphics g, GuiSkin skin, Font font, int x, int y, int w, int h, int cx, int cy, int mx, int my, int shake, float dim) {
        float t = GuiStyle.time();
        over = at(mx, my, cx, cy);
        g.enableScissor(x, y, x + w, y + h);
        cx += shake;
        int accent = skin.accent();
        // Links first: light travelling from the centre out to the inner ring and to what the pointer is on.
        float pulse = ClientSettings.reduceMotion() ? 0.5f : 0.5f + 0.5f * Mth.sin(t * 0.12f);
        for (Node n : all) {
            if (n.rad > RING0 + 4 && n != over)
                continue;
            link(g, cx, cy, sx(fx(n, t), cx), sy(fy(n, t), cy), accent, n == over ? 0.9f : 0.28f * dim, t, n.ph, pulse);
        }
        for (Node n : all) {
            for (Node k : n.kids)
                if (n.selected)
                    link(g, sx(fx(n, t), cx), sy(fy(n, t), cy), sx(fx(k, t), cx), sy(fy(k, t), cy), accent, 0.4f * dim, t, k.ph, pulse);
        }
        for (Node n : all) {
            orb(g, skin, font, n, cx, cy, n == over, t, dim);
            if (n.selected)
                for (Node k : n.kids)
                    orb(g, skin, font, k, cx, cy, k == over, t, dim);
        }
        g.disableScissor();
    }

    /** A short thread of dots with a bead of light running along it. */
    private void link(GuiGraphics g, int x0, int y0, int x1, int y1, int accent, float strength, float t, float seed, float pulse) {
        if (strength <= 0.02f)
            return;
        int dx = x1 - x0, dy = y1 - y0, steps = Mth.clamp((int) (Math.sqrt((double) dx * dx + (double) dy * dy) / 5), 3, 40);
        int a = (int) (0x50 * strength);
        for (int i = 3; i < steps; i++) {
            float f = (float) i / steps;
            // a slight sag so the thread hangs rather than ruling a line
            int px = x0 + Math.round(dx * f), py = y0 + Math.round(dy * f + Mth.sin(f * Mth.PI) * 3);
            g.fill(px, py, px + 1, py + 1, a << 24 | accent & 0xFFFFFF);
        }
        float head = ClientSettings.reduceMotion() ? 0.6f : Mth.positiveModulo(t * 0.02f + seed, 1f);
        int bx = x0 + Math.round(dx * head), by = y0 + Math.round(dy * head + Mth.sin(head * Mth.PI) * 3);
        int ba = (int) ((0x50 + 0x70 * pulse) * strength);
        g.fill(bx - 1, by - 1, bx + 2, by + 2, ba << 24 | 0xFFE0B0);
    }

    private void orb(GuiGraphics g, GuiSkin skin, Font font, Node n, int cx, int cy, boolean hot, float t, float dim) {
        int px = sx(fx(n, t), cx), py = sy(fy(n, t), cy);
        float z = (float) zoom;
        int r = Math.max(5, Math.round((R + (hot ? 2 : 0)) * z));
        if (px < -r || py < -r)
            return;
        n.glow += ((hot ? 1f : 0f) - n.glow) * (ClientSettings.reduceMotion() ? 1f : 0.25f);
        float faint = n.faint ? 0.38f : 1f, f = faint * Mth.clamp(dim, 0.15f, 1f);
        int ink = skin.ink(), accent = skin.accent();
        // Weight: a soft shadow below, then the warm body.
        GuiStyle.disc(g, px + 1, py + Math.max(2, r / 4), r, a(0x58 * f) | 0x000000);
        int rim = n.forged ? 0xE08A3A : n.selected ? ink : GuiSkin.lerp(0xFF000000 | skin.dim(), 0xFF000000 | accent, n.glow) & 0xFFFFFF;
        GuiStyle.disc(g, px, py, r, a(0xFF * f) | rim);
        GuiStyle.disc(g, px, py, r - 2, a(0xFF * f) | (GuiSkin.lerp(0xFF1B1511, 0xFF3A2A1C, n.glow) & 0xFFFFFF));
        if (n.glow > 0.02f)
            for (int k = 1; k <= 3; k++)
                GuiStyle.ring(g, px, py, r + k * 2, 2, a(0x30 * n.glow * (4 - k) / 3f * f) | 0xFFB060);
        if (n.selected)
            GuiStyle.ring(g, px, py, r + 3, 1, 0xFF000000 | ink);
        if (!n.icon.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(px, py, 20);
            g.pose().scale(z * (hot ? 1.1f : 1f), z * (hot ? 1.1f : 1f), 1f);
            g.renderItem(n.icon, -8, -8);
            if (n.icon.getCount() > 1 && n.kind != Kind.HELD)
                g.renderItemDecorations(font, n.icon, -8, -8);
            g.pose().popPose();
            if (n.faint)
                GuiStyle.disc(g, px, py, r - 2, 0x90000000 | 0x1B1511);
        } else if (!n.glyph.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(px, py, 20);
            g.pose().scale(z, z, 1f);
            g.drawString(font, n.glyph, -font.width(n.glyph) / 2, -4, a(0xFF * f) | ink & 0xFFFFFF, false);
            g.pose().popPose();
        }
        if (n.kind == Kind.HUB) {
            // the number of shapes inside, as a pip
            String c = Integer.toString(n.kids.size());
            GuiStyle.disc(g, px + r - 1, py - r + 1, 5, 0xFF000000 | accent & 0xFFFFFF);
            g.drawString(font, c, px + r - 1 - font.width(c) / 2, py - r - 2, 0xFF05070C, false);
        }
        if (n.wear >= 0f && n.wear > 0.02f) {
            int bw = Math.max(8, r * 2 - 2);
            int bx = px - bw / 2, by = py + r + 2;
            g.fill(bx, by, bx + bw, by + 2, 0xB0000000);
            g.fill(bx, by, bx + Math.round(bw * (1f - n.wear)), by + 2, 0xFF000000 | (n.wear > 0.7f ? skin.warn() : accent) & 0xFFFFFF);
        }
    }

    private static int a(float alpha) {
        return Mth.clamp((int) alpha, 0, 255) << 24;
    }
}
