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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.net.PressPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Reloading Press ({@code feedback_guns_spec.md} section 5): case, charge and projectile in,
 * a round out. The die set is the bore, stepped; the lever seats whatever is in the slots. The
 * line under the slots is the round's own spec sheet, read as it would be seated now -- it is the
 * press's only help, because the press does not know when a charge is enough.
 */
public class ReloadingPressScreen extends AbstractContainerScreen<ReloadingPressMenu> {

    private int bore;

    public ReloadingPressScreen(ReloadingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.bore = menu.openedBore();
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> setBore(bore - 1)).bounds(leftPos + 56, topPos + 20, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> setBore(bore + 1)).bounds(leftPos + 108, topPos + 20, 16, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.feedback.reloading_press.seat"), b -> send(true))
                .bounds(leftPos + 62, topPos + 44, 56, 16).build());
    }

    private void setBore(int value) {
        bore = Mth.clamp(value, GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM);
        send(false);
    }

    private void send(boolean seat) {
        PacketDistributor.sendToServer(new PressPayload(menu.getPos(), bore, seat));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (int[] xy : ReloadingPressMenu.SLOT_XY)
            g.fill(leftPos + xy[0] - 1, topPos + xy[1] - 1, leftPos + xy[0] + 17, topPos + xy[1] + 17, 0xFF8B8B8B);
        for (int c = 0; c < 9; c++)
            g.fill(leftPos + ReloadingPressMenu.HOTBAR_X + c * 18 - 1, topPos + ReloadingPressMenu.HOTBAR_Y - 1,
                    leftPos + ReloadingPressMenu.HOTBAR_X + c * 18 + 17, topPos + ReloadingPressMenu.HOTBAR_Y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawCenteredString(font, Component.literal(bore + " mm"), 90, 24, 0xFFFFFF);
        String[] names = {"case", "charge", "projectile", "round"};
        for (int i = 0; i < 4; i++)
            g.drawString(font, Component.translatable("gui.feedback.reloading_press." + names[i]),
                    ReloadingPressMenu.SLOT_XY[i][0] - 18 < 0 ? 0 : ReloadingPressMenu.SLOT_XY[i][0] + 20,
                    ReloadingPressMenu.SLOT_XY[i][1] + 5, 0x404040, false);
        // The round as it would be seated now.
        var materials = ClientMaterials.on(minecraft.level);
        var charge = menu.getSlot(ReloadingPressBlockEntity.CHARGE).getItem();
        var cas = menu.getSlot(ReloadingPressBlockEntity.CASE).getItem();
        var chargeMat = ReloadingPressBlockEntity.located(materials, charge);
        var caseMat = ReloadingPressBlockEntity.located(materials, cas);
        if (chargeMat.isEmpty() || caseMat.isEmpty() || chargeMat.get().material().propellant().isEmpty()
                || caseMat.get().material().mechanical().yieldStrength().isEmpty())
            return;
        float peak = chargeMat.get().material().propellant().get().peakPressure().value()
                * charge.getCount() / FTuning.GUN_CHARGE_NOMINAL;
        float rating = caseMat.get().material().mechanical().yieldStrength().get().value()
                * FTuning.GUN_CASE_WALL_PER_RADIUS;
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.preview", charge.getCount(),
                        String.format("%.1f", peak), String.format("%.1f", rating)), 8, 92,
                peak > rating ? 0xAA0000 : 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
