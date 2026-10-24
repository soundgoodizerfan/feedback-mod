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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillPools;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The combat skill screen, opened with K: a full-window canvas in the {@link ProgrammerScreen}'s mould --
 * drag to pan, wheel to zoom about the cursor -- with a tab per tree across the top and an info plate
 * along the bottom carrying the picked node's effect, cost, status and buy button. Each node is a ringed
 * orb that carries only its name; the ring's colour and a badge say its state: [*] learned, [+] ready, [!]
 * too dear, [-] locked. The server decides every purchase; this only asks.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    public static final KeyMapping KEY = new KeyMapping("key.feedback.skills", InputConstants.KEY_K, "key.categories.feedback");

    private static final int STRIP_H = 58, INFO_H = 58, COL = 74, SPREAD = 100, R = 18, ICON = 22;
    private static final double MIN_ZOOM = 0.3, MAX_ZOOM = 2.5, ZOOM_STEP = 1.15;
    private static final GuiSkin SKIN = GuiSkin.PANEL;

    private enum State { OWNED, BUYABLE, SHORT, LOCKED }

    private final Opening opening = new Opening(SKIN);
    private final Map<String, Boolean> hasIcon = new HashMap<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Random fx = new Random();
    private int tab, sub;
    private double camX, camY, zoom = 1;
    private boolean needFit = true;
    private String picked;
    /** The slot buttons that pop up from the info plate, when they last started, and the node they started for. */
    private final List<net.minecraft.client.gui.components.Button> popButtons = new ArrayList<>();
    private long popAt;
    private String popFor;
    private SkillPools shown = SkillPools.EMPTY;
    private boolean dragging, dragMoved;
    private String flashNode;
    private long flashAt;

    /** One mote of a purchase burst, in canvas coordinates. */
    private record Spark(float x, float y, float vx, float vy, long born) {
    }

    private SkillTreeScreen() {
        super(Component.translatable("screen.feedback.skills"));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick())
            if (mc.player != null && mc.screen == null)
                mc.setScreen(new SkillTreeScreen());
    }

    private List<SkillTree> trees() {
        return SkillTrees.get().trees();
    }

    /** The pools in order of first appearance: one tab each, the trees sharing a pool being the tab's sub-tabs. */
    private List<String> tabs() {
        List<String> out = new ArrayList<>();
        for (SkillTree t : trees())
            if (!out.contains(t.pool()))
                out.add(t.pool());
        return out;
    }

    private List<SkillTree> inTab() {
        var tabs = tabs();
        if (tabs.isEmpty())
            return List.of();
        String pool = tabs.get(Math.min(tab, tabs.size() - 1));
        return trees().stream().filter(t -> t.pool().equals(pool)).toList();
    }

    private SkillTree tree() {
        var t = inTab();
        return t.isEmpty() ? null : t.get(Math.min(sub, t.size() - 1));
    }

    private static Component tabName(String pool) {
        return Component.translatableWithFallback("skill.feedback.tab." + pool, treeName(pool).getString());
    }

    private SkillPools pools() {
        return SkillPools.of(Minecraft.getInstance().player);
    }

    private static Component treeName(String id) {
        return Component.translatableWithFallback("skill.feedback.tree." + id, id);
    }

    private static Component nodeName(SkillTree t, String id) {
        return Component.translatableWithFallback("skill.feedback.node." + t.tree() + "." + id, id);
    }

    // --- camera -----------------------------------------------------------------------------

    private int viewH() {
        return height - STRIP_H - INFO_H;
    }

    private double toScreenX(double cx) {
        return (cx - camX) * zoom + width / 2.0;
    }

    private double toScreenY(double cy) {
        return (cy - camY) * zoom + STRIP_H + viewH() / 2.0;
    }

    private double toCanvasX(double sx) {
        return (sx - width / 2.0) / zoom + camX;
    }

    private double toCanvasY(double sy) {
        return (sy - STRIP_H - viewH() / 2.0) / zoom + camY;
    }

    private boolean inView(double mx, double my) {
        return mx >= 0 && mx < width && my >= STRIP_H && my < height - INFO_H;
    }

    /** {minX, minY, maxX, maxY} of the node centres. */
    private float[] bounds(Map<String, float[]> at) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] p : at.values()) {
            b[0] = Math.min(b[0], p[0]);
            b[1] = Math.min(b[1], p[1]);
            b[2] = Math.max(b[2], p[0]);
            b[3] = Math.max(b[3], p[1]);
        }
        return b;
    }

    private void fit(Map<String, float[]> at) {
        needFit = false;
        if (at.isEmpty())
            return;
        float[] b = bounds(at);
        zoom = Mth.clamp(Math.min(width / (b[2] - b[0] + 220.0), viewH() / (b[3] - b[1] + 150.0)), 0.5, 1.5);
        camX = (b[0] + b[2]) / 2;
        camY = (b[1] + b[3]) / 2;
    }

    private void clampCamera(Map<String, float[]> at) {
        if (at.isEmpty())
            return;
        float[] b = bounds(at);
        camX = Mth.clamp(camX, b[0] - 140, b[2] + 140);
        camY = Mth.clamp(camY, b[1] - 100, b[3] + 100);
    }

    // --- tree shape -------------------------------------------------------------------------

    /** How many requirement steps sit between a node and a root: its column. */
    private static int depth(SkillTree t, String id, Map<String, Integer> memo) {
        Integer known = memo.get(id);
        if (known != null)
            return known;
        memo.put(id, 0); // a cycle in the data reads as a root rather than looping
        int d = 0;
        for (String r : t.node(id).map(SkillTree.Node::requires).orElse(List.of()))
            d = Math.max(d, depth(t, r, memo) + 1);
        memo.put(id, d);
        return d;
    }

    /**
     * Each node's centre in canvas coordinates: rows by depth (the trunk at the top, growing down), and a node sits under its first requirement, so a
     * stem reads as a trunk with its branches fanned out beside it. A parent is centred on its children; leaves take
     * the next free row.
     */
    private Map<String, float[]> layout(SkillTree t) {
        Map<String, Integer> memo = new HashMap<>();
        Map<String, List<String>> kids = new HashMap<>();
        List<String> roots = new ArrayList<>();
        for (SkillTree.Node n : t.nodes()) {
            if (n.requires().isEmpty() || t.node(n.requires().get(0)).isEmpty())
                roots.add(n.id());
            else
                kids.computeIfAbsent(n.requires().get(0), k -> new ArrayList<>()).add(n.id());
        }
        Map<String, float[]> at = new HashMap<>();
        float[] row = {0};
        for (String r : roots)
            place(t, r, kids, memo, at, row);
        float mid = row[0] / 2f - 0.5f;
        at.values().forEach(p -> {
            float across = p[1], down = p[0];
            p[0] = (across - mid) * SPREAD;
            p[1] = down;
        });
        return at;
    }

    private void place(SkillTree t, String id, Map<String, List<String>> kids, Map<String, Integer> memo,
            Map<String, float[]> at, float[] row) {
        List<String> k = kids.getOrDefault(id, List.of());
        float y;
        if (k.isEmpty())
            y = row[0]++;
        else {
            for (String c : k)
                place(t, c, kids, memo, at, row);
            y = (at.get(k.get(0))[1] + at.get(k.get(k.size() - 1))[1]) / 2f;
        }
        at.put(id, new float[]{depth(t, id, memo) * COL, y}); // x: steps down, y: slot across (turned when laid out)
    }

    private boolean available(SkillTree t, SkillTree.Node n) {
        SkillPools p = pools();
        return !p.has(t.tree(), n.id()) && n.requires().stream().allMatch(r -> p.has(t.tree(), r))
                && rival(t, n) == null;
    }

    /** The owned node that shares {@code n}'s exclusive tag, or null: a variant cannot be bought beside it. */
    private SkillTree.Node rival(SkillTree t, SkillTree.Node n) {
        if (n.exclusive().isEmpty())
            return null;
        SkillPools p = pools();
        for (SkillTree.Node o : t.nodes())
            if (o != n && n.exclusive().equals(o.exclusive()) && p.has(t.tree(), o.id()))
                return o;
        return null;
    }

    /** A bracket under each exclusive group, lit when one of its variants is learned. */
    private void brackets(GuiGraphics g, SkillTree t, Map<String, float[]> at) {
        Map<String, List<SkillTree.Node>> groups = new HashMap<>();
        for (SkillTree.Node n : t.nodes())
            n.exclusive().ifPresent(tag -> groups.computeIfAbsent(tag, k -> new ArrayList<>()).add(n));
        for (List<SkillTree.Node> grp : groups.values()) {
            if (grp.size() < 2)
                continue;
            double left = Double.MAX_VALUE, right = -Double.MAX_VALUE, y = 0;
            boolean lit = false;
            for (SkillTree.Node n : grp) {
                float[] p = at.get(n.id());
                left = Math.min(left, toScreenX(p[0]));
                right = Math.max(right, toScreenX(p[0]));
                y = toScreenY(p[1]);
                lit |= pools().has(t.tree(), n.id());
            }
            int by = (int) Math.round(y + (R + 18) * zoom), x0 = (int) Math.round(left - (R + 6) * zoom),
                    x1 = (int) Math.round(right + (R + 6) * zoom), tick = (int) Math.round(6 * zoom);
            int c = argb(lit ? 0xE0 : 0x70, lit ? SKIN.accent() : SKIN.dim());
            g.fill(x0, by, x1, by + 1, c);
            g.fill(x0, by - tick, x0 + 1, by, c);
            g.fill(x1 - 1, by - tick, x1, by, c);
        }
    }

    private State state(SkillTree t, SkillTree.Node n) {
        SkillPools p = pools();
        if (p.has(t.tree(), n.id()))
            return State.OWNED;
        if (!available(t, n))
            return State.LOCKED;
        return p.points(t.pool()) >= n.cost() ? State.BUYABLE : State.SHORT;
    }

    /** The marker that says the state without colour: learned, ready, too dear, out of reach. */
    private static String glyph(State s) {
        return switch (s) {
            case OWNED -> "*";
            case BUYABLE -> "+";
            case SHORT -> "!";
            case LOCKED -> "-";
        };
    }

    private String nodeAt(SkillTree t, Map<String, float[]> at, double mx, double my) {
        if (!inView(mx, my))
            return null;
        for (SkillTree.Node n : t.nodes()) {
            float[] p = at.get(n.id());
            double sx = toScreenX(p[0]), sy = toScreenY(p[1]), r = (R + 2) * zoom;
            if ((mx - sx) * (mx - sx) + (my - sy) * (my - sy) <= r * r)
                return n.id();
            double lw = font.width(nodeName(t, n.id())) * zoom / 2;
            if (Math.abs(mx - sx) <= lw && my >= sy + R * zoom && my <= sy + (R + 12) * zoom)
                return n.id();
        }
        return null;
    }

    // --- widgets ----------------------------------------------------------------------------

    @Override
    protected void init() {
        opening.start();
        popButtons.clear();
        shown = pools();
        SkillTree t = tree();
        int doneX = width - 16 - 60, infoY = height - INFO_H;
        if (t == null) {
            addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                    width / 2 - 30, height / 2 + 20, 60, 14));
            return;
        }
        List<Component> labels = new ArrayList<>();
        for (String pool : tabs())
            labels.add(tabName(pool));
        int tw = Math.min(width - 24, 480);
        addRenderableWidget(GuiStyle.tabs(SKIN, (width - tw) / 2, 21, tw, 14, labels, () -> tab, i -> {
            tab = i;
            sub = 0;
            picked = null;
            needFit = true;
            rebuildWidgets();
        }));
        var group = inTab();
        if (group.size() > 1) {
            List<Component> names = new ArrayList<>();
            for (SkillTree each : group)
                names.add(treeName(each.tree()));
            int sw = Math.min(width - 24, 120 * group.size());
            addRenderableWidget(GuiStyle.tabs(SKIN, (width - sw) / 2, 38, sw, 14, names, () -> sub, i -> {
                sub = i;
                picked = null;
                needFit = true;
                rebuildWidgets();
            }));
        }
        t.node(picked == null ? "" : picked).ifPresent(n -> {
            boolean can = available(t, n) && pools().points(t.pool()) >= n.cost();
            var buy = GuiStyle.primary(SKIN, Component.translatable("screen.feedback.skills.buy", n.cost()),
                    b -> PacketDistributor.sendToServer(new SkillBuyPayload(t.tree(), n.id())),
                    width - 16 - 90, infoY + 12, 90, 14, FSounds.UI_STAMP);
            buy.active = can;
            addRenderableWidget(buy);
            // A castable technique, once owned, is slotted to a cast key from here.
            var cast = n.effect().unlock().map(io.github.soundgoodizerfan.feedback.combat.cast.Casts::find).orElse(null);
            if (cast != null && pools().has(t.tree(), n.id())) {
                var slots = io.github.soundgoodizerfan.feedback.combat.cast.CastState.of(minecraft.player).slots();
                for (int i = 0; i < slots.size(); i++) {
                    int slot = i;
                    boolean in = slots.get(i).equals(cast.id());
                    Component label = Component.literal((in ? "[" : "") + CombatKeys.CAST[i].getTranslatedKeyMessage().getString() + (in ? "]" : ""));
                    popButtons.add(addWidget(GuiStyle.button(SKIN, label, b -> PacketDistributor.sendToServer(
                            new io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads.Slot(slot, in ? "" : cast.id())),
                            width - 16 - 24 * 3 - 4, infoY, 24, 14)));
                }
                if (!n.id().equals(popFor)) {
                    popFor = n.id();
                    popAt = Util.getMillis();
                }
            }
        });
        if (popButtons.isEmpty())
            popFor = null;
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                doneX, infoY + 34, 60, 14));
    }

    @Override
    public void tick() {
        var slots = io.github.soundgoodizerfan.feedback.combat.cast.CastState.of(minecraft.player).slots();
        if (!slots.equals(lastSlots)) {
            lastSlots = slots;
            rebuildWidgets();
        }
        SkillPools now = pools();
        if (!now.equals(shown)) {
            SkillTree t = tree();
            if (t != null && picked != null && now.has(t.tree(), picked) && !shown.has(t.tree(), picked)) {
                flashNode = picked;
                flashAt = Util.getMillis();
                GuiSkin.play(FSounds.UI_PUNCH.get(), 1f);
                var at = layout(t).get(picked);
                if (at != null)
                    for (int i = 0; i < 40; i++) {
                        double a = fx.nextDouble() * Math.PI * 2, sp = 25 + fx.nextDouble() * 100;
                        sparks.add(new Spark(at[0], at[1], (float) (Math.cos(a) * sp), (float) (Math.sin(a) * sp), flashAt));
                    }
            }
            rebuildWidgets();
        }
    }

    private Component effectText(SkillTree.Effect e) {
        if (e.unlock().isPresent())
            return Component.translatable("skill.feedback.unlock",
                    Component.translatableWithFallback("skill.feedback.move." + e.unlock().get(), e.unlock().get()));
        String fig = e.figure().get().getSerializedName();
        return Component.translatableWithFallback("skill.feedback.figure." + fig, fig + " %s",
                String.format(Locale.ROOT, "%+.2f", e.amount()).replaceAll("\\.?0+$", ""));
    }

    // --- drawing helpers --------------------------------------------------------------------

    private static int argb(int alpha, int rgb) {
        return Mth.clamp(alpha, 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static int mix(int a, int b, float f) {
        f = Mth.clamp(f, 0f, 1f);
        int r = Math.round(Mth.lerp(f, a >> 16 & 0xFF, b >> 16 & 0xFF));
        int gr = Math.round(Mth.lerp(f, a >> 8 & 0xFF, b >> 8 & 0xFF));
        int bl = Math.round(Mth.lerp(f, a & 0xFF, b & 0xFF));
        return r << 16 | gr << 8 | bl;
    }

    /** A stable pseudo-random 0..1 per index and salt, so ambient motes need no stored state. */
    private static float rnd(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ h >>> 13) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static int half(int r, int dy) {
        return (int) Math.floor(Math.sqrt((r + 0.5) * (r + 0.5) - dy * dy));
    }

    /** A filled disc about the origin. */
    private static void disc(GuiGraphics g, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = half(r, dy);
            g.fill(-hw, dy, hw + 1, dy + 1, color);
        }
    }

    /** A ring about the origin, {@code thick} wide, its outer edge at {@code r}. */
    private static void ring(GuiGraphics g, int r, int thick, int color) {
        int in = r - thick;
        for (int dy = -r; dy <= r; dy++) {
            int ho = half(r, dy);
            if (Math.abs(dy) >= in)
                g.fill(-ho, dy, ho + 1, dy + 1, color);
            else {
                int hi = half(in, dy);
                g.fill(-ho, dy, -hi, dy + 1, color);
                g.fill(hi + 1, dy, ho + 1, dy + 1, color);
            }
        }
    }

    /** Slow aurora bands, drifting dust at three depths that shifts with the camera, and a glow under the cursor. */
    private void ambience(GuiGraphics g, int vy, int vh, long now, int mouseX, int mouseY) {
        float t = now / 1000f;
        int acc = SKIN.accent();
        for (int b = 0; b < 2; b++) {
            int cy = vy + (int) (vh * (0.35f + 0.25f * b + 0.18f * Mth.sin(t * (0.25f + 0.1f * b) + b * 2f)));
            int hh = 44, tint = mix(acc, 0xA070FF, b * 0.7f);
            g.fillGradient(0, cy - hh, width, cy, argb(0, acc), argb(26, tint));
            g.fillGradient(0, cy, width, cy + hh, argb(26, tint), argb(0, acc));
        }
        for (int i = 0; i < 90; i++) {
            float d = 0.2f + 0.8f * rnd(i, 1);
            float px = Mth.positiveModulo(rnd(i, 2) * width - (float) (camX * zoom) * d * 0.5f + Mth.sin(t * 0.7f + i) * 4f, width);
            float py = Mth.positiveModulo(rnd(i, 3) * vh - (float) (camY * zoom) * d * 0.5f - t * (3 + 11 * d), vh);
            float tw = 0.5f + 0.5f * Mth.sin(t * (1.5f + 3f * rnd(i, 4)) + i * 7f);
            int al = (int) ((50 + 150 * d) * (0.3f + 0.7f * tw));
            int col = mix(acc, 0xFFFFFF, rnd(i, 5) * 0.8f);
            int sx = (int) px, sy = vy + (int) py;
            if (d > 0.75f) {
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, argb(al / 4, col));
                g.fill(sx, sy, sx + 2, sy + 2, argb(al, col));
            } else
                g.fill(sx, sy, sx + 1, sy + 1, argb(al, col));
        }
        if (inView(mouseX, mouseY))
            for (int r = 30; r > 0; r -= 6)
                g.fill(mouseX - r, mouseY - r, mouseX + r, mouseY + r, argb(5, acc));
    }

    /** The edges of the canvas fade to dark so the web seems to float in deep space. */
    private void vignette(GuiGraphics g, int vy, int vh) {
        int e = 18;
        g.fillGradient(0, vy, width, vy + e, 0xC0000000, 0);
        g.fillGradient(0, vy + vh - e, width, vy + vh, 0, 0xC0000000);
        for (int i = 0; i < e; i++) {
            int al = (int) (0xB0 * (1 - i / (float) e) * (1 - i / (float) e));
            g.fill(i, vy, i + 1, vy + vh, argb(al, 0));
            g.fill(width - i - 1, vy, width - i, vy + vh, argb(al, 0));
        }
    }

    /** The cubic from a to b, both ends leaving vertically: a point at t, in canvas coordinates. */
    private static double[] bezier(double ax, double ay, double bx, double by, double t) {
        double k = Mth.clamp(Math.abs(by - ay) * 0.5, 20, 90), c0 = ay + k, c1 = by - k, u = 1 - t;
        return new double[]{u * u * u * ax + 3 * u * u * t * ax + 3 * u * t * t * bx + t * t * t * bx,
                u * u * u * ay + 3 * u * u * t * c0 + 3 * u * t * t * c1 + t * t * t * by};
    }

    /** A curved link, glowing with travelling light when its requirement is learned. */
    private void link(GuiGraphics g, float[] a, float[] b, boolean lit, boolean feeds, int seed, long now) {
        double ax = a[0], ay = a[1] + R + 14, bx = b[0], by = b[1] - R;
        int steps = (int) Mth.clamp((Math.abs(bx - ax) + Math.abs(by - ay)) * zoom / 5, 10, 70);
        int th = Math.max(2, (int) Math.round(2 * zoom)), h = th / 2;
        int c = lit ? SKIN.accent() : argb(0xA0, SKIN.dim());
        for (int i = 0; i <= steps; i++) {
            double[] p = bezier(ax, ay, bx, by, (double) i / steps);
            int x = (int) Math.round(toScreenX(p[0])) - h, y = (int) Math.round(toScreenY(p[1])) - h;
            if (lit)
                g.fill(x - 1, y - 1, x + th + 1, y + th + 1, argb(feeds ? 0x38 : 0x28, SKIN.accent()));
            g.fill(x, y, x + th, y + th, c);
        }
        if (!lit)
            return;
        double len = Math.abs(bx - ax) + Math.abs(by - ay) + 1;
        float head = Mth.positiveModulo(now / 1000f * (feeds ? 60f : 32f) / (float) len + rnd(seed, 9), 1f);
        for (int tr = 0; tr < 7; tr++) {
            float tt = head - tr * (float) (2.2 / len);
            if (tt < 0)
                continue;
            double[] p = bezier(ax, ay, bx, by, tt);
            int x = (int) Math.round(toScreenX(p[0])), y = (int) Math.round(toScreenY(p[1])), s = tr == 0 ? 2 : 1;
            if (tr == 0)
                g.fill(x - 3, y - 3, x + 4, y + 4, argb(0x40, SKIN.accent()));
            g.fill(x - s + 1, y - s + 1, x + s, y + s, argb(255 - tr * 34, tr == 0 ? 0xFFFFFF : mix(SKIN.accent(), 0xFFFFFF, 0.5f)));
        }
    }

    private boolean icon(String id) {
        return hasIcon.computeIfAbsent(id, k -> Minecraft.getInstance().getResourceManager()
                .getResource(Feedback.id("textures/gui/skill/" + k + ".png")).isPresent());
    }

    /** One orb at its canvas position: halo, ring, icon, state badge and its name beneath. */
    private void orb(GuiGraphics g, SkillTree t, SkillTree.Node n, float[] p, State st, boolean hot, float beat, long now) {
        double sx = toScreenX(p[0]), sy = toScreenY(p[1]);
        if (sx < -60 * zoom || sx > width + 60 * zoom || sy < STRIP_H - 60 * zoom || sy > height - INFO_H + 60 * zoom)
            return;
        boolean sel = n.id().equals(picked);
        int acc = SKIN.accent(), warn = SKIN.warn();
        int tone = st == State.SHORT ? warn : st == State.LOCKED ? SKIN.dim() : acc;
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        float sc = (float) zoom * (hot ? 1.1f : 1f);
        g.pose().scale(sc, sc, 1);
        if (st != State.LOCKED || hot || sel) {
            float pulse = st == State.BUYABLE ? 0.4f + 0.6f * beat : st == State.OWNED ? 0.55f : st == State.SHORT ? 0.3f : 0.2f;
            for (int k = 8; k >= 2; k -= 2)
                disc(g, R + k, argb((int) ((hot || sel ? 24 : 14) * pulse * (10 - k) / 4f), tone));
        }
        disc(g, R, 0xFF05070C);
        disc(g, R - 2, st == State.OWNED ? argb(0xFF, mix(0x0A0C10, acc, 0.22f)) : 0xFF10151C);
        ring(g, R, 2, st == State.BUYABLE ? 0xFF000000 | mix(acc, 0xFFFFFF, beat * 0.5f) : 0xFF000000 | tone);
        if (sel)
            ring(g, R + 4, 1, 0xFF000000 | SKIN.ink());
        if (icon(n.id())) {
            g.setColor(1f, 1f, 1f, st == State.LOCKED ? 0.35f : 1f);
            g.blit(Feedback.id("textures/gui/skill/" + n.id() + ".png"), -ICON / 2, -ICON / 2, ICON, ICON, 0, 0, 64, 64, 64, 64);
            g.setColor(1f, 1f, 1f, 1f);
        } else {
            String ch = nodeName(t, n.id()).getString();
            ch = ch.isEmpty() ? "?" : ch.substring(0, 1).toUpperCase(Locale.ROOT);
            g.drawString(font, ch, -font.width(ch) / 2, -4, st == State.LOCKED ? SKIN.dim() : SKIN.ink(), false);
        }
        if (st == State.BUYABLE) {
            double a = now / 650.0 + rnd(n.id().hashCode(), 7) * 6.28;
            int ox = (int) Math.round(Math.cos(a) * (R - 1)), oy = (int) Math.round(Math.sin(a) * (R - 1));
            g.fill(ox - 2, oy - 2, ox + 3, oy + 3, argb(0x50, acc));
            g.fill(ox - 1, oy - 1, ox + 2, oy + 2, 0xFFFFFFFF);
        }
        int bx = (int) (R * 0.72), by = (int) (-R * 0.72);
        g.pose().pushPose();
        g.pose().translate(bx, by, 10);
        disc(g, 5, 0xFF05070C);
        disc(g, 4, 0xFF000000 | tone);
        String gl = glyph(st);
        g.drawString(font, gl, -font.width(gl) / 2, -4, 0xFF05070C, false);
        g.pose().popPose();
        Component name = nodeName(t, n.id());
        g.drawString(font, name, -font.width(name) / 2, R + 5, st == State.LOCKED ? SKIN.dim() : SKIN.ink(), true);
        g.pose().popPose();
        if (n.id().equals(flashNode)) {
            float f = (now - flashAt) / 900f;
            if (f < 1) {
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale((float) zoom, (float) zoom, 1);
                for (int rg = 0; rg < 3; rg++) {
                    float rf = Mth.clamp(f * 1.4f - rg * 0.18f, 0f, 1f);
                    if (rf > 0 && rf < 1)
                        ring(g, R + Math.round(rf * (18 + rg * 12)), 2,
                                argb((int) ((1 - rf) * 0xFF), mix(acc, 0xFFFFFF, 1 - rf)));
                }
                disc(g, R, argb((int) ((1 - f) * (1 - f) * 0xB0), 0xFFFFFF));
                g.pose().popPose();
            }
        }
    }

    // --- render -----------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        opening.render(g, width, height, 0, 0, width, height, 0.3f, () -> {
            SkillTree t = tree();
            long now = Util.getMillis();
            int vy = STRIP_H, vh = viewH();
            if (t == null) {
                g.fill(0, 0, width, height, 0xFF05070C);
                g.drawCenteredString(font, Component.translatable("screen.feedback.skills.empty"), width / 2, height / 2 - 10, SKIN.dim());
                super.render(g, mouseX, mouseY, partialTick);
                return;
            }
            Map<String, float[]> at = layout(t);
            if (needFit)
                fit(at);
            clampCamera(at);
            float beat = 0.5f + 0.5f * Mth.sin(now / 420f);
            g.enableScissor(0, vy, width, vy + vh);
            g.fill(0, vy, width, vy + vh, 0x7005070C); // translucent: the world shows through
            ambience(g, vy, vh, now, mouseX, mouseY);
            SkillPools pl = pools();
            for (SkillTree.Node n : t.nodes())
                for (String r : n.requires()) {
                    float[] a = at.get(r), b = at.get(n.id());
                    if (a == null || b == null)
                        continue;
                    boolean got = pl.has(t.tree(), r);
                    link(g, a, b, got, got && !pl.has(t.tree(), n.id()), n.id().hashCode() ^ r.hashCode(), now);
                }
            brackets(g, t, at);
            String over = opening.settled() && !dragMoved ? nodeAt(t, at, mouseX, mouseY) : null;
            for (SkillTree.Node n : t.nodes())
                orb(g, t, n, at.get(n.id()), state(t, n), n.id().equals(over), beat, now);
            sparks.removeIf(sp -> now - sp.born() > 1100);
            for (Spark sp : sparks) {
                float age = (now - sp.born()) / 1000f, travel = (1 - (float) Math.exp(-3.2 * age)) / 3.2f;
                int sx = (int) Math.round(toScreenX(sp.x() + sp.vx() * travel)), sy = (int) Math.round(toScreenY(sp.y() + sp.vy() * travel + 14 * age * age));
                int al = (int) (255 * Math.max(0f, 1f - age / 1.1f));
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, argb(al / 3, SKIN.accent()));
                g.fill(sx, sy, sx + 1, sy + 1, argb(al, 0xFFFFFF));
            }
            vignette(g, vy, vh);
            Component legend = Component.translatable("screen.feedback.skills.legend");
            g.drawString(font, legend, 12, vy + vh - 12, SKIN.dim(), false);
            g.disableScissor();

            // The top strip and bottom plate are the instrument panel, their edge screws pushed off the window.
            SKIN.panel(g, -4, -4, width + 8, STRIP_H + 4);
            pop(); // the slot buttons rise from behind the plate: drawn first, so it covers them
            for (var b : popButtons)
                if (b.visible)
                    b.render(g, mouseX, mouseY, partialTick);
            SKIN.panel(g, -4, height - INFO_H, width + 8, INFO_H + 4);
            title(g, now);
            g.drawString(font, Component.translatable("screen.feedback.skills.pool", pl.points(t.pool())), 12, 8, SKIN.accent(), false);
            info(g, t);
            super.render(g, mouseX, mouseY, partialTick);
        });
    }

    private void title(GuiGraphics g, long now) {
        String ts = this.title.getString().toUpperCase(Locale.ROOT);
        int tw = font.width(ts), cx = 0, tx = (width - tw) / 2;
        float sweep = now % 3600 / 3600f * (tw + 90) - 45;
        for (int i = 0; i < ts.length(); i++) {
            String ch = ts.substring(i, i + 1);
            float k = Math.max(0f, 1f - Math.abs(cx - sweep) / 20f);
            if (k > 0.2f)
                g.drawString(font, ch, tx + cx, 8, argb((int) (k * 0x50), SKIN.accent()), false);
            g.drawString(font, ch, tx + cx, 8, 0xFF000000 | mix(SKIN.ink(), 0xFFFFFF, k), false);
            cx += font.width(ch);
        }
    }

    /** The slot buttons rise out of the plate one after another and settle with a small overshoot. */
    private void pop() {
        int rest = height - INFO_H - 18, from = height - INFO_H + 4;
        for (int i = 0; i < popButtons.size(); i++) {
            float t = Math.min(1f, Math.max(0f, (Util.getMillis() - popAt - i * 70L) / 260f));
            float c = 1.70158f, u = t - 1f;
            float ease = t >= 1f ? 1f : 1f + (c + 1) * u * u * u + c * u * u; // ease-out-back
            var b = popButtons.get(i);
            b.visible = t > 0f;
            b.setX(width - 16 - 24 * 3 - 4 + i * 26);
            b.setY(Math.round(from + (rest - from) * ease));
        }
    }

    /** The bottom plate: the picked node's name, effect, cost and status, and a description when it has one. */
    private void info(GuiGraphics g, SkillTree t) {
        int y = height - INFO_H + 9, x = 16;
        var n = t.node(picked == null ? "" : picked).orElse(null);
        if (n == null) {
            g.drawString(font, Component.translatable("screen.feedback.skills.pick"), x, y, SKIN.dim(), false);
            return;
        }
        g.drawString(font, nodeName(t, n.id()), x, y, SKIN.ink(), false);
        g.drawString(font, effectText(n.effect()), x, y + 11, SKIN.ink(), false);
        g.drawString(font, Component.translatable("screen.feedback.skills.cost", n.cost()), x, y + 22, SKIN.dim(), false);
        g.drawString(font, statusText(t, n), x, y + 33, SKIN.warn(), false);
        n.needsTrait().ifPresent(tr -> g.drawString(font, Component.translatable("screen.feedback.skills.needs_trait",
                Component.translatableWithFallback("trait.feedback." + tr, tr)), x + 120, y + 22, SKIN.dim(), false));
        n.effect().unlock().ifPresent(m -> {
            Component d = Component.translatableWithFallback("skill.feedback.move." + m + ".desc", "");
            if (d.getString().isEmpty())
                return;
            int dx = Math.max(x + 190, width / 2 - 60), dw = width - 130 - dx;
            int ly = y;
            for (var line : font.split(d, Math.max(80, dw))) {
                if (ly > height - 14)
                    break;
                g.drawString(font, line, dx, ly, SKIN.dim(), false);
                ly += 10;
            }
        });
    }

    private Component statusText(SkillTree t, SkillTree.Node n) {
        return switch (state(t, n)) {
            case OWNED -> Component.translatable("screen.feedback.skills.owned");
            case LOCKED -> Component.translatable("screen.feedback.skills.locked", missing(t, n));
            case SHORT -> Component.translatable("screen.feedback.skills.short");
            case BUYABLE -> Component.translatable("screen.feedback.skills.ready");
        };
    }

    private Component missing(SkillTree t, SkillTree.Node n) {
        List<Component> need = new ArrayList<>();
        SkillTree.Node rival = rival(t, n);
        if (rival != null)
            need.add(Component.translatable("screen.feedback.skills.excludes", nodeName(t, rival.id())));
        for (String r : n.requires())
            if (!pools().has(t.tree(), r))
                need.add(Component.translatable("screen.feedback.skills.needs", nodeName(t, r)));
        return ComponentUtils.formatList(need, Component.literal(", "));
    }

    /** The dimming is the opening's. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    // --- input ------------------------------------------------------------------------------

    private java.util.List<String> lastSlots = java.util.List.of();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!opening.settled())
            return true;
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;
        dragging = button == 0 && inView(mouseX, mouseY);
        dragMoved = false;
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging)
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        if (dragMoved || Math.abs(dx) + Math.abs(dy) > 1) {
            dragMoved = true;
            camX -= dx / zoom;
            camY -= dy / zoom;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            SkillTree t = tree();
            String hit = dragMoved || t == null ? null : nodeAt(t, layout(t), mouseX, mouseY);
            dragMoved = false;
            if (hit != null) {
                picked = hit;
                GuiSkin.play(FSounds.UI_DETENT.get(), 1f);
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tree() == null || !inView(mouseX, mouseY))
            return false;
        double fx0 = toCanvasX(mouseX), fy0 = toCanvasY(mouseY);
        zoom = Mth.clamp(zoom * Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
        camX = fx0 - (mouseX - width / 2.0) / zoom;
        camY = fy0 - (mouseY - STRIP_H - viewH() / 2.0) / zoom;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (KEY.matches(key, scan) && !opening.closing()) {
            onClose();
            return true;
        }
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
