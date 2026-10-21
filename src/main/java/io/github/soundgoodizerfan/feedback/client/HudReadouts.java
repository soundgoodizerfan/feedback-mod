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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Short messages from the server -- a trait's stack count, a forging result -- drawn as HUD cards
 * where the ammunition readout sits, instead of the vanilla action bar. At most three live at once,
 * newest nearest the anchor with older ones stacked above; a gun in hand puts the stack above its
 * ammo card so they never overlap. A line already showing with the same text is refreshed, not
 * repeated, so a count that updates every hit holds one card. Each fades over its last half second;
 * nothing moves, so a readout never jitters.
 */
public final class HudReadouts {

    private static final int MAX = 3, GAP = 3, HEIGHT = 14, FADE_MS = 500;
    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private static final List<Entry> LIVE = new ArrayList<>();

    private record Entry(Component text, long born, long until, int priority) {
    }

    private HudReadouts() {
    }

    /** Server side: show {@code text} on {@code player}'s HUD for {@code ticks}. */
    public static void send(ServerPlayer player, Component text, int ticks) {
        PacketDistributor.sendToPlayer(player, new HudReadoutPayload(text, ticks, 0));
    }

    /** Client side: take a readout in. */
    public static void add(Component text, int ticks, int priority) {
        long now = Util.getMillis();
        LIVE.removeIf(e -> e.until() <= now || e.text().getString().equals(text.getString()));
        LIVE.add(new Entry(text, now, now + ticks * 50L, priority));
        while (LIVE.size() > MAX) // the lowest priority goes, the oldest of those first
            LIVE.remove(LIVE.stream().min(Comparator.comparingInt(Entry::priority).thenComparingLong(Entry::born)).get());
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("hud_readouts"), HudReadouts::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        long now = Util.getMillis();
        LIVE.removeIf(e -> e.until() <= now);
        if (LIVE.isEmpty() || mc.player == null || mc.options.hideGui)
            return;
        // GunHud's card spans y-6..y+64 with y = height - 80: the stack's bottom is its top edge when a gun is held.
        boolean gun = mc.player.getMainHandItem().getItem() instanceof GunItem
                && io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(mc.player.getMainHandItem()) != null;
        int right = g.guiWidth() - 12;
        int bottom = g.guiHeight() - (gun ? 86 + GAP : 16);
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            Entry e = LIVE.get(i);
            float a = Mth.clamp((e.until() - now) / (float) FADE_MS, 0f, 1f);
            int alpha = Math.round(a * 255);
            if (alpha < 8)
                continue;
            int w = mc.font.width(e.text());
            int y1 = bottom, y0 = y1 - HEIGHT, x0 = right - w - 10, x1 = right + 6;
            GuiStyle.hudCard(g, x0, y0, x1, y1, a);
            g.drawString(mc.font, e.text(), right - w, y0 + 3, alpha << 24 | SKIN.ink() & 0xFFFFFF, false);
            bottom = y0 - GAP;
        }
    }
}
