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

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillPools;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

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
 * The combat skill screen, opened with K: a tab per tree, the tree's pool, its nodes in columns by how
 * far down the requires chain they sit with an elbow line to each requirement, in a viewport that pans
 * (drag, wheel, shift-wheel), a tooltip per node, and the picked node's effect, cost and buy button. A
 * node's state reads in its marker as well as colour: [*] learned, [+] ready, [!] too dear, [-] locked. An instrument panel ({@link GuiSkin#PANEL}), the same pattern as
 * {@link MirrorScreen}. The server decides every purchase; this only asks.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    public static final KeyMapping KEY = new KeyMapping("key.feedback.skills", InputConstants.KEY_K, "key.categories.feedback");

    private static final int W = 360, H = 250, NODE_W = 84, NODE_H = 16, COL = 96, ROW = 22, PAD = 4;
    private static final int VIEW_X = 12, VIEW_Y = 60, VIEW_W = W - 24, VIEW_H = 118;
    private static final GuiSkin SKIN = GuiSkin.PANEL;

    private enum State { OWNED, BUYABLE, SHORT, LOCKED }

    private final Opening opening = new Opening(SKIN);
    private int tab, panX, panY;
    private String picked;
    private SkillPools shown = SkillPools.EMPTY;
    private boolean dragging, dragMoved;
    private String flashNode;
    private long flashAt;

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

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private List<SkillTree> trees() {
        return SkillTrees.get().trees();
    }

    private SkillTree tree() {
        var t = trees();
        return t.isEmpty() ? null : t.get(Math.min(tab, t.size() - 1));
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

    /** Where each node sits, in the scrolling content's own coordinates. */
    private Map<String, int[]> layout(SkillTree t) {
        Map<String, Integer> memo = new HashMap<>();
        Map<Integer, Integer> rows = new HashMap<>();
        Map<String, int[]> at = new HashMap<>();
        for (SkillTree.Node n : t.nodes()) {
            int d = depth(t, n.id(), memo), row = rows.merge(d, 1, Integer::sum) - 1;
            at.put(n.id(), new int[]{PAD + d * COL, PAD + row * ROW});
        }
        return at;
    }

    private boolean available(SkillTree t, SkillTree.Node n) {
        SkillPools p = pools();
        return !p.has(t.tree(), n.id()) && n.requires().stream().allMatch(r -> p.has(t.tree(), r));
    }

    private State state(SkillTree t, SkillTree.Node n) {
        SkillPools p = pools();
        if (p.has(t.tree(), n.id()))
            return State.OWNED;
        if (!available(t, n))
            return State.LOCKED;
        return p.points(t.tree()) >= n.cost() ? State.BUYABLE : State.SHORT;
    }

    /** The marker that says the state without colour: learned, ready, too dear, out of reach. */
    private static String glyph(State s) {
        return switch (s) {
            case OWNED -> "[*]";
            case BUYABLE -> "[+]";
            case SHORT -> "[!]";
            case LOCKED -> "[-]";
        };
    }

    private int[] size(Map<String, int[]> at) {
        int w = 0, h = 0;
        for (int[] p : at.values()) {
            w = Math.max(w, p[0] + NODE_W + PAD);
            h = Math.max(h, p[1] + NODE_H + PAD);
        }
        return new int[]{w, h};
    }

    private void clampPan(Map<String, int[]> at) {
        int[] sz = size(at);
        panX = Mth.clamp(panX, 0, Math.max(0, sz[0] - VIEW_W));
        panY = Mth.clamp(panY, 0, Math.max(0, sz[1] - VIEW_H));
    }

    private String nodeAt(SkillTree t, Map<String, int[]> at, double mx, double my) {
        int vx = left() + VIEW_X, vy = top() + VIEW_Y;
        if (mx < vx || mx >= vx + VIEW_W || my < vy || my >= vy + VIEW_H)
            return null;
        for (SkillTree.Node n : t.nodes()) {
            int[] p = at.get(n.id());
            int nx = vx + p[0] - panX, ny = vy + p[1] - panY;
            if (mx >= nx && mx < nx + NODE_W && my >= ny && my < ny + NODE_H)
                return n.id();
        }
        return null;
    }

    @Override
    protected void init() {
        opening.start();
        shown = pools();
        int x = left(), y = top();
        SkillTree t = tree();
        if (t == null) {
            addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                    x + W / 2 - 30, y + H - 22, 60, 14));
            return;
        }
        List<Component> labels = new ArrayList<>();
        for (SkillTree each : trees())
            labels.add(treeName(each.tree()));
        addRenderableWidget(GuiStyle.tabs(SKIN, x + 12, y + 24, W - 24, 14, labels, () -> tab, i -> {
            tab = i;
            picked = null;
            panX = panY = 0;
            rebuildWidgets();
        }));
        t.node(picked == null ? "" : picked).ifPresent(n -> {
            boolean can = available(t, n) && pools().points(t.tree()) >= n.cost();
            var buy = GuiStyle.primary(SKIN, Component.translatable("screen.feedback.skills.buy", n.cost()),
                    b -> PacketDistributor.sendToServer(new SkillBuyPayload(t.tree(), n.id())),
                    x + W - 12 - 90, y + H - 38, 90, 14, FSounds.UI_STAMP);
            buy.active = can;
            addRenderableWidget(buy);
        });
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                x + W - 12 - 60, y + H - 20, 60, 14));
    }

    @Override
    public void tick() {
        SkillPools now = pools();
        if (!now.equals(shown)) {
            SkillTree t = tree();
            if (t != null && picked != null && now.has(t.tree(), picked) && !shown.has(t.tree(), picked)) {
                flashNode = picked;
                flashAt = net.minecraft.Util.getMillis();
                GuiSkin.play(FSounds.UI_PUNCH.get(), 1f);
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, H, () -> {
            SKIN.panel(g, x, y, W, H);
            Component title = Component.literal(this.title.getString().toUpperCase(Locale.ROOT));
            g.drawString(font, title, x + (W - font.width(title)) / 2, y + 10, SKIN.ink(), false);
            SkillTree t = tree();
            if (t == null) {
                g.drawCenteredString(font, Component.translatable("screen.feedback.skills.empty"), x + W / 2, y + 60, SKIN.dim());
                super.render(g, mouseX, mouseY, partialTick);
                return;
            }
            Map<String, int[]> at = layout(t);
            clampPan(at);
            int[] sz = size(at);
            int vx = x + VIEW_X, vy = y + VIEW_Y;
            g.drawString(font, Component.translatable("screen.feedback.skills.pool", pools().points(t.tree())),
                    x + 12, y + 44, SKIN.accent(), false);
            Component legend = Component.translatable("screen.feedback.skills.legend");
            g.drawString(font, legend, x + W - 12 - font.width(legend), y + 44, SKIN.dim(), false);
            SKIN.inset(g, vx - 1, vy - 1, VIEW_W + 2, VIEW_H + 2);
            g.enableScissor(vx, vy, vx + VIEW_W, vy + VIEW_H);
            for (SkillTree.Node n : t.nodes())
                for (String r : n.requires()) {
                    int[] a = at.get(r), b = at.get(n.id());
                    if (a == null || b == null)
                        continue;
                    int c = pools().has(t.tree(), r) ? SKIN.accent() : SKIN.dim();
                    int x0 = vx + a[0] - panX + NODE_W, y0 = vy + a[1] - panY + NODE_H / 2;
                    int x1 = vx + b[0] - panX, y1 = vy + b[1] - panY + NODE_H / 2, mid = x1 - (COL - NODE_W) / 2;
                    g.fill(x0, y0, mid + 1, y0 + 1, c);
                    g.fill(mid, Math.min(y0, y1), mid + 1, Math.max(y0, y1) + 1, c);
                    g.fill(mid, y1, x1, y1 + 1, c);
                }
            String over = opening.settled() ? nodeAt(t, at, mouseX, mouseY) : null;
            for (SkillTree.Node n : t.nodes()) {
                int[] p = at.get(n.id());
                int nx = vx + p[0] - panX, ny = vy + p[1] - panY;
                State st = state(t, n);
                SKIN.inset(g, nx, ny, NODE_W, NODE_H);
                if (st == State.OWNED)
                    g.fill(nx + 1, ny + 1, nx + NODE_W - 1, ny + NODE_H - 1, 0x30000000 | SKIN.accent() & 0xFFFFFF);
                SKIN.row(g, nx + 1, ny + 1, NODE_W - 2, NODE_H - 2, n.id().equals(picked), n.id().equals(over));
                if (st == State.BUYABLE)
                    g.renderOutline(nx, ny, NODE_W, NODE_H, SKIN.accent());
                else if (st == State.SHORT)
                    g.renderOutline(nx, ny, NODE_W, NODE_H, SKIN.warn());
                int ink = st == State.LOCKED ? SKIN.dim() : SKIN.ink();
                int gc = st == State.OWNED || st == State.BUYABLE ? SKIN.accent() : st == State.SHORT ? SKIN.warn() : SKIN.dim();
                String gl = glyph(st);
                g.drawString(font, gl, nx + 3, ny + 4, gc, false);
                int gw = font.width(gl) + 6;
                g.drawString(font, font.plainSubstrByWidth(nodeName(t, n.id()).getString(), NODE_W - gw - 2), nx + gw, ny + 4, ink, false);
                if (n.id().equals(flashNode)) {
                    float f = (net.minecraft.Util.getMillis() - flashAt) / 700f;
                    if (f < 1) {
                        int grow = Math.round(f * 5);
                        g.renderOutline(nx - grow, ny - grow, NODE_W + 2 * grow, NODE_H + 2 * grow,
                                (int) ((1 - f) * 0xFF) << 24 | SKIN.accent() & 0xFFFFFF);
                    }
                }
            }
            g.disableScissor();
            SKIN.scrollbar(g, vx + VIEW_W - 3, vy, VIEW_H, sz[1], VIEW_H, panY);
            if (sz[0] > VIEW_W)
                SKIN.bar(g, vx + 1, vy + VIEW_H + 3, VIEW_W - 2, 2, (float) (panX + VIEW_W) / sz[0], -1);
            super.render(g, mouseX, mouseY, partialTick);
            int ty = y + VIEW_Y + VIEW_H + 14;
            g.fill(x + 12, ty - 4, x + W - 12, ty - 3, 0x40D8DCD8);
            var n = t.node(picked == null ? "" : picked).orElse(null);
            if (n == null) {
                g.drawString(font, Component.translatable("screen.feedback.skills.pick"), x + 12, ty, SKIN.dim(), false);
            } else {
                g.drawString(font, nodeName(t, n.id()), x + 12, ty, SKIN.ink(), false);
                g.drawString(font, effectText(n.effect()), x + 12, ty + 11, SKIN.ink(), false);
                g.drawString(font, Component.translatable("screen.feedback.skills.cost", n.cost()), x + 12, ty + 22, SKIN.dim(), false);
                g.drawString(font, statusText(t, n), x + 12, ty + 33, SKIN.warn(), false);
            }
            if (over != null)
                t.node(over).ifPresent(h -> g.renderComponentTooltip(font, tooltip(t, h), mouseX, mouseY));
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
        for (String r : n.requires())
            if (!pools().has(t.tree(), r))
                need.add(nodeName(t, r));
        return ComponentUtils.formatList(need, Component.literal(", "));
    }

    private List<Component> tooltip(SkillTree t, SkillTree.Node n) {
        List<Component> tip = new ArrayList<>();
        tip.add(nodeName(t, n.id()));
        n.branch().ifPresent(b -> tip.add(Component.translatable("screen.feedback.skills.branch", b)));
        tip.add(effectText(n.effect()));
        n.effect().unlock().ifPresent(m -> {
            Component d = Component.translatableWithFallback("skill.feedback.move." + m + ".desc", "");
            if (!d.getString().isEmpty())
                for (var line : font.getSplitter().splitLines(d, 200, net.minecraft.network.chat.Style.EMPTY.withColor(0xAAAAAA)))
                    tip.add(Component.literal(line.getString()).withStyle(net.minecraft.ChatFormatting.GRAY));
        });
        tip.add(Component.translatable("screen.feedback.skills.cost", n.cost()));
        if (!n.requires().isEmpty()) {
            List<Component> need = new ArrayList<>();
            for (String r : n.requires())
                need.add(Component.literal((pools().has(t.tree(), r) ? "[*] " : "[-] ") + nodeName(t, r).getString()));
            tip.add(Component.translatable("screen.feedback.skills.requires", ComponentUtils.formatList(need, Component.literal(", "))));
        }
        tip.add(statusText(t, n));
        return tip;
    }

    /** The dimming is the opening's. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!opening.settled())
            return true;
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;
        int vx = left() + VIEW_X, vy = top() + VIEW_Y;
        dragging = button == 0 && mouseX >= vx && mouseX < vx + VIEW_W && mouseY >= vy && mouseY < vy + VIEW_H;
        dragMoved = false;
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging)
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        if (dragMoved || Math.abs(dx) + Math.abs(dy) > 1) {
            dragMoved = true;
            panX -= (int) Math.round(dx);
            panY -= (int) Math.round(dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            SkillTree t = tree();
            String hit = dragMoved || t == null ? null : nodeAt(t, layout(t), mouseX, mouseY);
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
        SkillTree t = tree();
        if (t == null)
            return false;
        int[] sz = size(layout(t));
        if (sz[1] > VIEW_H && !hasShiftDown())
            panY -= (int) Math.signum(scrollY) * ROW;
        else
            panX -= (int) Math.signum(scrollY != 0 ? scrollY : scrollX) * COL / 2;
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
