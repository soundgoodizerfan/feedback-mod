// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int BAR_W = 40, BAR_H = 3, DROP = 14;

    /** The longest the current phase has been seen, so the bar drains from full. */
    private static CombatPhase seen = CombatPhase.IDLE;
    private static int peak = 1, left;
    private static final Gauge GAUGE = Gauge.of(() -> Math.min(1f, left / (double) peak))
            .lead(() -> seen == CombatPhase.PARRY_ACTIVE ? SKIN.accent() : SKIN.warn());

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
        left = s.ticksLeft();
        GAUGE.render(g, SKIN, g.guiWidth() / 2 - BAR_W / 2, g.guiHeight() / 2 + DROP, BAR_W, BAR_H);
    }
}
