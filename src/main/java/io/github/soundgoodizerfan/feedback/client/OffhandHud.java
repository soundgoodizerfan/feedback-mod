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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The off hand's own attack indicator, a bar above the crosshair (vanilla's, below it, is the main hand's).
 * Drawn only while the off hand recharges after a swing.
 */
final class OffhandHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int BAR_W = 20, BAR_H = 3, RISE = 14;

    private static float charge;
    private static final Gauge GAUGE = Gauge.of(() -> charge);

    private OffhandHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("offhand_charge"), OffhandHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson())
            return;
        charge = UpswingSwing.offhandCharge(mc.player, delta.getGameTimeDeltaPartialTick(false));
        if (charge < 1f)
            GAUGE.render(g, SKIN, g.guiWidth() / 2 - BAR_W / 2, g.guiHeight() / 2 - RISE - BAR_H, BAR_W, BAR_H);
    }
}
