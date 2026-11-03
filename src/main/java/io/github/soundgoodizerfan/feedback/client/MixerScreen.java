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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.mixer.MixerMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Mixture;
import io.github.soundgoodizerfan.feedback.weapon.CartridgeItem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Mixer: input slots, a draw button and the drawn item, the tank, and the drum read as an open drum is read by eye: what is in
 * it as rough shares (quarters, never a figure), how mixed it is in words, friction heat through the instrument seam.
 */
public class MixerScreen extends SkinnedScreen<MixerMenu> {

    private static final int SHARE_X = 90, SHARE_W = 70;

    public MixerScreen(MixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.STONE);
        titleLabelX = 12;
        titleLabelY = 6;
        imageWidth = 176;
        imageHeight = MixerMenu.INV_Y + 58 + 18 + 8;
        inventoryLabelY = MixerMenu.INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.mixer.draw"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MixerMenu.DRAW),
                leftPos + MixerMenu.SLOTS_X, topPos + 42, 72, 14));
    }

    /** Material and its parts (items and fluids as whole parts), biggest first. */
    private List<Mixture.FluidPart> fluidParts() {
        var mixer = menu.mixer();
        List<Mixture.FluidPart> out = new ArrayList<>();
        if (mixer != null)
            mixer.fluidParts().forEach((m, mb) -> out.add(new Mixture.FluidPart(m, mb)));
        return out;
    }

    private List<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge> parts() {
        var mixer = menu.mixer();
        List<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge> out = new ArrayList<>();
        if (mixer == null)
            return out;
        out.addAll(mixer.contents());
        fluidParts().forEach(f -> out.add(f.asCharge()));
        out.sort(java.util.Comparator.comparingInt(io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge::count).reversed());
        return out;
    }

    private int tankColour() {
        if (menu.productMb() > 0)
            return IClientFluidTypeExtensions.of(menu.productFluid()).getTintColor() | 0xFF000000;
        var biggest = fluidParts().stream().max(java.util.Comparator.comparingInt(Mixture.FluidPart::mb));
        if (biggest.isPresent()) {
            var material = ClientMaterials.get().get(biggest.get().material());
            if (material != null && material.color().isPresent())
                return material.color().get() | 0xFF000000;
        }
        return skin.accent();
    }

    private Component uniformity() {
        float u = menu.uniformityPermille() / 1000f;
        String key = menu.overmixed() ? "feedback.mixer.over" : u < 0.34f ? "feedback.mixer.unmixed" : u < 0.9f ? "feedback.mixer.partly"
                : u < 0.999f ? "feedback.mixer.settling" : "feedback.mixer.mixed";
        return Component.translatable(key);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (int slot = 0; slot < FTuning.MIXER_INPUT_SLOTS; slot++)
            skin.slot(graphics, leftPos + MixerMenu.SLOTS_X + slot * MixerMenu.SLOT_PX, topPos + MixerMenu.SLOTS_Y);
        skin.slot(graphics, leftPos + MixerMenu.OUT_X, topPos + MixerMenu.SLOTS_Y);

        int colour = tankColour();
        double cap = FTuning.GUN_CHARGE_MAX * FTuning.MIXER_MB_PER_PART;
        Gauge.of(() -> (menu.productMb() > 0 ? menu.productMb() : menu.fluidMb()) / cap).lead(() -> colour).vertical()
                .render(graphics, skin, leftPos + MixerMenu.TANK_X, topPos + MixerMenu.TANK_Y, MixerMenu.TANK_W, MixerMenu.TANK_H);

        var parts = parts();
        int total = parts.stream().mapToInt(c -> c.count()).sum();
        for (int row = 0; row < Math.min(parts.size(), 4); row++) {
            // Quarters: a bar that reads a recipe off a screen would be a recipe read off a screen.
            double share = Math.max(1, Math.round(parts.get(row).count() / (double) total * 4)) / 4.0;
            Gauge.of(() -> share).ticks(4).render(graphics, skin, leftPos + SHARE_X, topPos + MixerMenu.ROWS_Y + row * MixerMenu.ROW_PX + 1,
                    SHARE_W, 7);
        }
        inventory(graphics, 8, MixerMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        var parts = parts();
        if (parts.isEmpty()) {
            graphics.drawString(font, Component.translatable("feedback.mixer.empty"), 12, MixerMenu.ROWS_Y, skin.dim(), false);
            return;
        }
        for (int row = 0; row < Math.min(parts.size(), 4); row++)
            graphics.drawString(font, font.plainSubstrByWidth(CartridgeItem.name(parts.get(row).material()).getString(), SHARE_X - 16),
                    12, MixerMenu.ROWS_Y + row * MixerMenu.ROW_PX, skin.ink(), false);
        graphics.drawString(font, uniformity(), 12, MixerMenu.ROWS_Y + 4 * MixerMenu.ROW_PX + 2, menu.overmixed() ? skin.warn() : skin.ink(), false);
        if (menu.heatTu() > 0)
            graphics.drawString(font, Component.translatable("feedback.mixer.heat", Readout.temperatureReading(new Tu(menu.temperatureTu()))),
                    12, MixerMenu.ROWS_Y + 4 * MixerMenu.ROW_PX + 13, skin.warn(), false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        if (mouseX < leftPos + MixerMenu.TANK_X || mouseX >= leftPos + MixerMenu.TANK_X + MixerMenu.TANK_W
                || mouseY < topPos + MixerMenu.TANK_Y || mouseY >= topPos + MixerMenu.TANK_Y + MixerMenu.TANK_H)
            return;
        Component line;
        if (menu.productMb() > 0)
            line = new FluidStack(menu.productFluid(), 1).getHoverName();
        else if (menu.fluidMb() > 0) {
            var fluids = fluidParts();
            var biggest = fluids.stream().max(java.util.Comparator.comparingInt(Mixture.FluidPart::mb));
            ResourceLocation material = biggest.map(Mixture.FluidPart::material).orElse(null);
            float share = menu.fluidMb() / (float) (FTuning.GUN_CHARGE_MAX * FTuning.MIXER_MB_PER_PART);
            line = material == null ? Component.empty() : Component.translatable(
                    "tooltip.feedback.mixture." + (share >= 0.5f ? "mostly" : share >= 0.2f ? "some" : "little"), CartridgeItem.name(material));
        } else
            line = Component.translatable("tooltip.feedback.crucible.empty");
        graphics.renderTooltip(font, List.of(line), Optional.empty(), mouseX, mouseY);
    }
}
