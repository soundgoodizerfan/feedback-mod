// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselMenu;

import io.github.soundgoodizerfan.feedback.client.gui.Gauge;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.HeatGlow;
import io.github.soundgoodizerfan.feedback.client.item.FormTint;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;

import io.github.soundgoodizerfan.feedback.client.gui.Specks;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Spinner;

import net.minecraft.client.Minecraft;
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
 * using {@link ThermalTuning#HEAT_BAND_TOPS}: what the player sees is the blacksmith's read of the
 * glow, in a strip instead of a sentence, no finer than the free sense it stands in for.
 */
public class ThermalVesselScreen extends SkinnedScreen<ThermalVesselMenu> {

    private static final ResourceLocation LIT_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    static final int[] BAND_COLORS = {
            0xFF6B6B6B, 0xFF7A4A38, 0xFF8B2E12, 0xFFA82A00, 0xFFC22800,
            0xFFE83300, 0xFFFF7A00, 0xFFFFC400, 0xFFFFE988, 0xFFFFFFFF,
    };

    /** Sparks and steam over the grid; the slot's panel-relative position. */
    private final Specks specks = new Specks(40);

    private static final int GAUGE_WIDTH = 10;

    private static final int FLAME_X = 8;
    private static final int FLAME_Y = ThermalVesselMenu.GRID_Y;

    private static GuiSkin skinOf(ThermalVesselMenu menu) {
        return switch (menu.getKind()) {
            case SMOKER -> GuiSkin.SMOKE;
            case BLAST_FURNACE, PUDDLING_FURNACE -> GuiSkin.SLAG;
            case KILN -> GuiSkin.KILN;
            case ANNEALING_FURNACE -> GuiSkin.ANNEALING_FURNACE;
            case IMPROVED_FURNACE -> GuiSkin.IMPROVED_FURNACE;
            default -> GuiSkin.STONE;
        };
    }

    public ThermalVesselScreen(ThermalVesselMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, skinOf(menu));
        io.github.soundgoodizerfan.feedback.client.LinkedMachine.link(title, menu.getKind().ceilingTu());
        titleLabelX = 12;
        titleLabelY = 6;
        imageWidth = 176;
        imageHeight = ThermalVesselMenu.INV_Y + 58 + 18 + 8;
        inventoryLabelY = ThermalVesselMenu.INV_Y - 12;
    }

    private final Spinner rake = new Spinner();
    private static final ResourceLocation RAKE = Feedback.id("spin/rabble_rake"), BED = Feedback.id("spin/hearth_bed"),
            HEARTH = Feedback.id("spin/hearth_rim");

    /** The rabble window, right of the gauge (relative to the panel): the rake end-on, a speed dial under it. */
    private static final int ROD_X = ThermalVesselMenu.GAUGE_X + 18, ROD_Y = ThermalVesselMenu.GAUGE_Y, ROD_W = 28;

    @Override
    protected void containerTick() {
        super.containerTick();
        specks.tick(0f);
        int tu = menu.getGaugeTu();
        for (int slot = 0; slot < ThermalVesselBlockEntity.SLOTS; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            int sx = ThermalVesselMenu.GRID_X + slot % ThermalVesselMenu.GRID_COLUMNS * ThermalVesselMenu.SLOT_PX;
            int sy = ThermalVesselMenu.GRID_Y + slot / ThermalVesselMenu.GRID_COLUMNS * ThermalVesselMenu.SLOT_PX;
            if (FormTint.glowing(stack))
                HeatGlow.embers(specks, sx, sy, stack);
            else if (stack.has(FDataComponents.FOOD_STATE.get()) && tu >= 100)
                HeatGlow.steam(specks, sx, sy);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        specks.draw(graphics, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        menu.showHeat(minecraft.level);
        int gaugeTu = menu.getGaugeTu();
        skin.heat(gaugeTu); // a data skin's heat decor glows with the chamber
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int band = gaugeTu >= (int) ThermalTuning.MAX_VISIBLE_TU.value()
                ? BAND_COLORS.length - 1
                : firstBandAbove(gaugeTu);

        // The chamber takes the colour of its heat, by band and no finer -- the glow a smith reads.
        int gx = leftPos + ThermalVesselMenu.GRID_X, gy = topPos + ThermalVesselMenu.GRID_Y;
        int side = ThermalVesselMenu.GRID_COLUMNS * ThermalVesselMenu.SLOT_PX;
        graphics.fill(gx - 3, gy - 3, gx + side + 1, gy + side + 1, 0xFF161311);
        graphics.renderOutline(gx - 3, gy - 3, side + 4, side + 4, 0xFF6E6A64);
        float pulse = HeatGlow.pulse();
        if (band > 0)
            graphics.fillGradient(gx - 2, gy - 2, gx + side, gy + side,
                    Math.min(0xFF, (int) (band * 0x05 * pulse)) << 24 | BAND_COLORS[band] & 0xFFFFFF,
                    Math.min(0xFF, (int) (band * 0x0C * pulse)) << 24 | BAND_COLORS[band] & 0xFFFFFF);
        for (int slot = 0; slot < ThermalVesselBlockEntity.SLOTS; slot++) {
            int column = slot % ThermalVesselMenu.GRID_COLUMNS;
            int row = slot / ThermalVesselMenu.GRID_COLUMNS;
            int sx = gx + column * ThermalVesselMenu.SLOT_PX, sy = gy + row * ThermalVesselMenu.SLOT_PX;
            skin.slot(graphics, sx, sy);
            // Glowing stock throws its own bloom, at its own heat.
            if (FormTint.glowing(menu.getSlot(slot).getItem()))
                HeatGlow.slot(graphics, sx, sy, menu.getSlot(slot).getItem());
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
        if (menu.getKind().rotates())
            rabble(graphics);
        inventory(graphics, 8, ThermalVesselMenu.INV_Y);
    }

    /**
     * The rabble end-on in the charge, turning at its real speed, and under it a dial: the speed on a scale with the working band of
     * the running row marked and a needle at the rod's speed. The needle is the accent inside the band and the warning colour outside
     * it, so a charge that is not changing shows why, in place.
     */
    private void rabble(GuiGraphics g) {
        int x = leftPos + ROD_X, y = topPos + ROD_Y;
        skin.inset(g, x, y, ROD_W, ROD_W);
        int cx = x + ROD_W / 2, cy = y + ROD_W / 2;
        // The charge in the hearth: a grey cinder sprite tinted from dark brown to glowing by the chamber's heat.
        int bed = io.github.soundgoodizerfan.feedback.client.render.GlowColours.heated(0x2A1C14, menu.getGaugeTu());
        g.setColor((bed >> 16 & 0xFF) / 255f, (bed >> 8 & 0xFF) / 255f, (bed & 0xFF) / 255f, 1f);
        g.blitSprite(BED, cx - 12, cy - 12, 24, 24);
        g.setColor(1f, 1f, 1f, 1f);
        rake.draw(g, RAKE, cx, cy, 24, 24, menu.rabbleRpm());
        g.blitSprite(HEARTH, cx - 12, cy - 12, 24, 24);

        int dy = y + ROD_W + 4, dh = 12;
        skin.inset(g, x, dy, ROD_W, dh);
        int lo = menu.bandMin(), hi = menu.bandMax(), rpm = Math.abs(menu.rabbleRpm());
        float span = Math.max(Math.max(hi * 1.5f, rpm * 1.1f), 1f);
        if (hi > 0)
            g.fill(x + 1 + Math.round((ROD_W - 2) * lo / span), dy + 1, x + 1 + Math.round((ROD_W - 2) * hi / span), dy + dh - 1,
                    0x60000000 | skin.accent() & 0xFFFFFF);
        int nx = x + 1 + Math.min(ROD_W - 3, Math.round((ROD_W - 2) * rpm / span));
        boolean inside = hi <= 0 || rpm >= lo && rpm <= hi;
        g.fill(nx, dy + 1, nx + 1, dy + dh - 1, inside ? skin.ink() : skin.warn());
    }

    static int firstBandAbove(int tu) {
        for (int band = 0; band < ThermalTuning.HEAT_BAND_TOPS.size(); band++)
            if (tu < ThermalTuning.HEAT_BAND_TOPS.get(band).value())
                return band;
        return ThermalTuning.HEAT_BAND_TOPS.size() - 1;
    }
}
