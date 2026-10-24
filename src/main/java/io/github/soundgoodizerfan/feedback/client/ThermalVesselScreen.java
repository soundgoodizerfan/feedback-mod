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

import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselMenu;
import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A plain, chest-like panel -- nine slots, one fuel slot set apart, a banded heat gauge. No
 * vanilla furnace art: see {@link ThermalVesselMenu} for why the fuel slot is not underneath the
 * grid, and {@code VanillaVessels} (the retired mixin pass) for why an arrow was wrong the moment
 * a finished item stopped travelling anywhere -- everything here changes in place, so nothing
 * this screen draws should suggest otherwise.
 *
 * <h2>Why the gauge is banded rather than a smooth bar</h2>
 * A continuously-filling bar is a number wearing a picture -- exactly what {@code Readout}
 * refuses to do anywhere else, and free precision this mod otherwise sells as an instrument (§8).
 * The fill steps through the same ten bands {@code Readout.temperature} already names in words,
 * using {@link FTuning#HEAT_BAND_TOPS}: what the player sees is the blacksmith's read of the
 * glow, in a strip instead of a sentence, no finer than the free sense it stands in for.
 */
public class ThermalVesselScreen extends SkinnedScreen<ThermalVesselMenu> {

    private static final ResourceLocation LIT_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    private static final int[] BAND_COLORS = {
            0xFF6B6B6B, 0xFF7A4A38, 0xFF8B2E12, 0xFFA82A00, 0xFFC22800,
            0xFFE83300, 0xFFFF7A00, 0xFFFFC400, 0xFFFFE988, 0xFFFFFFFF,
    };

    private static final int GAUGE_WIDTH = 10;

    private static final int FLAME_X = 8;
    private static final int FLAME_Y = ThermalVesselMenu.GRID_Y;

    public ThermalVesselScreen(ThermalVesselMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GuiSkin.STONE);
        titleLabelX = 12;
        titleLabelY = 6;
        imageWidth = 176;
        imageHeight = ThermalVesselMenu.INV_Y + 58 + 18 + 8;
        inventoryLabelY = ThermalVesselMenu.INV_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int gaugeTu = menu.getGaugeTu();
        int band = gaugeTu >= (int) FTuning.MAX_VISIBLE_TU.value()
                ? BAND_COLORS.length - 1
                : firstBandAbove(gaugeTu);

        // The chamber takes the colour of its heat, by band and no finer -- the glow a smith reads.
        int gx = leftPos + ThermalVesselMenu.GRID_X, gy = topPos + ThermalVesselMenu.GRID_Y;
        int side = ThermalVesselMenu.GRID_COLUMNS * ThermalVesselMenu.SLOT_PX;
        graphics.fill(gx - 3, gy - 3, gx + side + 1, gy + side + 1, 0xFF161311);
        graphics.renderOutline(gx - 3, gy - 3, side + 4, side + 4, 0xFF6E6A64);
        if (band > 0)
            graphics.fillGradient(gx - 2, gy - 2, gx + side, gy + side,
                    (band * 0x05) << 24 | BAND_COLORS[band] & 0xFFFFFF, (band * 0x0C) << 24 | BAND_COLORS[band] & 0xFFFFFF);
        for (int slot = 0; slot < ThermalVesselBlockEntity.SLOTS; slot++) {
            int column = slot % ThermalVesselMenu.GRID_COLUMNS;
            int row = slot / ThermalVesselMenu.GRID_COLUMNS;
            skin.slot(graphics, gx + column * ThermalVesselMenu.SLOT_PX, gy + row * ThermalVesselMenu.SLOT_PX);
        }

        // The fire mouth: an arch under the fuel, glowing while lit, the glow flickering as a fire does.
        int fx = leftPos + ThermalVesselMenu.FUEL_X, fy = topPos + ThermalVesselMenu.FUEL_Y;
        graphics.fill(fx - 4, fy - 4, fx + 20, fy + 20, 0xFF0E0C0B);
        graphics.renderOutline(fx - 4, fy - 4, 24, 24, 0xFF4A4743);
        graphics.fill(fx - 2, fy - 6, fx + 18, fy - 4, 0xFF4A4743); // the arch's lintel
        if (menu.isLit()) {
            double t = Util.getMillis() / 1000.0;
            float flicker = 0.75f + 0.15f * (float) Math.sin(t * 11.3) + 0.1f * (float) Math.sin(t * 23.7 + 1.3);
            for (int i = 0; i < 4; i++) {
                int a = (int) (flicker * (0x30 - i * 0x0A));
                graphics.fill(fx - 3 + i, fy - 3 + i, fx + 19 - i, fy + 19 - i, a << 24 | 0xFF7A20);
            }
        }
        skin.slot(graphics, fx, fy);
        if (menu.isLit()) {
            int height = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
            int bottom = topPos + FLAME_Y + 14;
            graphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - height,
                    leftPos + FLAME_X, bottom - height, 14, height);
        }

        // The gauge, filled to the band and no finer, a tick at every band.
        Gauge.of(() -> (band + 1f) / BAND_COLORS.length).lead(() -> BAND_COLORS[band]).ticks(BAND_COLORS.length).vertical()
                .render(graphics, skin, leftPos + ThermalVesselMenu.GAUGE_X, topPos + ThermalVesselMenu.GAUGE_Y,
                        GAUGE_WIDTH, ThermalVesselMenu.GAUGE_HEIGHT);
        inventory(graphics, 8, ThermalVesselMenu.INV_Y);
    }

    /**
     * A workpiece mid-cook has no stamped {@code ItemHeat} of its own -- see {@code
     * ThermalVesselMenu}'s {@code HeatStampingSlot}, which only stamps one at the moment it leaves -- so
     * this is the one place a figure has to come from the vessel's own live reading instead of
     * the item. Only for the nine workpiece slots; the fuel slot has no comparable claim to a
     * temperature of its own.
     */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && hoveredSlot.getContainerSlot() < ThermalVesselBlockEntity.SLOTS)
            tooltip.add(Readout.temperatureReading(new Tu(menu.getGaugeTu())));
        return tooltip;
    }

    private static int firstBandAbove(int tu) {
        for (int band = 0; band < FTuning.HEAT_BAND_TOPS.size(); band++)
            if (tu < FTuning.HEAT_BAND_TOPS.get(band).value())
                return band;
        return FTuning.HEAT_BAND_TOPS.size() - 1;
    }
}
