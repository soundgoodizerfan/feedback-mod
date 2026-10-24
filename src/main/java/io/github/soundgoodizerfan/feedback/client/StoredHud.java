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
import io.github.soundgoodizerfan.feedback.modular.attunement.ChargeKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.ReservoirKinds;

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
final class StoredHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int BAR_W = 90, BAR_H = 8, GAP = 14, MARGIN = 12;

    private static float charge, xp;
    private static final Gauge CHARGE = Gauge.of(() -> charge).mark(() -> 0.5);
    private static final Gauge XP = Gauge.of(() -> xp).lead(SKIN::accent);

    private StoredHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
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
