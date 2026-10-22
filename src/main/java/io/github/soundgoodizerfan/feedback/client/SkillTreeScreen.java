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

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The combat skill screen, opened with K: a tab per tree, the tree's pool, its nodes in columns by how
 * far down the requires chain they sit with a line to each requirement, and the picked node's effect,
 * cost and buy button. An instrument panel ({@link GuiSkin#PANEL}), the same pattern as
 * {@link MirrorScreen}. The server decides every purchase; this only asks.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    public static final KeyMapping KEY = new KeyMapping("key.feedback.skills", InputConstants.KEY_K, "key.categories.feedback");

    private static final int W = 360, H = 200, NODE_W = 76, NODE_H = 16, COL = 88, ROW = 22;
    private static final GuiSkin SKIN = GuiSkin.PANEL;

    private final Opening opening = new Opening(SKIN);
    private int tab;
    private String picked;
    private SkillPools shown = SkillPools.EMPTY;

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

    /** Where each node's button sits, relative to the screen's top-left. */
    private Map<String, int[]> layout(SkillTree t) {
        Map<String, Integer> memo = new HashMap<>();
        Map<Integer, Integer> rows = new HashMap<>();
        Map<String, int[]> at = new HashMap<>();
        for (SkillTree.Node n : t.nodes()) {
            int d = depth(t, n.id(), memo), row = rows.merge(d, 1, Integer::sum) - 1;
            at.put(n.id(), new int[]{12 + d * COL, 62 + row * ROW});
        }
        return at;
    }

    private boolean available(SkillTree t, SkillTree.Node n) {
        SkillPools p = pools();
        return !p.has(t.tree(), n.id()) && n.requires().stream().allMatch(r -> p.has(t.tree(), r));
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
            rebuildWidgets();
        }));
        Map<String, int[]> at = layout(t);
        for (SkillTree.Node n : t.nodes()) {
            int[] p = at.get(n.id());
            addRenderableWidget(GuiStyle.button(SKIN, nodeName(t, n.id()), b -> {
                picked = n.id();
                rebuildWidgets();
            }, x + p[0], y + p[1], NODE_W, NODE_H));
        }
        t.node(picked == null ? "" : picked).ifPresent(n -> {
            boolean can = available(t, n) && pools().points(t.tree()) >= n.cost();
            var buy = GuiStyle.primary(SKIN, Component.translatable("screen.feedback.skills.buy", n.cost()),
                    b -> PacketDistributor.sendToServer(new SkillBuyPayload(t.tree(), n.id())),
                    x + W - 12 - 90, y + H - 38, 90, 14, null);
            buy.active = can;
            addRenderableWidget(buy);
        });
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                x + W - 12 - 60, y + H - 20, 60, 14));
    }

    @Override
    public void tick() {
        SkillPools now = pools();
        if (!now.equals(shown))
            rebuildWidgets();
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
            g.drawString(font, Component.translatable("screen.feedback.skills.pool", pools().points(t.tree())),
                    x + 12, y + 44, SKIN.accent(), false);
            Map<String, int[]> at = layout(t);
            for (SkillTree.Node n : t.nodes())
                for (String r : n.requires()) {
                    int[] a = at.get(r), b = at.get(n.id());
                    if (a != null && b != null)
                        GuiStyle.line(g, x + a[0] + NODE_W, y + a[1] + NODE_H / 2, x + b[0], y + b[1] + NODE_H / 2,
                                pools().has(t.tree(), r) ? SKIN.accent() : SKIN.dim());
                }
            super.render(g, mouseX, mouseY, partialTick);
            for (SkillTree.Node n : t.nodes()) {
                int[] p = at.get(n.id());
                boolean owned = pools().has(t.tree(), n.id());
                if (owned || n.id().equals(picked))
                    g.fill(x + p[0], y + p[1] + NODE_H + 1, x + p[0] + NODE_W, y + p[1] + NODE_H + 2,
                            owned ? SKIN.accent() : SKIN.dim());
            }
            int ty = y + H - 54;
            g.fill(x + 12, ty - 4, x + W - 12, ty - 3, 0x40D8DCD8);
            var n = t.node(picked == null ? "" : picked).orElse(null);
            if (n == null) {
                g.drawString(font, Component.translatable("screen.feedback.skills.pick"), x + 12, ty, SKIN.dim(), false);
                return;
            }
            g.drawString(font, nodeName(t, n.id()), x + 12, ty, SKIN.ink(), false);
            g.drawString(font, effectText(n.effect()), x + 12, ty + 11, SKIN.ink(), false);
            g.drawString(font, Component.translatable("screen.feedback.skills.cost", n.cost()), x + 12, ty + 22, SKIN.dim(), false);
            Component status;
            if (pools().has(t.tree(), n.id()))
                status = Component.translatable("screen.feedback.skills.owned");
            else if (!available(t, n)) {
                List<Component> need = new ArrayList<>();
                for (String r : n.requires())
                    if (!pools().has(t.tree(), r))
                        need.add(nodeName(t, r));
                status = Component.translatable("screen.feedback.skills.locked", net.minecraft.network.chat.ComponentUtils.formatList(need, Component.literal(", ")));
            } else if (pools().points(t.tree()) < n.cost())
                status = Component.translatable("screen.feedback.skills.short");
            else
                status = Component.empty();
            g.drawString(font, status, x + 12, ty + 33, SKIN.warn(), false);
        });
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
        return !opening.settled() || super.mouseClicked(mouseX, mouseY, button);
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
