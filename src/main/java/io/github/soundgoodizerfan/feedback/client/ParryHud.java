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
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * A bar under the crosshair while a parry is committed: green while the window is open (anything landing
 * now is turned), red while the recovery leaves the fighter open. Reads the synced {@link CombatState};
 * nothing is shown when idle, so it only costs attention when a press has happened.
 */
final class ParryHud {

    private static final int BAR_W = 40, BAR_H = 3, DROP = 14;
    private static final int OPEN = 0xFF5CE65C, SHUT = 0xFFE05A4A, BACK = 0x80000000;

    /** The longest the current phase has been seen, so the bar drains from full. */
    private static CombatPhase seen = CombatPhase.IDLE;
    private static int peak = 1;

    private ParryHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("parry_state"), ParryHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;
        CombatState s = CombatState.of(mc.player);
        if (s.phase() != CombatPhase.PARRY_ACTIVE && s.phase() != CombatPhase.PARRY_RECOVERY) {
            seen = CombatPhase.IDLE;
            return;
        }
        if (s.phase() != seen) {
            seen = s.phase();
            peak = Math.max(1, s.ticksLeft());
        }
        int x = g.guiWidth() / 2 - BAR_W / 2, y = g.guiHeight() / 2 + DROP;
        int fill = Math.round(BAR_W * Math.min(1f, s.ticksLeft() / (float) peak));
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, BACK);
        g.fill(x, y, x + fill, y + BAR_H, seen == CombatPhase.PARRY_ACTIVE ? OPEN : SHUT);
    }
}
