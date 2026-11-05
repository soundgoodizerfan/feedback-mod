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

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The off hand's own attack indicator, a bar above the crosshair drawn with vanilla's attack indicator sprites (the main hand's, below it, is vanilla's own, or ours when barehanded).
 * Drawn only while the off hand recharges after a swing.
 */
final class OffhandHud {

    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_background");
    private static final ResourceLocation PROGRESS = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
    private static final int BAR_W = 16, BAR_H = 4, RISE = 8;

    private OffhandHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("offhand_charge"), OffhandHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson())
            return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        bar(g, UpswingSwing.offhandCharge(mc.player, partial), g.guiHeight() / 2 - RISE - BAR_H);
        // Barehanded the main hand is tracked here too, so vanilla's own indicator (below the crosshair) has nothing to show.
        bar(g, UpswingSwing.mainCharge(mc.player, partial), g.guiHeight() / 2 + RISE + 1);
    }

    private static void bar(GuiGraphics g, float charge, int y) {
        if (charge >= 1f)
            return;
        int x = g.guiWidth() / 2 - BAR_W / 2;
        g.blitSprite(BACKGROUND, x, y, BAR_W, BAR_H);
        g.blitSprite(PROGRESS, BAR_W, BAR_H, 0, 0, x, y, (int) (charge * 17f), BAR_H);
    }
}
