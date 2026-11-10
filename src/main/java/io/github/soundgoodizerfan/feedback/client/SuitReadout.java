// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * How the suit's own gauges tell the wearer what it is doing. A suit with no gauge fitted reads in words (a lamp's state, not a figure); a
 * gauge fitting gives figures, rounded to a step the finer the clearer its glass (the sensing rule: numbers cost an instrument, and a better
 * instrument has a finer step). A debug helmet reads the truth tier, exact. Every suit screen formats through here, never by hand.
 */
final class SuitReadout {

    private SuitReadout() {
    }

    /** Rounding step of a figure as a share of its full scale, or 0 for words only. 0.65 glass is about 8%, none 12%. */
    static float step(SuitSyncPayload s) {
        if (truth())
            return -1f;
        return s.gauge() > 0f ? 0.12f * s.gauge() : 0f;
    }

    static boolean figures(SuitSyncPayload s) {
        return s.gauge() > 0f || truth();
    }

    private static boolean truth() {
        var p = Minecraft.getInstance().player;
        return p != null && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof io.github.soundgoodizerfan.feedback.item.DebugHelmetItem;
    }

    /** A 0..1 figure as the gauge would show it: rounded to its step, exact at the truth tier, unrounded when there are only words. */
    static float shown(float v, SuitSyncPayload s) {
        float step = step(s);
        return step > 0f ? Math.round(v / step) * step : v;
    }

    static Component charge(SuitSyncPayload s) {
        if (!s.battery())
            return Component.translatable("hud.feedback.suit.no_battery");
        if (figures(s))
            return Component.literal(Math.round(100f * Mth.clamp(shown(s.charge(), s), 0f, 1f)) + "%");
        float c = s.charge();
        return Component.translatable("hud.feedback.suit.charge." + (c <= 0.02f ? "empty" : c < 0.25f ? "low" : c < 0.6f ? "half" : c < 0.95f ? "good" : "full"));
    }

    /** 0..1 how far the heat has risen from ambient toward the cut-out. */
    static float heatShare(SuitSyncPayload s) {
        float amb = AmbientTable.at(Minecraft.getInstance().level).value();
        return Mth.clamp((s.heat() - amb) / Math.max(1f, s.limit() - amb), 0f, 1f);
    }

    static Component heat(SuitSyncPayload s) {
        if (s.cut())
            return Component.translatable("hud.feedback.suit.heat.cut");
        if (figures(s))
            return Component.literal(Math.round(truth() ? s.heat() : Math.round(s.heat() / 5f) * 5f) + " Tu");
        float h = heatShare(s);
        return Component.translatable("hud.feedback.suit.heat." + (h < 0.25f ? "cool" : h < 0.6f ? "warm" : h < 0.85f ? "hot" : "critical"));
    }

    /** A temperature seen through the thermal view: a figure when a gauge is fitted (to a step the gauge sets, exact at the truth tier), else a word. */
    static Component tu(float tu, SuitSyncPayload s) {
        if (truth())
            return Component.literal(Math.round(tu) + " Tu");
        if (figures(s)) {
            float step = Math.max(1f, 10f * s.gauge());
            return Component.literal(Math.round(Math.round(tu / step) * step) + " Tu");
        }
        return Component.translatable("hud.feedback.suit.thermal." + (tu < 5f ? "cold" : tu < 28f ? "cool" : tu < 45f ? "warm" : tu < 150f ? "hot" : "scorching"));
    }

    static Component mass(SuitSyncPayload s) {
        return figures(s) ? Component.translatable("hud.feedback.suit.mass", Math.round(s.mass() * 10f) / 10f)
                : Component.translatable("hud.feedback.suit.mass." + (s.overload() ? "over" : s.mass() < 30f ? "light" : s.mass() < 60f ? "heavy" : "crushing"));
    }
}
