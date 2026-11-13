// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.hud;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.attunement.ChargeKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.ReservoirKinds;
import io.github.soundgoodizerfan.feedback.client.gui.Gauge;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Gauges in the bottom-right corner, through {@link Gauge}, for what the held tool has stored: Charge (the tick marks
 * where the safe amount ends, past it every blow arcs) and Reservoir experience (in the skin's accent). Nothing is
 * drawn for a tool that stores neither.
 */
public final class StoredHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int BAR_W = 90, BAR_H = 8, GAP = 14, MARGIN = 12;

    private static float charge, xp;
    private static final Gauge CHARGE = Gauge.of(() -> charge).mark(() -> 0.5);
    private static final Gauge XP = Gauge.of(() -> xp).lead(SKIN::accent);

    private StoredHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("stored_state"), StoredHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;
        ItemStack held = mc.player.getMainHandItem();
        charge = ChargeKinds.fill(held, mc.player);
        xp = ReservoirKinds.fill(held);
        int x = g.guiWidth() - BAR_W - MARGIN, y = g.guiHeight() - MARGIN - BAR_H;
        if (charge >= 0) {
            CHARGE.render(g, SKIN, x, y, BAR_W, BAR_H);
            y -= GAP;
        }
        if (xp >= 0)
            XP.render(g, SKIN, x, y, BAR_W, BAR_H);
    }
}
