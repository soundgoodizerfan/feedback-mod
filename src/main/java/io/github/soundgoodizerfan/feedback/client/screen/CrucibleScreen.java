// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleMenu;
import io.github.soundgoodizerfan.feedback.client.gui.Gauge;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.HeatGlow;
import io.github.soundgoodizerfan.feedback.client.bench.BenchFire;
import io.github.soundgoodizerfan.feedback.client.item.FormTint;
import io.github.soundgoodizerfan.feedback.client.render.GlowColours;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;

import io.github.soundgoodizerfan.feedback.client.gui.Specks;

import net.minecraft.client.Minecraft;
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

    /** Sparks off glowing stock and bubbles in a hot melt, panel-relative. */
    private final Specks specks = new Specks(40);

    @Override
    protected void containerTick() {
        super.containerTick();
        specks.tick(0f);
        int tu = menu.temperatureTu();
        for (int slot = 0; slot < menu.size(); slot++)
            if (FormTint.glowing(menu.getSlot(slot).getItem()))
                HeatGlow.embers(specks, CrucibleMenu.slotX(slot, menu.size()), CrucibleMenu.slotY(slot, menu.size()),
                        menu.getSlot(slot).getItem());
        if (menu.tankMb() > 0 && tu >= 480) {
            int h = Math.round(CrucibleMenu.GAUGE_H * (float) (menu.tankMb() / (double) menu.tankCapacity()));
            HeatGlow.bubble(specks, CrucibleMenu.TANK_X, CrucibleMenu.GAUGE_Y + CrucibleMenu.GAUGE_H - h, CrucibleMenu.TANK_W, h,
                    IClientFluidTypeExtensions.of(menu.tankFluid()).getTintColor() & 0xFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        specks.draw(graphics, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    private int band() {
        int tu = menu.temperatureTu();
        return tu >= (int) ThermalTuning.MAX_VISIBLE_TU.value() ? ThermalVesselScreen.BAND_COLORS.length - 1 : ThermalVesselScreen.firstBandAbove(tu);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        menu.showHeat(minecraft.level);
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        float tu = menu.temperatureTu();
        // The chamber's own light, drawn first so it bleeds past the gauges' edge.
        BenchFire.halo(graphics, leftPos + CrucibleMenu.HEAT_X - 2, topPos + CrucibleMenu.GAUGE_Y - 2,
                CrucibleMenu.TANK_X + CrucibleMenu.TANK_W - CrucibleMenu.HEAT_X + 4, CrucibleMenu.GAUGE_H + 4, tu * HeatGlow.pulse());
        for (int slot = 0; slot < menu.size(); slot++) {
            int sx = leftPos + CrucibleMenu.slotX(slot, menu.size()), sy = topPos + CrucibleMenu.slotY(slot, menu.size());
            skin.slot(graphics, sx, sy);
            if (FormTint.glowing(menu.getSlot(slot).getItem()))
                HeatGlow.slot(graphics, sx, sy, menu.getSlot(slot).getItem());
        }

        int band = band();
        Gauge.of(() -> (band + 1f) / ThermalVesselScreen.BAND_COLORS.length).lead(() -> ThermalVesselScreen.BAND_COLORS[band])
                .ticks(ThermalVesselScreen.BAND_COLORS.length).vertical()
                .render(graphics, skin, leftPos + CrucibleMenu.HEAT_X, topPos + CrucibleMenu.GAUGE_Y, CrucibleMenu.GAUGE_W, CrucibleMenu.GAUGE_H);

        var fluid = menu.tankFluid();
        // The melt shines toward the glow colour as hot as the chamber is, flickering with the fire.
        int colour = GlowColours.heated(IClientFluidTypeExtensions.of(fluid).getTintColor() & 0xFFFFFF, tu * HeatGlow.pulse()) | 0xFF000000;
        Gauge.of(() -> menu.tankMb() / (double) menu.tankCapacity()).liquid(menu::tankFluid).lead(() -> colour).vertical()
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
