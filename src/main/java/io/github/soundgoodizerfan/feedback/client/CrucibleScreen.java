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

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The crucible: its two slots, a heat gauge banded like the vessel's (no finer than the glow a smith reads), and the melt in its tank
 * as a fill in the fluid's own colour. Hover the tank for what it holds, in ingots.
 */
public class CrucibleScreen extends SkinnedScreen<CrucibleMenu> {

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.STONE);
        titleLabelX = 12;
        titleLabelY = 6;
        imageWidth = 176;
        imageHeight = CrucibleMenu.INV_Y + 58 + 18 + 8;
        inventoryLabelY = CrucibleMenu.INV_Y - 12;
    }

    private int band() {
        int tu = menu.temperatureTu();
        return tu >= (int) FTuning.MAX_VISIBLE_TU.value() ? ThermalVesselScreen.BAND_COLORS.length - 1 : ThermalVesselScreen.firstBandAbove(tu);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (int slot = 0; slot < menu.size(); slot++)
            skin.slot(graphics, leftPos + CrucibleMenu.slotX(slot, menu.size()), topPos + CrucibleMenu.slotY(slot, menu.size()));

        int band = band();
        Gauge.of(() -> (band + 1f) / ThermalVesselScreen.BAND_COLORS.length).lead(() -> ThermalVesselScreen.BAND_COLORS[band])
                .ticks(ThermalVesselScreen.BAND_COLORS.length).vertical()
                .render(graphics, skin, leftPos + CrucibleMenu.HEAT_X, topPos + CrucibleMenu.GAUGE_Y, CrucibleMenu.GAUGE_W, CrucibleMenu.GAUGE_H);

        var fluid = menu.tankFluid();
        int colour = IClientFluidTypeExtensions.of(fluid).getTintColor() | 0xFF000000;
        Gauge.of(() -> menu.tankMb() / (double) menu.tankCapacity()).lead(() -> colour).vertical()
                .render(graphics, skin, leftPos + CrucibleMenu.TANK_X, topPos + CrucibleMenu.GAUGE_Y, CrucibleMenu.TANK_W, CrucibleMenu.GAUGE_H);
        inventory(graphics, 8, CrucibleMenu.INV_Y);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        int top = topPos + CrucibleMenu.GAUGE_Y, bottom = top + CrucibleMenu.GAUGE_H;
        if (mouseY < top || mouseY >= bottom)
            return;
        if (mouseX >= leftPos + CrucibleMenu.HEAT_X && mouseX < leftPos + CrucibleMenu.HEAT_X + CrucibleMenu.GAUGE_W)
            graphics.renderTooltip(font, List.of(Readout.temperatureReading(new Tu(menu.temperatureTu()))), Optional.empty(), mouseX, mouseY);
        else if (mouseX >= leftPos + CrucibleMenu.TANK_X && mouseX < leftPos + CrucibleMenu.TANK_X + CrucibleMenu.TANK_W) {
            Component line = menu.tankMb() <= 0 ? Component.translatable("tooltip.feedback.crucible.empty")
                    : Component.translatable("tooltip.feedback.crucible.holds", new FluidStack(menu.tankFluid(), 1).getHoverName(),
                            String.format("%.1f", menu.tankMb() / 144f));
            graphics.renderTooltip(font, List.of(line), Optional.empty(), mouseX, mouseY);
        }
    }
}
