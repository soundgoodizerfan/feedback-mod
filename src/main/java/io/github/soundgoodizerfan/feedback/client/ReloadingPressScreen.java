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
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Reloading Press ({@code feedback_guns_spec.md} section 5): case, charge and projectile in,
 * a round out. The die set is the bore, stepped; the lever seats whatever is in the slots. The
 * line under the slots is the round's own spec sheet, read as it would be seated now -- it is the
 * press's only help, because the press does not know when a charge is enough. Iron and brass
 * ({@link GuiSkin#FORGE}): the die set is a stamped plaque, the seated pressure a bar against the
 * case's rating, red past it.
 */
public class ReloadingPressScreen extends SkinnedScreen<ReloadingPressMenu> {

    private static final int DIE_X = 110, DIE_Y = 30, BAR_Y = 124;

    private int bore;
    private final GuiStyle.Flash flash = new GuiStyle.Flash(1);
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
        addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> setBore(bore - 1), leftPos + DIE_X, topPos + DIE_Y,
                14, 12, FSounds.UI_DETENT::get, 0.9f));
        addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> setBore(bore + 1), leftPos + DIE_X + 42, topPos + DIE_Y,
                14, 12, FSounds.UI_DETENT::get, 1.1f));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.reloading_press.seat"), b -> send(true),
                leftPos + DIE_X, topPos + 76, 56, 16, () -> SoundEvents.PISTON_EXTEND));
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
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int[] xy : ReloadingPressMenu.SLOT_XY)
            skin.slot(g, leftPos + xy[0], topPos + xy[1]);
        for (int c = 0; c < 9; c++)
            skin.slot(g, leftPos + ReloadingPressMenu.HOTBAR_X + c * 18, topPos + ReloadingPressMenu.HOTBAR_Y);
        // The die plaque between its steppers: a brass plate, the bore stamped in.
        int px = leftPos + DIE_X + 15, py = topPos + DIE_Y - 1;
        g.fill(px, py, px + 26, py + 14, 0xFF5E4520);
        g.renderOutline(px, py, 26, 14, 0xFFC8A054);
        // The line the round is fed along, components to the press.
        int ly = topPos + ReloadingPressMenu.SLOT_XY[5][1] + 8;
        g.fill(leftPos + 106, topPos + 28, leftPos + 107, topPos + 96, 0x40E8C878);
        g.fill(leftPos + 128, ly, leftPos + 146, ly + 1, 0x60E8C878);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String die = bore + " mm";
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.die"), DIE_X, DIE_Y - 10, skin.dim(), false);
        g.drawCenteredString(font, die, DIE_X + 28, DIE_Y + 2, flash.colour(0, die, skin.ink(), skin.accent()));
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
        int x = 12, w = imageWidth - 24, fill = Math.round(Math.min(gauge, 1.5f) / 1.5f * w), mark = w * 2 / 3;
        g.fill(x, BAR_Y, x + w, BAR_Y + 4, 0xFF0E0C0B);
        g.renderOutline(x - 1, BAR_Y - 1, w + 2, 6, 0xFF8C6A32);
        g.fill(x, BAR_Y, x + Math.min(fill, mark), BAR_Y + 4, 0xFFE8D9B0);
        if (fill > mark)
            g.fill(x + mark, BAR_Y, x + fill, BAR_Y + 4, skin.warn());
        g.fill(x + mark, BAR_Y - 2, x + mark + 1, BAR_Y + 6, skin.accent());
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
