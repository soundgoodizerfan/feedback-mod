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
import io.github.soundgoodizerfan.feedback.combat.cast.CastState;
import io.github.soundgoodizerfan.feedback.combat.cast.Casts;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The stamina bar and the three cast slots, top right: each slot shows its key and technique, and dims when the
 * stamina in the bar cannot pay for it.
 */
final class CastHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int BAR_W = 96, BAR_H = 5, CHIP_H = 12;

    private static double shown = -1;

    private CastHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("cast_hud"), CastHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;
        CastState s = CastState.of(mc.player);
        boolean any = s.slots().stream().anyMatch(t -> !t.isEmpty());
        if (!any && s.stamina() >= io.github.soundgoodizerfan.feedback.combat.cast.Stamina.MAX)
            return;
        double target = s.stamina() / (double) io.github.soundgoodizerfan.feedback.combat.cast.Stamina.MAX;
        shown = shown < 0 ? target : shown + (target - shown) * 0.25; // the bar glides to its value
        int w = BAR_W + 12, h = CastState.SLOTS * (CHIP_H + 2) + BAR_H + 16;
        int x1 = g.guiWidth() - 6, x0 = x1 - w, y0 = 6, y1 = y0 + h;
        GuiStyle.hudCard(g, x0, y0, x1, y1);
        int x = x0 + 6;
        boolean low = shown < 0.25;
        Gauge.of(() -> shown).lead(() -> low ? SKIN.warn() : SKIN.accent()).render(g, SKIN, x, y1 - 6 - BAR_H, BAR_W, BAR_H);
        var font = mc.font;
        for (int i = 0; i < CastState.SLOTS; i++) {
            String tech = s.slots().get(i);
            var cast = Casts.find(tech);
            int cy = y0 + 6 + i * (CHIP_H + 2);
            int wait = i < s.waits().size() ? s.waits().get(i) : 0;
            boolean afford = cast != null && s.stamina() >= cast.cost();
            int ink = cast == null || wait != 0 ? SKIN.dim() : SKIN.ink();
            Component name = cast == null ? Component.translatable("gui.feedback.cast_empty")
                    : Component.translatableWithFallback("skill.feedback.move." + tech, tech);
            g.fill(x, cy, x + BAR_W, cy + CHIP_H, 0x33000000);
            g.fill(x, cy, x + 14, cy + CHIP_H, wait != 0 ? 0xFF777777 : 0xFF555555);
            g.drawCenteredString(font, CombatKeys.CAST[i].getTranslatedKeyMessage(), x + 7, cy + 2, 0xFFFFFFFF);
            if (cast == null) {
                g.drawString(font, name, x + 18, cy + 2, ink, false);
                continue;
            }
            if (wait != 0) {
                // The technique is spent: what it waits on, in place of its cost, over a darkened chip.
                g.fill(x + 14, cy, x + BAR_W, cy + CHIP_H, 0x55000000);
                Component why = wait == Casts.WRONG_GEAR ? Component.translatable("hud.feedback.cast.wait.gear") : wait >= 10 ? Component.literal(((wait - 10) * 5 + 19) / 20 + "s")
                        : Component.translatable("hud.feedback.cast.wait." + (wait == 1 ? "strike" : wait == 2 ? "parry" : "stagger"));
                g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(name, BAR_W - 24 - font.width(why))), x + 18, cy + 2, ink, false);
                g.drawString(font, why, x + BAR_W - 2 - font.width(why), cy + 2, SKIN.warn(), false);
            } else {
                g.drawString(font, name, x + 18, cy + 2, ink, false);
                String cost = cast.cost() > 0 ? Integer.toString((int) cast.cost()) : "";
                g.drawString(font, cost, x + BAR_W - 2 - font.width(cost), cy + 2, afford ? SKIN.ink() : SKIN.warn(), false);
            }
        }
    }
}
