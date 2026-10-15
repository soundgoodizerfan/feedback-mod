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
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
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
        imageHeight = 176;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.button(Component.literal("<"), b -> setBore(bore - 1), leftPos + 84, topPos + 18, 16, 14));
        addRenderableWidget(GuiStyle.button(Component.literal(">"), b -> setBore(bore + 1), leftPos + 140, topPos + 18, 16, 14));
        addRenderableWidget(GuiStyle.button(Component.translatable("gui.feedback.reloading_press.seat"), b -> send(true), leftPos + 84, topPos + 74, 56, 16));
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
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int[] xy : ReloadingPressMenu.SLOT_XY)
            GuiStyle.slot(g, leftPos + xy[0], topPos + xy[1]);
        GuiStyle.hotbar(g, leftPos + ReloadingPressMenu.HOTBAR_X, topPos + ReloadingPressMenu.HOTBAR_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawCenteredString(font, Component.literal(bore + " mm"), 120, 22, 0xFFFFFF);
        // Labels sit to the right of each slot, the second charge's after its own; the round's above it.
        String[] names = {"case", null, "charge_b", "projectile", "payload", null};
        for (int i = 0; i < names.length; i++)
            if (names[i] != null)
                g.drawString(font, Component.translatable("gui.feedback.reloading_press." + names[i]),
                        ReloadingPressMenu.SLOT_XY[i][0] + 20, ReloadingPressMenu.SLOT_XY[i][1] + 5, 0x404040, false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.charge"), 8, 42, 0x404040, false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.round"),
                ReloadingPressMenu.SLOT_XY[5][0], ReloadingPressMenu.SLOT_XY[5][1] - 11, 0x404040, false);

        // The round as it would be seated now: the blend's pressure against the case's rating.
        var materials = ClientMaterials.on(minecraft.level);
        var cas = ReloadingPressBlockEntity.located(materials, menu.getSlot(ReloadingPressBlockEntity.CASE).getItem());
        var charges = new java.util.ArrayList<Cartridge.Charge>();
        for (int slot : new int[]{ReloadingPressBlockEntity.CHARGE, ReloadingPressBlockEntity.CHARGE_B}) {
            var stack = menu.getSlot(slot).getItem();
            var found = ReloadingPressBlockEntity.located(materials, stack);
            if (found.isPresent() && found.get().material().propellant().isPresent())
                charges.add(new Cartridge.Charge(found.get().id(), Math.min(stack.getCount(), FTuning.GUN_CHARGE_MAX)));
        }
        if (cas.isEmpty() || charges.isEmpty())
            return;
        var round = new Cartridge(bore, cas.get().id(), charges, cas.get().id(), java.util.Optional.empty());
        var load = round.load(materials);
        var rating = round.caseRating(materials);
        if (load.isEmpty() || rating.isEmpty()) {
            g.drawString(font, Component.translatable("message.feedback.press.no_blend"), 8, 100, 0xAA0000, false);
            return;
        }
        float peak = load.get().peakPressure();
        g.drawString(font, Component.translatable(load.get().coil() ? "gui.feedback.reloading_press.preview_coil"
                        : "gui.feedback.reloading_press.preview", round.charge(), String.format("%.1f", peak),
                String.format("%.1f", rating.get().value())), 8, 100, peak > rating.get().value() ? 0xAA0000 : 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
