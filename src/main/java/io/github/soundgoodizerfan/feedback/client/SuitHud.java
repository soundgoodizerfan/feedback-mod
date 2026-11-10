// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.suit.Ability;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The suit's card, bottom left, while one is worn: five lamps (one per ability: lit cyan when on and live, stuttering amber when on but
 * starved, outlined when off, dark when nothing fitted provides it), a charge bar and a heat bar, and the pack's overload warning. The bars read
 * in words or figures as the suit's gauges allow ({@link SuitReadout}). It slides in when a suit goes on, and is drawn on the suit skin so it
 * reads as the armour's own display, not the game's.
 */
final class SuitHud {

    private static final GuiSkin SKIN = GuiSkin.SUIT;
    private static final int W = 104, BAR_H = 5;
    private static final String[] TAG = { "ST", "JP", "PL", "NV", "TH" };

    private static final GuiStyle.Slide SLIDE = new GuiStyle.Slide();
    private static final GuiStyle.Flash FLASH = new GuiStyle.Flash(2);
    private static double charge = -1, heat = -1;

    private SuitHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("suit_hud"), SuitHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !SuitClient.worn() || mc.screen instanceof SuitWheelScreen) {
            charge = heat = -1;
            return;
        }
        SuitSyncPayload s = SuitClient.state();
        double ct = SuitReadout.shown(s.charge(), s), ht = SuitReadout.shown(SuitReadout.heatShare(s), s);
        charge = charge < 0 ? ct : charge + (ct - charge) * 0.25;
        heat = heat < 0 ? ht : heat + (ht - heat) * 0.25;
        int slide = SLIDE.offset(Boolean.TRUE, W + 14);
        int x0 = 6 - slide, y1 = g.guiHeight() - 6, h = 60, y0 = y1 - h, x1 = x0 + W;
        long now = net.minecraft.Util.getMillis();

        // the card: dark glass, chamfered corners, a cyan edge and an amber tick
        g.fill(x0, y0, x1, y1, 0xC0 << 24 | 0x06090B);
        g.renderOutline(x0, y0, W, h, 0x90 << 24 | 0x35B6D0);
        g.fill(x0, y0, x0 + 3, y0 + 1, 0xFF << 24 | 0xFFA52E);
        g.fill(x1 - 3, y1 - 1, x1, y1, 0xFF << 24 | 0x4FE3FF);
        for (int i = 0; i < 3; i++) {
            g.fill(x0 + i, y0 + 2 - i, x0 + i + 1, y0 + 3 - i, 0);
        }

        var font = mc.font;
        // lamps
        Ability[] all = Ability.values();
        for (int i = 0; i < all.length; i++) {
            Ability a = all[i];
            int lx = x0 + 5 + i * 19, ly = y0 + 5;
            boolean can = SuitClient.can(a), on = SuitClient.on(a), live = SuitClient.live(a);
            int c = !can ? 0x40 << 24 | 0x7C99A6 : live ? 0xFF << 24 | 0x4FE3FF : on ? ((now / 90) % 3 != 0 ? 0xFF << 24 | 0xFFA52E : 0x60 << 24 | 0xFFA52E) : 0xA0 << 24 | 0x2E7F94;
            if (live)
                g.fill(lx, ly, lx + 17, ly + 11, 0x38 << 24 | 0x4FE3FF);
            g.renderOutline(lx, ly, 17, 11, c);
            g.drawCenteredString(font, TAG[i], lx + 9, ly + 2, can ? c | 0xFF000000 : c);
        }
        // charge and heat
        int bx = x0 + 5, bw = W - 10;
        Component cw = SuitReadout.charge(s), hw = SuitReadout.heat(s);
        g.drawString(font, cw, bx, y0 + 20, FLASH.colour(0, cw.getString(), SKIN.ink() | 0xFF000000, SKIN.accent() | 0xFF000000), false);
        g.drawString(font, Component.translatable("hud.feedback.suit.charge"), x1 - 5 - font.width(Component.translatable("hud.feedback.suit.charge")), y0 + 20, SKIN.dim() | 0xFF000000, false);
        boolean low = charge < 0.25;
        Gauge.of(() -> charge).lead(() -> low ? SKIN.warn() : SKIN.accent()).render(g, SKIN, bx, y0 + 30, bw, BAR_H);
        g.drawString(font, hw, bx, y0 + 40, FLASH.colour(1, hw.getString(), s.cut() ? SKIN.warn() | 0xFF000000 : SKIN.ink() | 0xFF000000, SKIN.accent() | 0xFF000000), false);
        g.drawString(font, Component.translatable("hud.feedback.suit.heat"), x1 - 5 - font.width(Component.translatable("hud.feedback.suit.heat")), y0 + 40, SKIN.dim() | 0xFF000000, false);
        double hshown = heat;
        Gauge.of(() -> hshown).lead(() -> hshown > 0.75 ? SKIN.warn() : SKIN.accent()).render(g, SKIN, bx, y0 + 50, bw, BAR_H);
        if (s.overload() && (now / 250) % 2 == 0)
            g.drawCenteredString(font, Component.translatable("hud.feedback.suit.overload"), x0 + W / 2, y0 - 10, SKIN.warn() | 0xFF000000);
    }
}
