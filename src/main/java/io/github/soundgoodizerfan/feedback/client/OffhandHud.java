// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
