// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.SightCell;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The rangefinder's line under the crosshair: the distance to what the held gun is pointed at, whole blocks (the sight is the
 * instrument, and it resolves a block). Drawn only while the gun's rangefinder has charge in its cell.
 */
public final class SightHud {

    private static final double MAX = 300;

    private SightHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("sight_range"), SightHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson())
            return;
        ItemStack gun = SightCell.powered(mc.player.getMainHandItem(), SightCell.RANGEFINDER) ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
        if (!SightCell.powered(gun, SightCell.RANGEFINDER))
            return;
        SightRay.Hit hit = SightRay.cast(mc.player, MAX);
        Component line = hit.at() == null ? Component.translatable("hud.feedback.gun.range.none")
                : Component.translatable("hud.feedback.gun.range", Math.round(hit.distance()));
        g.drawCenteredString(mc.font, line, g.guiWidth() / 2, g.guiHeight() / 2 + 14, 0xFFFFFFFF);
    }
}
