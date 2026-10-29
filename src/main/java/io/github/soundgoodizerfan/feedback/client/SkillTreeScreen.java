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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads;
import io.github.soundgoodizerfan.feedback.combat.cast.CastState;
import io.github.soundgoodizerfan.feedback.combat.cast.Casts;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillPools;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees;
import io.github.soundgoodizerfan.feedback.combat.skill.TreeHue;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
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
    /** The left column of the plate, the description box's left edge and its height, and the room kept for the buy and slot buttons. */
    private static final int COLUMN_W = 260, DESC_X = 290, DESC_RIGHT = 130;
    /** The loadout's orb pitch across a row. */
    private static final int EQUIP_CELL = 80;
    private static final double MIN_ZOOM = 0.3, MAX_ZOOM = 2.5, ZOOM_STEP = 1.15;
    private static final GuiSkin SKIN = GuiSkin.PANEL;

    private enum State { OWNED, BUYABLE, SHORT, LOCKED }

    private final Opening opening = new Opening(SKIN);
    /** Each node's icon side in pixels (icons come as 16 or 64), 0 when it has none. */
    private final Map<String, Integer> iconSide = new HashMap<>();
    /** Each technique's icon texture name, found once. */
    private final Map<String, String> castIcon = new HashMap<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Random fx = new Random();
    private int tab, sub;
    /** The screen's hue: eased toward the tab's colour, so switching tabs washes from one to the next. */
    private float[] tint;
    private long tintAt;
    /** The top-level tab: the trees, or the loadout (which techniques sit in the cast keys). */
    private boolean equip;
    private Component descFull = Component.empty();
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

    /** Ease the tint toward the current tab's colour; frame-rate independent, about a third of a second. */
    private int tint(long now) {
        var tabs = tabs();
        int goal = equip || tabs.isEmpty() ? SKIN.accent() : TreeHue.of(tabs.get(Math.min(tab, tabs.size() - 1)));
        float[] to = {goal >> 16 & 0xFF, goal >> 8 & 0xFF, goal & 0xFF};
        if (tint == null) {
            tint = to;
        } else {
            float k = 1f - (float) Math.exp(-Math.min(100, now - tintAt) / 90.0);
            for (int i = 0; i < 3; i++)
                tint[i] += (to[i] - tint[i]) * k;
        }
        tintAt = now;
        return Math.round(tint[0]) << 16 | Math.round(tint[1]) << 8 | Math.round(tint[2]);
    }

    /** The accent, pulled toward the tab's colour. */
    private int accent() {
        return tint == null ? SKIN.accent() : mix(SKIN.accent(), Math.round(tint[0]) << 16 | Math.round(tint[1]) << 8 | Math.round(tint[2]), 0.85f);
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
            int c = argb(lit ? 0xE0 : 0x70, lit ? accent() : SKIN.dim());
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
        int topW = Math.min(width - 24, 200);
        List<Component> top = List.of(Component.translatable("screen.feedback.skills.trees"),
                Component.translatable("screen.feedback.skills.equip"));
        addRenderableWidget(GuiStyle.tabs(SKIN, (width - topW) / 2, 4, topW, 14, top, () -> equip ? 1 : 0, i -> {
            equip = i == 1;
            picked = null;
            needFit = true;
            rebuildWidgets();
        }));
        if (equip) {
            initEquip();
            return;
        }
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
            var cast = n.effect().unlock().map(Casts::find).orElse(null);
            if (cast != null && pools().has(t.tree(), n.id()))
                slotButtons(cast.id(), n.id());
        });
        if (popButtons.isEmpty())
            popFor = null;
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                doneX, infoY + 34, 60, 14));
    }

    /** The cast-key buttons that rise out of the plate: each puts {@code move} in its key, or takes it out if it is there. */
    private void slotButtons(String move, String key) {
        var slots = CastState.of(minecraft.player).slots();
        for (int i = 0; i < slots.size(); i++) {
            int slot = i;
            boolean in = slots.get(i).equals(move);
            Component label = Component.literal((in ? "[" : "") + CombatKeys.castLabel(i).getString() + (in ? "]" : ""));
            popButtons.add(addWidget(GuiStyle.button(SKIN, label, b -> PacketDistributor.sendToServer(
                    new CastPayloads.Slot(slot, in ? "" : move)),
                    width - 16 - 24 * 3 - 4, height - INFO_H, 24, 14)));
        }
        if (!key.equals(popFor)) {
            popFor = key;
            popAt = Util.getMillis();
        }
    }

    /**
     * The loadout, drawn on the same canvas and plate as the trees: the cast keys as orbs along the top, every castable
     * technique the player has (bought, or granted by worn gear) as orbs beneath. Picking one puts what it does on the
     * plate, with the key buttons to slot it.
     */
    private void initEquip() {
        if (picked != null && Casts.find(picked) != null && Skills.owned(minecraft.player, picked))
            slotButtons(picked, picked);
        else
            popFor = null;
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                width - 16 - 60, height - INFO_H + 34, 60, 14));
    }

    /**
     * Where the loadout's orbs sit, in screen pixels: the keys ({@code "#0"} on) in a row, then the techniques in
     * centred rows. shortcut: no scroll, so a player owning more techniques than fit the window loses the last rows off
     * the bottom; add a by-pixel scroll when that happens.
     */
    private Map<String, int[]> equipSpots() {
        Map<String, int[]> at = new LinkedHashMap<>();
        int keys = CastState.of(minecraft.player).slots().size(), top = STRIP_H + 30;
        for (int i = 0; i < keys; i++)
            at.put("#" + i, new int[]{width / 2 + Math.round((i - (keys - 1) / 2f) * 100), top});
        List<String> have = Casts.ALL.stream().map(Casts.Cast::id).filter(id -> Skills.owned(minecraft.player, id)).toList();
        int cols = Math.max(1, Math.min(have.size(), (width - 40) / EQUIP_CELL));
        for (int k = 0; k < have.size(); k++) {
            int row = k / cols, inRow = Math.min(cols, have.size() - row * cols);
            at.put(have.get(k), new int[]{width / 2 + Math.round((k % cols - (inRow - 1) / 2f) * EQUIP_CELL), top + 84 + row * 56});
        }
        return at;
    }

    private String equipAt(double mx, double my) {
        if (!inView(mx, my))
            return null;
        for (var e : equipSpots().entrySet()) {
            int[] p = e.getValue();
            if (Math.abs(mx - p[0]) <= EQUIP_CELL / 2 - 4 && my >= p[1] - R - 2 && my <= p[1] + R + (e.getKey().startsWith("#") ? 24 : 14))
                return e.getKey();
        }
        return null;
    }

    /** The move's name, or its id until the lang has one. */
    private static String moveName(String move) {
        return Component.translatableWithFallback("skill.feedback.move." + move, move).getString();
    }

    @Override
    public void tick() {
        var slots = CastState.of(minecraft.player).slots();
        if (!slots.equals(lastSlots)) {
            lastSlots = slots;
            rebuildWidgets();
        }
        SkillPools now = pools();
        if (!now.equals(shown)) {
            SkillTree t = tree();
            if (!equip && t != null && picked != null && now.has(t.tree(), picked) && !shown.has(t.tree(), picked)) {
                flashNode = picked;
                flashAt = Util.getMillis();
                GuiSkin.play(FSounds.UI_PUNCH.get(), 1f);
                SKIN.cue(GuiSkin.Cue.FINISH);
                var at = layout(t).get(picked);
                if (at != null && !ClientSettings.reduceMotion())
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

    /** White discs and rings baked once per size: one textured quad an orb layer instead of a fill per pixel row. */
    private static final Map<Long, ResourceLocation> SHAPES = new HashMap<>();

    /** The baked shape of outer radius {@code r}; {@code in} is the ring's inner radius, or -1 for a disc. */
    private static ResourceLocation shape(int r, int in) {
        return SHAPES.computeIfAbsent((long) r << 32 | (in & 0xFFFFFFFFL), k -> {
            int size = 2 * r + 1;
            NativeImage img = new NativeImage(size, size, true);
            for (int dy = -r; dy <= r; dy++) {
                int ho = half(r, dy);
                int hi = in >= 0 && Math.abs(dy) < in ? half(in, dy) : -1;
                for (int dx = -ho; dx <= ho; dx++)
                    if (hi < 0 || Math.abs(dx) > hi)
                        img.setPixelRGBA(dx + r, dy + r, 0xFFFFFFFF);
            }
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation id = Feedback.id("skill_shape/" + r + "_" + in);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static void stamp(GuiGraphics g, int r, int in, int color) {
        g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
        int size = 2 * r + 1;
        g.blit(shape(r, in), -r, -r, size, size, 0, 0, size, size, size, size);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** A filled disc about the origin. */
    private static void disc(GuiGraphics g, int r, int color) {
        stamp(g, r, -1, color);
    }

    /** A ring about the origin, {@code thick} wide, its outer edge at {@code r}. */
    private static void ring(GuiGraphics g, int r, int thick, int color) {
        stamp(g, r, r - thick, color);
    }

    /** Slow aurora bands, drifting dust at three depths that shifts with the camera, and a glow under the cursor. */
    private void ambience(GuiGraphics g, int vy, int vh, long now, int mouseX, int mouseY) {
        float t = now / 1000f;
        int acc = accent();
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
        int c = lit ? accent() : argb(0xA0, SKIN.dim());
        for (int i = 0; i <= steps; i++) {
            double[] p = bezier(ax, ay, bx, by, (double) i / steps);
            int x = (int) Math.round(toScreenX(p[0])) - h, y = (int) Math.round(toScreenY(p[1])) - h;
            if (lit)
                g.fill(x - 1, y - 1, x + th + 1, y + th + 1, argb(feeds ? 0x38 : 0x28, accent()));
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
                g.fill(x - 3, y - 3, x + 4, y + 4, argb(0x40, accent()));
            g.fill(x - s + 1, y - s + 1, x + s, y + s, argb(255 - tr * 34, tr == 0 ? 0xFFFFFF : mix(accent(), 0xFFFFFF, 0.5f)));
        }
    }

    private int icon(String id) {
        return iconSide.computeIfAbsent(id, k -> {
            var res = Minecraft.getInstance().getResourceManager().getResource(Feedback.id("textures/gui/skill/" + k + ".png"));
            if (res.isEmpty())
                return 0;
            try (var in = res.get().open(); var img = NativeImage.read(in)) {
                return img.getWidth();
            } catch (IOException e) {
                return 0;
            }
        });
    }

    /** One orb at its canvas position: halo, ring, icon, state badge and its name beneath. */
    private void orb(GuiGraphics g, SkillTree t, SkillTree.Node n, float[] p, State st, boolean hot, float beat, long now) {
        double sx = toScreenX(p[0]), sy = toScreenY(p[1]);
        if (sx < -60 * zoom || sx > width + 60 * zoom || sy < STRIP_H - 60 * zoom || sy > height - INFO_H + 60 * zoom)
            return;
        boolean sel = n.id().equals(picked);
        int acc = accent(), warn = SKIN.warn();
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
        int side = icon(n.id());
        if (side > 0) {
            g.setColor(1f, 1f, 1f, st == State.LOCKED ? 0.35f : 1f);
            g.blit(Feedback.id("textures/gui/skill/" + n.id() + ".png"), -ICON / 2, -ICON / 2, ICON, ICON, 0, 0, side, side, side, side);
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
            // The hue goes down first: the body, panels, text and widgets all draw over it.
            g.fill(0, 0, width, height, argb(34, tint(Util.getMillis())));
            renderBody(g, mouseX, mouseY, partialTick);
        });
    }

    private void renderBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SkillTree t = tree();
        long now = Util.getMillis();
        int vy = STRIP_H, vh = viewH();
        if (t == null && !equip) {
            g.fill(0, 0, width, height, 0x7005070C);
            g.drawCenteredString(font, Component.translatable("screen.feedback.skills.empty"), width / 2, height / 2 - 10, SKIN.dim());
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        float beat = 0.5f + 0.5f * Mth.sin(now / 420f);
        SkillPools pl = pools();
        g.enableScissor(0, vy, width, vy + vh);
        g.fill(0, vy, width, vy + vh, 0x7005070C); // translucent: the world shows through
        ambience(g, vy, vh, now, mouseX, mouseY);
        if (equip)
            loadout(g, mouseX, mouseY, beat);
        else {
            Map<String, float[]> at = layout(t);
            if (needFit)
                fit(at);
            clampCamera(at);
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
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, argb(al / 3, accent()));
                g.fill(sx, sy, sx + 1, sy + 1, argb(al, 0xFFFFFF));
            }
        }
        vignette(g, vy, vh);
        g.disableScissor();

        // The top strip and bottom plate are the instrument panel, their edge screws pushed off the window.
        SKIN.panel(g, -4, -4, width + 8, STRIP_H + 4);
        pop(); // the slot buttons rise from behind the plate: drawn first, so it covers them
        for (var b : popButtons)
            if (b.visible)
                b.render(g, mouseX, mouseY, partialTick);
        SKIN.panel(g, -4, height - INFO_H, width + 8, INFO_H + 4);
        title(g, now);
        if (equip) {
            equipInfo(g);
            description(g, picked);
        } else {
            g.drawString(font, Component.translatable("screen.feedback.skills.pool", pl.points(t.pool())), 12, 8, accent(), false);
            info(g, t);
            var n = t.node(picked == null ? "" : picked).orElse(null);
            description(g, n == null ? null : n.effect().unlock().orElse(null));
        }
        super.render(g, mouseX, mouseY, partialTick);
        tips(g, tree(), mouseX, mouseY);
    }

    /** Hover tips: an orb gives its effect line and state; the plate's text gives the full description. */
    private void tips(GuiGraphics g, SkillTree t, int mouseX, int mouseY) {
        if (!opening.settled())
            return;
        int dy = height - INFO_H + 4;
        if (mouseY >= dy && mouseX >= DESC_X && mouseX < width - DESC_RIGHT && !descFull.getString().isEmpty()) {
            g.renderTooltip(font, font.split(descFull, 220), mouseX, mouseY);
            return;
        }
        if (mouseY < STRIP_H || mouseY >= height - INFO_H)
            return;
        if (equip) {
            String hit = equipAt(mouseX, mouseY);
            if (hit == null)
                return;
            String move = hit.startsWith("#") ? CastState.of(minecraft.player).slots().get(Integer.parseInt(hit.substring(1))) : hit;
            g.renderTooltip(font, font.split(move.isEmpty() ? Component.translatable("screen.feedback.skills.equip.tip.key")
                    : Component.translatableWithFallback("skill.feedback.move." + move + ".desc", moveName(move)), 220), mouseX, mouseY);
        } else if (t != null) {
            String hit = nodeAt(t, layout(t), mouseX, mouseY);
            var n = hit == null ? null : t.node(hit).orElse(null);
            if (n != null)
                g.renderTooltip(font, List.of(effectText(n.effect()).getVisualOrderText(),
                        statusText(t, n).copy().withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText()), mouseX, mouseY);
        }
    }

    /** The loadout's orbs: the keys lit when they hold a technique, each technique lit while it sits in a key. */
    private void loadout(GuiGraphics g, int mouseX, int mouseY, float beat) {
        var slots = CastState.of(minecraft.player).slots();
        String over = opening.settled() ? equipAt(mouseX, mouseY) : null;
        for (var e : equipSpots().entrySet()) {
            String k = e.getKey();
            int[] p = e.getValue();
            if (k.startsWith("#")) {
                int i = Integer.parseInt(k.substring(1));
                String move = slots.get(i);
                Component under = Component.literal(move.isEmpty() ? Component.translatable("screen.feedback.skills.equip.empty").getString() : moveName(move));
                castOrb(g, p[0], p[1], R + 4, move, CombatKeys.castLabel(i), under, !move.isEmpty(),
                        !move.isEmpty() && move.equals(picked), k.equals(over), beat);
            } else
                castOrb(g, p[0], p[1], R, k, Component.literal(moveName(k)), null, slots.contains(k), k.equals(picked), k.equals(over), beat);
        }
    }

    /** One loadout orb in screen pixels, built like a tree node: halo, ring, the technique's icon, and its labels beneath. */
    private void castOrb(GuiGraphics g, int x, int y, int r, String move, Component name, Component under, boolean lit, boolean sel, boolean hot, float beat) {
        int tone = lit ? accent() : SKIN.dim();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float sc = hot ? 1.1f : 1f;
        g.pose().scale(sc, sc, 1);
        if (lit || hot || sel) {
            float pulse = lit ? 0.4f + 0.6f * beat : 0.3f;
            for (int k = 8; k >= 2; k -= 2)
                disc(g, r + k, argb((int) ((hot || sel ? 24 : 14) * pulse * (10 - k) / 4f), tone));
        }
        disc(g, r, 0xFF05070C);
        disc(g, r - 2, lit ? argb(0xFF, mix(0x0A0C10, accent(), 0.22f)) : 0xFF10151C);
        ring(g, r, 2, 0xFF000000 | tone);
        if (sel)
            ring(g, r + 4, 1, 0xFF000000 | SKIN.ink());
        String tex = move.isEmpty() ? "" : iconFor(move);
        int side = tex.isEmpty() ? 0 : icon(tex);
        if (side > 0)
            g.blit(Feedback.id("textures/gui/skill/" + tex + ".png"), -ICON / 2, -ICON / 2, ICON, ICON, 0, 0, side, side, side, side);
        else if (!move.isEmpty()) {
            String ch = moveName(move).substring(0, 1).toUpperCase(Locale.ROOT);
            g.drawString(font, ch, -font.width(ch) / 2, -4, SKIN.ink(), false);
        }
        String nm = font.plainSubstrByWidth(name.getString(), EQUIP_CELL - 6);
        g.drawString(font, nm, -font.width(nm) / 2, r + 5, under != null ? accent() : lit ? SKIN.ink() : SKIN.dim(), true);
        if (under != null) {
            String u = font.plainSubstrByWidth(under.getString(), 96);
            g.drawString(font, u, -font.width(u) / 2, r + 15, lit ? SKIN.ink() : SKIN.dim(), false);
        }
        g.pose().popPose();
    }

    /** The texture for a technique: the icon of the tree node that unlocks it, else one named for the move. */
    private String iconFor(String move) {
        return castIcon.computeIfAbsent(move, m -> {
            for (SkillTree t : trees())
                for (SkillTree.Node n : t.nodes())
                    if (n.effect().unlock().filter(m::equals).isPresent() && icon(n.id()) > 0)
                        return n.id();
            return m;
        });
    }

    /** The plate for the loadout: the picked technique's name, the gear granting it, its stamina and the key it sits in. */
    private void equipInfo(GuiGraphics g) {
        int y = height - INFO_H + 9, x = 16, cw = COLUMN_W;
        var c = picked == null ? null : Casts.find(picked);
        if (c == null) {
            g.drawString(font, Component.translatable("screen.feedback.skills.equip.pick"), x, y, SKIN.dim(), false);
            return;
        }
        g.drawString(font, font.plainSubstrByWidth(moveName(c.id()), cw), x, y, SKIN.ink(), false);
        var gear = CombatGear.granting(minecraft.player, Feedback.id(c.id()));
        if (!gear.isEmpty())
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("screen.feedback.skills.equip.gear", gear.getHoverName()).getString(), cw),
                    x, y + 10, SKIN.dim(), false);
        g.drawString(font, Component.translatable("screen.feedback.skills.equip.stamina", Math.round(c.cost())), x, y + 20, SKIN.dim(), false);
        int in = CastState.of(minecraft.player).slots().indexOf(c.id());
        g.drawString(font, in < 0 ? Component.translatable("screen.feedback.skills.equip.free")
                : Component.translatable("screen.feedback.skills.equip.in", CombatKeys.castLabel(in)),
                x, y + 30, in < 0 ? SKIN.dim() : accent(), false);
    }

    private void title(GuiGraphics g, long now) {
        String ts = this.title.getString().toUpperCase(Locale.ROOT);
        int tw = font.width(ts), cx = 0, tx = (width - tw) / 2;
        float sweep = now % 3600 / 3600f * (tw + 90) - 45;
        for (int i = 0; i < ts.length(); i++) {
            String ch = ts.substring(i, i + 1);
            float k = Math.max(0f, 1f - Math.abs(cx - sweep) / 20f);
            if (k > 0.2f)
                g.drawString(font, ch, tx + cx, 8, argb((int) (k * 0x50), accent()), false);
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
        int cw = COLUMN_W;
        g.drawString(font, font.plainSubstrByWidth(nodeName(t, n.id()).getString(), cw), x, y, SKIN.ink(), false);
        g.drawString(font, font.plainSubstrByWidth(effectText(n.effect()).getString(), cw), x, y + 10, SKIN.ink(), false);
        g.drawString(font, Component.translatable("screen.feedback.skills.cost", n.cost()), x, y + 20, SKIN.dim(), false);
        g.drawString(font, statusText(t, n), x, y + 30, SKIN.warn(), false);
        n.needsTrait().ifPresent(tr -> g.drawString(font, font.plainSubstrByWidth(Component.translatable("screen.feedback.skills.needs_trait",
                Component.translatableWithFallback("trait.feedback." + tr, tr)).getString(), cw), x, y + 40, SKIN.dim(), false));
    }

    /** The plate's right side: the description's first sentence, no box, no scroll; the whole text is the hover tip. */
    private void description(GuiGraphics g, String move) {
        Component d = move == null ? Component.empty()
                : Component.translatableWithFallback("skill.feedback.move." + move + ".desc", "");
        descFull = d;
        String full = d.getString();
        if (full.isEmpty())
            return;
        int cut = full.indexOf(". ");
        Component first = cut < 0 ? d : Component.literal(full.substring(0, cut + 1));
        int x = DESC_X, w = Math.max(120, width - DESC_RIGHT - x), y = height - INFO_H + 9;
        for (var line : font.split(first, w - 8).stream().limit(4).toList()) {
            g.drawString(font, line, x, y, SKIN.dim(), false);
            y += 10;
        }
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
        if (!dragging || equip)
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
            String hit = dragMoved ? null : equip ? equipAt(mouseX, mouseY) : t == null ? null : nodeAt(t, layout(t), mouseX, mouseY);
            dragMoved = false;
            if (hit != null && hit.startsWith("#")) // a key picks the technique in it
                hit = CastState.of(minecraft.player).slots().get(Integer.parseInt(hit.substring(1)));
            if (hit != null && !hit.isEmpty()) {
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
        if (tree() == null && !equip)
            return false;
        if (equip || !inView(mouseX, mouseY))
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
        String move = pickedMove();
        if (move != null && opening.settled())
            for (int i = 0; i < CombatKeys.CAST.length; i++)
                if (CombatKeys.CAST[i].matches(key, scan)) {
                    PacketDistributor.sendToServer(new CastPayloads.Slot(i, move));
                    SKIN.cue(GuiSkin.Cue.SEAT);
                    return true;
                }
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    /** The castable technique the player has picked and owns, which a cast key pressed here slots; else null. */
    private String pickedMove() {
        if (picked == null)
            return null;
        if (equip)
            return Casts.find(picked) != null && Skills.owned(minecraft.player, picked) ? picked : null;
        SkillTree t = tree();
        var n = t == null ? null : t.node(picked).orElse(null);
        if (n == null || !pools().has(t.tree(), n.id()))
            return null;
        return n.effect().unlock().filter(m -> Casts.find(m) != null).orElse(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
