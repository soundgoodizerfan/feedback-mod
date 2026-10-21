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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;
import java.util.Objects;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunAssembly;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Gun Table: blueprint, sixteen part slots, a round slot, the gun, and beside them the gun's
 * spec sheet against the round (guns spec 13.6). Every figure goes through {@link Readout#figure}.
 */
public class GunTableScreen extends SkinnedScreen<GunTableMenu> {

    /** The sheet's body area, under its heading: it scrolls when its content is taller. */
    private static final int SHEET_AREA_H = GunTableMenu.H - 10 - GunTableMenu.SHEET_Y;

    private int sheetScroll, contentH, sheetKey;

    public GunTableScreen(GunTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        imageWidth = GunTableMenu.W;
        imageHeight = GunTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = GunTableMenu.INV_X;
        inventoryLabelY = GunTableMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int gx = leftPos + GunTableMenu.GRID_X, gy = topPos + GunTableMenu.GRID_Y;
        g.fill(gx - 4, gy - 4, gx + 74, gy + 74, 0xFF161311);
        g.renderOutline(gx - 4, gy - 4, 78, 78, 0xFF8C6A32);
        for (int i = 0; i < 16; i++)
            skin.slot(g, gx + i % 4 * 18, gy + i / 4 * 18);
        skin.slot(g, leftPos + GunTableMenu.PLAN_X, topPos + GunTableMenu.PLAN_Y);
        skin.slot(g, leftPos + GunTableMenu.ROUND_X, topPos + GunTableMenu.ROUND_Y);
        int rx = leftPos + GunTableMenu.OUT_X, ry = topPos + GunTableMenu.OUT_Y;
        g.fill(rx - 5, ry - 5, rx + 21, ry + 21, 0xFF5E4520);
        g.renderOutline(rx - 5, ry - 5, 26, 26, 0xFFC8A054);
        skin.slot(g, rx, ry);
        inventory(g, GunTableMenu.INV_X, GunTableMenu.INV_Y);
        int px = leftPos + GunTableMenu.SHEET_X - 6, py = topPos + 18;
        g.fill(px, py, leftPos + imageWidth - 8, topPos + imageHeight - 8, 0x30000000);
        g.fill(px + 4, py + 16, px + 4 + GunTableMenu.SHEET_W + 2, py + 18, skin.accent());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        caption(g, "blueprint", GunTableMenu.PLAN_X, GunTableMenu.PLAN_Y - 11);
        caption(g, "round", GunTableMenu.ROUND_X, GunTableMenu.ROUND_Y - 11);
        caption(g, "parts", GunTableMenu.GRID_X, GunTableMenu.GRID_Y - 11);
        Component result = Component.translatable("screen.feedback.gun_table.result");
        g.drawString(font, result, GunTableMenu.OUT_X + 8 - font.width(result) / 2, GunTableMenu.OUT_Y - 12, skin.dim(), false);
        g.drawString(font, Component.translatable("screen.feedback.gun_table.sheet"), GunTableMenu.SHEET_X, GunTableMenu.SHEET_Y - 14,
                skin.ink(), false);
        GunFigures f = null;
        String state = menu.getSlot(GunTableMenu.PLAN).getItem().isEmpty() ? "put"
                : menu.gun().isEmpty() || !GunAssembly.isGun(menu.gun()) ? "short"
                : menu.round().isEmpty() ? "add_round" : "sheet";
        if (state.equals("sheet") && (f = menu.figures().get(FWeapons.GUN_FIGURES.get())) == null)
            state = "no_fit";
        // A different state, figures or missing list is a different sheet: back to its top.
        int key = Objects.hash(state, f, menu.missing());
        if (key != sheetKey) {
            sheetKey = key;
            sheetScroll = 0;
        }
        sheetScroll = Mth.clamp(sheetScroll, 0, Math.max(0, contentH - SHEET_AREA_H));
        g.enableScissor(leftPos + GunTableMenu.SHEET_X, topPos + GunTableMenu.SHEET_Y,
                leftPos + GunTableMenu.SHEET_X + GunTableMenu.SHEET_W, topPos + GunTableMenu.SHEET_Y + SHEET_AREA_H);
        int top = GunTableMenu.SHEET_Y - sheetScroll;
        contentH = body(g, state, f, top) - top;
        g.disableScissor();
        scrollbar(g, GunTableMenu.SHEET_X + GunTableMenu.SHEET_W + 2, GunTableMenu.SHEET_Y, SHEET_AREA_H, contentH, SHEET_AREA_H, sheetScroll);
    }

    /** The sheet's body from {@code y}; returns the y below it, which is how tall the content is. */
    private int body(GuiGraphics g, String state, GunFigures f, int y) {
        switch (state) {
            case "short" -> {
                var gun = ModularTypes.known().get(Feedback.id("gun"));
                if (gun == null || menu.missing() == 0)
                    return line(g, Component.translatable("screen.feedback.gun_table.short"), y, skin.dim());
                y = line(g, Component.translatable("screen.feedback.gun_table.missing"), y, skin.dim());
                List<String> order = gun.order();
                for (int i = 0; i < order.size(); i++)
                    if ((menu.missing() >> i & 1) != 0)
                        y = line(g, Component.translatable("modular.feedback.role." + gun.roles().get(order.get(i)).name()), y);
                return y;
            }
            case "put", "add_round" -> {
                return line(g, Component.translatable("screen.feedback.gun_table." + state), y, skin.dim());
            }
            case "no_fit" -> {
                return line(g, Component.translatable("screen.feedback.gun_table.no_fit").withStyle(ChatFormatting.RED), y);
            }
            default -> {
            }
        }
        int pressure = f.overpressure() > 1 ? 0xFFE06060 : skin.ink();
        y = group(g, "chamber", y);
        y = line(g, Component.translatable("screen.feedback.gun_table.charge", num(f.peakPressure().value())), y, pressure);
        y = line(g, Component.translatable("screen.feedback.gun_table.rating", num(f.chamberRating().value())), y, skin.ink());
        y = line(g, Component.translatable("screen.feedback.gun_table.over", num(f.overpressure())), y, pressure);
        int fill = Math.round(GunTableMenu.SHEET_W * Math.min(1f, f.overpressure()));
        g.fill(GunTableMenu.SHEET_X, y, GunTableMenu.SHEET_X + GunTableMenu.SHEET_W, y + 4, 0xFF161311);
        g.fill(GunTableMenu.SHEET_X, y, GunTableMenu.SHEET_X + fill, y + 4, f.overpressure() > 1 ? 0xFFE06060 : skin.accent());
        y = group(g, "muzzle", y + 10);
        y = line(g, Component.translatable("screen.feedback.gun_table.speed", num(f.muzzleSpeed() * 20)), y, skin.ink());
        y = line(g, Component.translatable("screen.feedback.gun_table.energy", num(f.muzzleEnergy())), y, skin.ink());
        y = line(g, Component.translatable("screen.feedback.gun_table.report", num(f.report())), y, skin.ink());
        y = group(g, "handling", y + 2);
        y = line(g, Component.translatable("screen.feedback.gun_table.kick", num(f.kickDeg())), y, skin.ink());
        y = line(g, Component.translatable("screen.feedback.gun_table.spread", num(f.spreadDeg())), y, skin.ink());
        return line(g, Component.translatable("screen.feedback.gun_table.cycle", num(f.cycleMs())), y, skin.ink());
    }

    /** The wheel over the sheet scrolls it, when it overflows; anywhere else it does what it did. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (settled() && contentH > SHEET_AREA_H && mouseX >= leftPos + GunTableMenu.SHEET_X - 6 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + 18 && mouseY < topPos + imageHeight - 8) {
            sheetScroll = Mth.clamp(sheetScroll - (int) Math.signum(scrollY) * 10, 0, contentH - SHEET_AREA_H);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static String num(float value) {
        return Readout.figure(Quantity.FIGURE, value);
    }

    private void caption(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable("screen.feedback.gun_table." + key), x, y, skin.dim(), false);
    }

    private int group(GuiGraphics g, String key, int y) {
        g.drawString(font, Component.translatable("screen.feedback.gun_table." + key), GunTableMenu.SHEET_X, y, skin.dim(), false);
        return y + 11;
    }

    /** One wrapped line of the sheet; returns the y below it. */
    private int line(GuiGraphics g, Component text, int y) {
        return line(g, text, y, skin.ink());
    }

    private int line(GuiGraphics g, Component text, int y, int colour) {
        g.drawWordWrap(font, text, GunTableMenu.SHEET_X, y, GunTableMenu.SHEET_W, colour);
        return y + 9 * font.split(text, GunTableMenu.SHEET_W).size() + 2;
    }
}
