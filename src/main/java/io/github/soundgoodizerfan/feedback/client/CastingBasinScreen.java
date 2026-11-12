// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Casting Basin: molds in a column, the first at the top filling first, and a gauge for the
 * open pool beside them, one tick to the ingot. Stone ({@link GuiSkin#STONE}).
 */
public class CastingBasinScreen extends SkinnedScreen<CastingBasinMenu> {

    private static final int GAUGE_X = 130, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 86;

    private final Gauge pool = Gauge.of(() -> menu.poolMb() / (float) FTuning.BASIN_POOL_MB).ticks(9).vertical();

    public CastingBasinScreen(CastingBasinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.STONE);
        imageWidth = 176;
        imageHeight = 198;
        inventoryLabelY = CastingBasinMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < CastingBasinBlockEntity.SLOTS; i++)
            skin.slot(g, leftPos + CastingBasinMenu.SLOT_X, topPos + CastingBasinMenu.SLOT_Y + i * CastingBasinMenu.SLOT_STEP);
        inventory(g, CastingBasinMenu.INVENTORY_X, CastingBasinMenu.INVENTORY_Y);
        pool.render(g, skin, leftPos + GAUGE_X, topPos + GAUGE_Y, GAUGE_W, GAUGE_H);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.feedback.casting_basin.molds"), 12, CastingBasinMenu.SLOT_Y + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.casting_basin.pool"), GAUGE_X - 4, GAUGE_Y - 9, skin.dim(), false);
    }
}
