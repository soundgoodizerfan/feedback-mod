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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * A pulse of brackets around the crosshair the moment a roll's cooldown ends, for play with the sound off
 * ({@code CombatCues.ready} is only a tick). Reads the synced {@link CombatState}; nothing is drawn otherwise.
 */
final class RollReadyHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final long PULSE_MS = 350;
    private static boolean rolled;
    private static long pulseStart;

    private RollReadyHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("roll_ready"), RollReadyHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;
        CombatState s = CombatState.of(mc.player);
        if (s.phase() == CombatPhase.ROLL || s.phase() == CombatPhase.ROLL_RECOVERY)
            rolled = true;
        else if (rolled && s.ready()) {
            rolled = false;
            pulseStart = System.currentTimeMillis();
        }
        float t = (System.currentTimeMillis() - pulseStart) / (float) PULSE_MS;
        if (t >= 1f)
            return;
        // Four corner ticks that open outward as they fade.
        int gap = 6 + Math.round(t * 8), len = 5, cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int colour = (Mth.clamp(Math.round((1f - t) * 255), 0, 255) << 24) | (SKIN.accent() & 0xFFFFFF);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) {
                int x = cx + sx * gap, y = cy + sy * gap;
                g.fill(Math.min(x, x - sx * len), y, Math.max(x, x - sx * len) + 1, y + 1, colour);
                g.fill(x, Math.min(y, y - sy * len), x + 1, Math.max(y, y - sy * len) + 1, colour);
            }
    }
}
