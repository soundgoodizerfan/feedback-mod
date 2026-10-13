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

import io.github.soundgoodizerfan.feedback.net.GunStandPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Barrel;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Wall;
import io.github.soundgoodizerfan.feedback.weapon.GunStandMenu;
import io.github.soundgoodizerfan.feedback.weapon.GunTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.UnaryOperator;

/**
 * The Gun Stand's editor ({@code feedback_guns_spec.md} section 4), the Engine Stand's shape:
 * bore stepped, barrel and wall chosen from their bands, the material set by a sample in its
 * slot, and the figures the design makes read live off the material system -- including the
 * chamber's charge against its rating, in red when the metal is asked for too much, so a bad
 * gun is seen on the Stand and not found by firing it. Every edit goes to the stand; Print
 * writes it onto the paper. Plain filled panels -- all art is placeholder.
 */
public class GunStandScreen extends AbstractContainerScreen<GunStandMenu> {

    private static final int ROW_Y = 18, ROW_H = 16, MINUS_X = 140, PLUS_X = 214;

    private GunDesign design;

    public GunStandScreen(GunStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.design = menu.opened();
        imageWidth = 240;
        imageHeight = 220;
        inventoryLabelY = 10_000; // no inventory label: only the hotbar is shown
    }

    @Override
    protected void init() {
        super.init();
        row(0, d -> new GunDesign(d.boreMm() - 1, d.barrel(), d.wall(), d.material(), d.sight()),
                d -> new GunDesign(d.boreMm() + 1, d.barrel(), d.wall(), d.material(), d.sight()));
        row(1, d -> new GunDesign(d.boreMm(), Barrel.values()[Mth.clamp(d.barrel().ordinal() - 1, 0, Barrel.values().length - 1)], d.wall(), d.material(), d.sight()),
                d -> new GunDesign(d.boreMm(), Barrel.values()[Mth.clamp(d.barrel().ordinal() + 1, 0, Barrel.values().length - 1)], d.wall(), d.material(), d.sight()));
        row(2, d -> new GunDesign(d.boreMm(), d.barrel(), Wall.values()[Mth.clamp(d.wall().ordinal() - 1, 0, Wall.values().length - 1)], d.material(), d.sight()),
                d -> new GunDesign(d.boreMm(), d.barrel(), Wall.values()[Mth.clamp(d.wall().ordinal() + 1, 0, Wall.values().length - 1)], d.material(), d.sight()));
        row(3, d -> new GunDesign(d.boreMm(), d.barrel(), d.wall(), d.material(),
                        GunDesign.Sight.values()[Mth.clamp(d.sight().ordinal() - 1, 0, GunDesign.Sight.values().length - 1)]),
                d -> new GunDesign(d.boreMm(), d.barrel(), d.wall(), d.material(),
                        GunDesign.Sight.values()[Mth.clamp(d.sight().ordinal() + 1, 0, GunDesign.Sight.values().length - 1)]));
        addRenderableWidget(Button.builder(Component.translatable("gui.feedback.gun_stand.print"), b -> send(true))
                .bounds(leftPos + 8, topPos + 102, 60, 16).build());
    }

    /** The sample slot sets the material; the server did it on insert, the screen follows so the readout moves at once. */
    @Override
    protected void containerTick() {
        super.containerTick();
        GunDesign.sampleMaterial(ClientMaterials.on(minecraft.level), menu.getSlot(1).getItem())
                .filter(m -> !m.equals(design.material()))
                .ifPresent(m -> design = design.withMaterial(m));
    }

    private void row(int index, UnaryOperator<GunDesign> down, UnaryOperator<GunDesign> up) {
        int y = topPos + ROW_Y + index * ROW_H;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> edit(down)).bounds(leftPos + MINUS_X, y, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> edit(up)).bounds(leftPos + PLUS_X, y, 16, 14).build());
    }

    private void edit(UnaryOperator<GunDesign> change) {
        design = change.apply(design);
        send(false);
    }

    private void send(boolean print) {
        PacketDistributor.sendToServer(new GunStandPayload(menu.getPos(), design, print));
    }

    private void slotBox(GuiGraphics g, int x, int y) {
        g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slotBox(g, GunStandMenu.PAPER_X, GunStandMenu.PAPER_Y);
        slotBox(g, GunStandMenu.SAMPLE_X, GunStandMenu.SAMPLE_Y);
        for (int c = 0; c < 9; c++)
            slotBox(g, GunStandMenu.HOTBAR_X + c * 18, GunStandMenu.HOTBAR_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String[] names = {"bore", "barrel", "wall", "sight", "material"};
        Component[] values = {
                Component.literal(design.boreMm() + " mm"),
                Component.translatable("gun.feedback.barrel." + design.barrel().getSerializedName()),
                Component.translatable("gun.feedback.wall." + design.wall().getSerializedName()),
                Component.translatable("gun.feedback.sight." + design.sight().getSerializedName()),
                Component.translatable("material." + design.material().getNamespace() + "." + design.material().getPath())};
        for (int i = 0; i < 5; i++) {
            int y = ROW_Y + i * ROW_H + 3;
            g.drawString(font, Component.translatable("gui.feedback.gun_stand." + names[i]), 8, y, 0x404040, false);
            if (i < 4)
                g.drawCenteredString(font, values[i], (MINUS_X + 16 + PLUS_X) / 2, y, 0xFFFFFF);
            else // the material sits beside its sample slot
                g.drawString(font, values[i], GunStandMenu.SAMPLE_X + 20, y + 1, 0x404040, false);
        }
        var materials = ClientMaterials.on(minecraft.level);
        int y = 126;
        var figures = design.bake(materials);
        if (figures.isEmpty()) {
            g.drawString(font, Component.translatable("gun.feedback.unknown_material"), 8, y, 0xAA0000, false);
            return;
        }
        for (Component line : GunTooltip.figures(figures.get())) {
            g.drawString(font, line, 8, y, line.getStyle().getColor() != null ? line.getStyle().getColor().getValue() : 0x404040, false);
            y += 9;
        }
        g.drawString(font, Component.translatable("gui.feedback.gun_stand.cost", design.ingots()), 8, y, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
