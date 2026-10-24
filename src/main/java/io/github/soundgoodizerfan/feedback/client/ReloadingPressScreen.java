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
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunCalibres;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Reloading Press ({@code docs/feedback_guns_spec.md} section 5): case, charge and projectile in,
 * a round out. The die set is the bore, stepped; the lever seats whatever is in the slots. The
 * line under the slots is the round's own spec sheet, read as it would be seated now -- it is the
 * press's only help, because the press does not know when a charge is enough. Iron and brass
 * ({@link GuiSkin#FORGE}): the die set is a stamped plaque, the seated pressure a bar against the
 * case's rating, red past it.
 */
public class ReloadingPressScreen extends SkinnedScreen<ReloadingPressMenu> {

    private static final int DIE_X = 110, DIE_Y = 30, BAR_Y = 124;

    private int bore;
    /** The pressure bar as drawn, easing toward the figure; negative until there is one. */
    private float gauge = -1;
    private long lastFrame;

    public ReloadingPressScreen(ReloadingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        this.bore = menu.openedBore();
        imageWidth = 176;
        imageHeight = 184;
        titleLabelX = 12;
        inventoryLabelY = 10_000; // only the hotbar is shown
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + DIE_X, topPos + DIE_Y - 1, 56, 14, "<", ">",
                () -> Component.translatable("modular.feedback.variant." + GunCalibres.nearest(bore).name()).getString() + ", " + bore + " mm", dir -> setBore(GunCalibres.step(bore, dir).boreMm()), true, FSounds.UI_DETENT::get));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.reloading_press.seat"), b -> send(true),
                leftPos + DIE_X, topPos + 76, 56, 16, () -> SoundEvents.PISTON_EXTEND));
    }

    private void setBore(int value) {
        bore = GunCalibres.nearest(value).boreMm();
        send(false);
    }

    private void send(boolean seat) {
        PacketDistributor.sendToServer(new PressPayload(menu.getPos(), bore, seat));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int[] xy : ReloadingPressMenu.SLOT_XY)
            skin.slot(g, leftPos + xy[0], topPos + xy[1]);
        for (int c = 0; c < 9; c++)
            skin.slot(g, leftPos + ReloadingPressMenu.HOTBAR_X + c * 18, topPos + ReloadingPressMenu.HOTBAR_Y);
        // The line the round is fed along, components to the press.
        int ly = topPos + ReloadingPressMenu.SLOT_XY[5][1] + 8;
        g.fill(leftPos + 106, topPos + 28, leftPos + 107, topPos + 96, 0x40E8C878);
        g.fill(leftPos + 128, ly, leftPos + 146, ly + 1, 0x60E8C878);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.die"), DIE_X, DIE_Y - 10, skin.dim(), false);
        String[] names = {"case", "charge", null, "projectile", "payload"};
        for (int i = 0; i < names.length; i++)
            if (names[i] != null)
                g.drawString(font, Component.translatable("gui.feedback.reloading_press." + names[i]),
                        12, ReloadingPressMenu.SLOT_XY[i][1] + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.round"),
                DIE_X, ReloadingPressMenu.SLOT_XY[5][1] + 4, skin.dim(), false);

        long now = net.minecraft.Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100L) / 1000f;
        lastFrame = now;
        // The round as it would be seated now: the blend's pressure against the case's rating.
        var materials = ClientMaterials.on(minecraft.level);
        var cas = ReloadingPressBlockEntity.located(materials, menu.getSlot(ReloadingPressBlockEntity.CASE).getItem());
        var charges = new java.util.ArrayList<Cartridge.Charge>();
        for (int slot : new int[]{ReloadingPressBlockEntity.CHARGE, ReloadingPressBlockEntity.CHARGE_B}) {
            var stack = menu.getSlot(slot).getItem();
            if (stack.has(io.github.soundgoodizerfan.feedback.registry.FWeapons.BLEND.get())) {
                charges.addAll(stack.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.BLEND.get()));
                continue;
            }
            var found = ReloadingPressBlockEntity.located(materials, stack);
            if (found.isPresent() && found.get().material().propellant().isPresent())
                charges.add(new Cartridge.Charge(found.get().id(), Math.min(stack.getCount(), FTuning.GUN_CHARGE_MAX)));
        }
        if (cas.isEmpty() || charges.isEmpty()) {
            gauge = -1;
            return;
        }
        var round = new Cartridge(bore, cas.get().id(), charges, cas.get().id(), java.util.Optional.empty());
        var load = round.load(materials);
        var rating = round.caseRating(materials);
        if (load.isEmpty() || rating.isEmpty()) {
            text(g, Component.translatable("message.feedback.press.no_blend"), skin.warn());
            gauge = -1;
            return;
        }
        float peak = load.get().peakPressure(), limit = rating.get().value();
        text(g, Component.translatable(load.get().coil() ? "gui.feedback.reloading_press.preview_coil"
                        : "gui.feedback.reloading_press.preview", round.charge(), String.format("%.1f", peak),
                String.format("%.1f", limit)), peak > limit ? skin.warn() : skin.ink());
        // The bar: full scale is half again the rating, the rating the tick, past it red.
        float target = limit > 0 ? peak / limit : 1.5f;
        gauge = gauge < 0 ? target : gauge + (target - gauge) * Math.min(1, dt * 10);
        skin.bar(g, 12, BAR_Y, imageWidth - 24, 4, gauge / 1.5f, 2 / 3f);
    }

    /** The spec line, wrapped, above the bar. */
    private void text(GuiGraphics g, Component line, int colour) {
        int y = 102;
        for (var l : font.split(line, imageWidth - 24)) {
            g.drawString(font, l, 12, y, colour, false);
            y += 9;
        }
    }
}
