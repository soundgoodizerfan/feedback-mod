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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
public class ThermalVesselScreen extends AbstractContainerScreen<ThermalVesselMenu> {

    private static final ResourceLocation LIT_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    private static final int[] BAND_COLORS = {
            0xFF6B6B6B, 0xFF7A4A38, 0xFF8B2E12, 0xFFA82A00, 0xFFC22800,
            0xFFE83300, 0xFFFF7A00, 0xFFFFC400, 0xFFFFE988, 0xFFFFFFFF,
    };

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SOCKET = 0xFF8B8B8B;
    private static final int GAUGE_WIDTH = 10;

    private static final int FLAME_X = 8;
    private static final int FLAME_Y = ThermalVesselMenu.GRID_Y;

    public ThermalVesselScreen(ThermalVesselMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = ThermalVesselMenu.INV_Y + 58 + 18 + 6;
        inventoryLabelY = ThermalVesselMenu.INV_Y - 12;
    }

    /**
     * {@code AbstractContainerScreen} never calls {@code renderTooltip} itself -- every vanilla
     * screen (see {@code AbstractFurnaceScreen}) overrides {@code render} to call it explicitly
     * after {@code super.render()}. Omitting this entirely was the actual bug: not a broken
     * temperature line, but no tooltip at all, for anything, in this screen.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        for (int slot = 0; slot < ThermalVesselBlockEntity.SLOTS; slot++) {
            int column = slot % ThermalVesselMenu.GRID_COLUMNS;
            int row = slot / ThermalVesselMenu.GRID_COLUMNS;
            socket(graphics, ThermalVesselMenu.GRID_X + column * ThermalVesselMenu.SLOT_PX,
                    ThermalVesselMenu.GRID_Y + row * ThermalVesselMenu.SLOT_PX);
        }
        socket(graphics, ThermalVesselMenu.FUEL_X, ThermalVesselMenu.FUEL_Y);

        if (menu.isLit()) {
            int height = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
            int bottom = topPos + FLAME_Y + 14;
            graphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - height,
                    leftPos + FLAME_X, bottom - height, 14, height);
        }

        int gaugeTu = menu.getGaugeTu();
        int band = gaugeTu >= (int) FTuning.MAX_VISIBLE_TU.value()
                ? BAND_COLORS.length - 1
                : firstBandAbove(gaugeTu);

        int x1 = leftPos + ThermalVesselMenu.GAUGE_X;
        int y1 = topPos + ThermalVesselMenu.GAUGE_Y;
        graphics.fill(x1, y1, x1 + GAUGE_WIDTH, y1 + ThermalVesselMenu.GAUGE_HEIGHT, 0xFF202020);

        int filled = (band + 1) * ThermalVesselMenu.GAUGE_HEIGHT / BAND_COLORS.length;
        graphics.fill(x1 + 1, y1 + ThermalVesselMenu.GAUGE_HEIGHT - filled, x1 + GAUGE_WIDTH - 1,
                y1 + ThermalVesselMenu.GAUGE_HEIGHT - 1, BAND_COLORS[band]);
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

    private void socket(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + ThermalVesselMenu.SLOT_PX,
                topPos + y + ThermalVesselMenu.SLOT_PX, SOCKET);
    }

    private static int firstBandAbove(int tu) {
        for (int band = 0; band < FTuning.HEAT_BAND_TOPS.size(); band++)
            if (tu < FTuning.HEAT_BAND_TOPS.get(band).value())
                return band;
        return FTuning.HEAT_BAND_TOPS.size() - 1;
    }
}
