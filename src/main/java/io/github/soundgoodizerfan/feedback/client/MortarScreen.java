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

import io.github.soundgoodizerfan.feedback.item.MortarMenu;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandBlockEntity;
import io.github.soundgoodizerfan.feedback.potion.PotionBrew;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Mortar's bench: a plant slot, a Crush button, and the live crushed-plant line, the path the plant would add in the brewing stand
 * (the stand's own ghost arithmetic, {@link PotionBrew}, at cooking heat), drawn fitted to the box: dashed as it is, solid once crushed.
 */
public class MortarScreen extends SkinnedScreen<MortarMenu> {

    private static final int BOX_X = 34, BOX_Y = 8, BOX_W = 134, BOX_H = 62, N = 24;

    public MortarScreen(MortarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        imageWidth = 176;
        imageHeight = MortarMenu.INV_Y + 58 + 18 + 8;
        titleLabelY = 10_000;
        inventoryLabelY = MortarMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.mortar.crush"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MortarMenu.CRUSH),
                leftPos + MortarMenu.SLOT_X - 1, topPos + 30, 24, 18, () -> net.minecraft.sounds.SoundEvents.GRINDSTONE_USE));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + MortarMenu.SLOT_X, topPos + MortarMenu.SLOT_Y);
        inventory(g, MortarMenu.INV_X, MortarMenu.INV_Y);
        skin.inset(g, leftPos + BOX_X, topPos + BOX_Y, BOX_W, BOX_H);
        ItemStack stack = menu.plant();
        var plant = stack.isEmpty() || minecraft.level == null ? null : BrewingStandBlockEntity.potionOf(minecraft.level, stack).orElse(null);
        if (plant == null) {
            g.drawString(font, Component.translatable(stack.isEmpty() ? "gui.feedback.mortar.put" : "gui.feedback.mortar.none"),
                    leftPos + BOX_X + 6, topPos + BOX_Y + 6, skin.dim(), false);
            return;
        }
        float rot = BrewingStandBlockEntity.rotOf(minecraft.level, stack);
        boolean crushed = stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CRUSHED.get());
        if (!crushed)
            curve(g, plant, rot, PotionTuning.GRIND_CRUSHED, skin.accent(), false);
        curve(g, plant, rot, BrewingStandBlockEntity.surface(stack), skin.ink(), !crushed);
    }

    /** The plant's path from the box's middle-left, fitted to the box; dashed marks the plant as it is, not as crushed. */
    private void curve(GuiGraphics g, io.github.soundgoodizerfan.feedback.process.Material.Potion plant, float rot, float surface, int colour, boolean dashed) {
        var seg = PotionBrew.segment(plant, rot, PotionBrew.extraction(surface, PotionTuning.COOK_TU), 0f);
        float[] p = new float[2];
        // Fit to the plant at full surface so crushing visibly grows the line inside a fixed frame.
        float fit = PotionBrew.segment(plant, rot, PotionBrew.extraction(PotionTuning.GRIND_CRUSHED, PotionTuning.COOK_TU), 0f).arcLength();
        float k = fit <= 0f ? 1f : Math.min(BOX_H, BOX_W) * 0.45f / fit;
        int cx = leftPos + BOX_X + 12, cy = topPos + BOX_Y + BOX_H / 2, px = cx, py = cy;
        for (int i = 1; i <= N; i++) {
            seg.at(i / (float) N, p);
            int x = cx + Math.round(p[0] * k), y = cy + Math.round(p[1] * k);
            if (!dashed || i % 2 == 0)
                GuiStyle.line(g, px, py, x, y, colour);
            px = x;
            py = y;
        }
        GuiStyle.disc(g, px, py, 1, colour);
    }
}
