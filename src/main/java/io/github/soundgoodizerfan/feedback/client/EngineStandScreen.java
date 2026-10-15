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

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandMenu;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Engine Stand's editor, v1 ({@code feedback_vehicles_spec.md} section 6): three settings
 * stepped with buttons, the material set by a sample in its slot, and what the design makes read
 * live off the material system. Every
 * edit goes to the stand; Print writes it onto the paper in the slot. Plain filled panels, no
 * texture -- all art is placeholder.
 */
public class EngineStandScreen extends AbstractContainerScreen<EngineStandMenu> {

    private static final int ROW_Y = 18, ROW_H = 16, BUTTON_X = 96, VALUE_X = 116, PLUS_X = 152;

    private EngineDesign design;

    public EngineStandScreen(EngineStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.design = menu.opened();
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 10_000; // no inventory label: only the hotbar is shown
    }

    @Override
    protected void init() {
        super.init();
        row(0, d -> new EngineDesign(d.cylinders() - 1, d.boreMm(), d.strokeMm(), d.material()),
                d -> new EngineDesign(d.cylinders() + 1, d.boreMm(), d.strokeMm(), d.material()));
        row(1, d -> new EngineDesign(d.cylinders(), d.boreMm() - EngineDesign.STEP_MM, d.strokeMm(), d.material()),
                d -> new EngineDesign(d.cylinders(), d.boreMm() + EngineDesign.STEP_MM, d.strokeMm(), d.material()));
        row(2, d -> new EngineDesign(d.cylinders(), d.boreMm(), d.strokeMm() - EngineDesign.STEP_MM, d.material()),
                d -> new EngineDesign(d.cylinders(), d.boreMm(), d.strokeMm() + EngineDesign.STEP_MM, d.material()));
        addRenderableWidget(GuiStyle.button(Component.translatable("gui.feedback.engine_stand.print"), b -> send(true), leftPos + 8, topPos + ROW_Y + 4 * ROW_H + 4, 60, 16));
    }

    /**
     * The sample slot sets the material: the server did it when the sample went in, and the screen
     * follows here so the readout changes at once.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        EngineDesign.sampleMaterial(ClientMaterials.on(minecraft.level), menu.getSlot(1).getItem())
                .filter(m -> !m.equals(design.material()))
                .ifPresent(m -> design = design.withMaterial(m));
    }

    private void row(int index, java.util.function.UnaryOperator<EngineDesign> down,
                     java.util.function.UnaryOperator<EngineDesign> up) {
        int y = topPos + ROW_Y + index * ROW_H;
        addRenderableWidget(GuiStyle.button(Component.literal("-"), b -> edit(down), leftPos + BUTTON_X, y, 16, 14));
        addRenderableWidget(GuiStyle.button(Component.literal("+"), b -> edit(up), leftPos + PLUS_X, y, 16, 14));
    }

    private void edit(java.util.function.UnaryOperator<EngineDesign> change) {
        design = change.apply(design);
        send(false);
    }

    private void send(boolean print) {
        PacketDistributor.sendToServer(new EngineStandPayload(menu.getPos(), design, print));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        GuiStyle.slot(g, leftPos + EngineStandMenu.PAPER_X, topPos + EngineStandMenu.PAPER_Y);
        GuiStyle.slot(g, leftPos + EngineStandMenu.SAMPLE_X, topPos + EngineStandMenu.SAMPLE_Y);
        GuiStyle.hotbar(g, leftPos + EngineStandMenu.HOTBAR_X, topPos + EngineStandMenu.HOTBAR_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String[] names = {"cylinders", "bore", "stroke", "material"};
        Component[] values = {
                Component.literal(String.valueOf(design.cylinders())),
                Component.literal(design.boreMm() + " mm"),
                Component.literal(design.strokeMm() + " mm"),
                Component.translatable("material." + design.material().getNamespace() + "." + design.material().getPath())};
        for (int i = 0; i < 4; i++) {
            int y = ROW_Y + i * ROW_H + 3;
            g.drawString(font, Component.translatable("gui.feedback.engine_stand." + names[i]), 8, y, 0x404040, false);
            if (i < 3)
                g.drawCenteredString(font, values[i], (VALUE_X + PLUS_X) / 2 - 2, y, 0xFFFFFF);
            else // the material sits beside its sample slot
                g.drawString(font, values[i], EngineStandMenu.SAMPLE_X + 20, y + 1, 0x404040, false);
        }
        Material m = ClientMaterials.on(minecraft.level).get(design.material());
        List<Component> lines = design.describe(m);
        for (int i = 1; i < lines.size(); i++) // the first line is the settings, already shown above
            g.drawString(font, lines.get(i), 30, 100 + (i - 1) * 9, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
