// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillPools;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees;
import io.github.soundgoodizerfan.feedback.combat.skill.TreeHue;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.joml.Vector3f;

/**
 * How a skill point gain shows: a small tree-coloured bar above the hotbar that the points fill on each gain and that fades a
 * few seconds after the last one, nothing in the world per hit. The gain is read off the synced {@link SkillPools}, so no
 * packet is needed for it. A kill that paid sends one wisp ({@code SkillWispPayload}) that drifts from the fallen to the
 * player; {@link ClientSettings#gainWisp()} turns it off live.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SkillGainHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int SHOW_TICKS = 70, WISP_TICKS = 14, BAR_W = 80, BAR_H = 5;

    /** The last points seen per tree, to tell a gain from a purchase (points falling). */
    private static final Map<String, Integer> SEEN = new HashMap<>();
    /** The tree on show, its gain stacked since the bar came up, and the tick it last grew. */
    private static String tree;
    private static int stacked, shownTicks;
    private static float shownFill;

    private record Wisp(Vec3 from, int rgb, int[] age) {
    }

    private static final List<Wisp> WISPS = new ArrayList<>();

    private SkillGainHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("skill_gain"), SkillGainHud::render);
    }

    /** A kill that paid {@code rgb}'s tree: one wisp from where the creature fell. */
    public static void wisp(int rgb, double x, double y, double z) {
        if (ClientSettings.gainWisp() && !ClientSettings.reduceMotion() && WISPS.size() < 8)
            WISPS.add(new Wisp(new Vec3(x, y, z), rgb, new int[1]));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            SEEN.clear();
            WISPS.clear();
            tree = null;
            return;
        }
        SkillPools pools = SkillPools.of(mc.player);
        for (Map.Entry<String, Integer> e : pools.points().entrySet()) {
            int before = SEEN.getOrDefault(e.getKey(), e.getValue()), now = e.getValue();
            SEEN.put(e.getKey(), now);
            if (now > before) {
                if (!e.getKey().equals(tree))
                    stacked = 0;
                tree = e.getKey();
                stacked += now - before;
                shownTicks = SHOW_TICKS;
            }
        }
        if (shownTicks > 0 && --shownTicks == 0)
            tree = null;
        tickWisps(mc);
    }

    private static void tickWisps(Minecraft mc) {
        var it = WISPS.iterator();
        while (it.hasNext()) {
            Wisp w = it.next();
            if (++w.age()[0] > WISP_TICKS) {
                it.remove();
                continue;
            }
            double t = w.age()[0] / (double) WISP_TICKS;
            t = t * t * (3 - 2 * t); // eased: lingers, then drifts in
            Vec3 to = mc.player.position().add(0, mc.player.getBbHeight() * 0.6, 0);
            Vec3 at = w.from().lerp(to, t).add(0, Math.sin(t * Math.PI) * 0.4, 0);
            int rgb = w.rgb();
            var colour = new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
            mc.level.addParticle(new DustParticleOptions(colour, 0.6f), at.x, at.y, at.z, 0, 0, 0);
        }
    }

    /** Points of {@code pool} over the price of the cheapest node of that tree not yet owned, 0..1; 1 when nothing is left to buy. */
    private static float progress(SkillPools pools, String pool) {
        int cheapest = Integer.MAX_VALUE;
        for (SkillTree t : SkillTrees.get().trees())
            if (t.pool().equals(pool))
                for (SkillTree.Node n : t.nodes())
                    if (!pools.has(t.tree(), n.id()))
                        cheapest = Math.min(cheapest, n.cost());
        return cheapest == Integer.MAX_VALUE || cheapest <= 0 ? 1f : Math.min(1f, pools.points(pool) / (float) cheapest);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (tree == null || mc.player == null || mc.options.hideGui || mc.screen != null)
            return;
        SkillPools pools = SkillPools.of(mc.player);
        float target = progress(pools, tree);
        shownFill += (target - shownFill) * 0.2f; // the bar glides up to its value
        final int rgb = TreeHue.of(tree);
        var font = mc.font;
        Component name = Component.translatableWithFallback("skill.feedback.tree." + tree, tree);
        String gain = "+" + stacked + "  " + pools.points(tree);
        int w = BAR_W + 12, x0 = g.guiWidth() / 2 - w / 2, y1 = g.guiHeight() - 64, y0 = y1 - 28;
        GuiStyle.hudCard(g, x0, y0, x0 + w, y1);
        g.drawString(font, name, x0 + 6, y0 + 6, 0xFF000000 | rgb, false);
        g.drawString(font, gain, x0 + w - 6 - font.width(gain), y0 + 6, SKIN.ink(), false);
        Gauge.of(() -> shownFill).lead(() -> 0xFF000000 | rgb).render(g, SKIN, x0 + 6, y1 - 6 - BAR_H, BAR_W, BAR_H);
    }
}
