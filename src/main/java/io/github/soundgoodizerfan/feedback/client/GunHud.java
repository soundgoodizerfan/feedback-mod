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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.MagazineItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The ammunition readout, bottom right while a gun is held: rounds in the gun (magazine plus
 * chamber), how many of the held gun's bore are carried spare, and a state word -- reloading,
 * empty, spent -- when there is one. The count goes red when the next round asks more of the
 * chamber than it is rated for, so it reads the spec sheet at the moment of firing.
 */
public final class GunHud {

    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private static final GuiStyle.Slide SLIDE = new GuiStyle.Slide(), SLIDE_OFF = new GuiStyle.Slide();
    /** The count, flashing as it changes: a shot, a round loaded. */
    private static final GuiStyle.Flash FLASH = new GuiStyle.Flash(1), FLASH_OFF = new GuiStyle.Flash(1);

    private GunHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("gun_ammo"), GunHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
            return;
        // The main-hand gun reads on the right; a gun in the off hand reads on the left, so two guns have two counters.
        card(g, mc, mc.player.getMainHandItem(), false);
        card(g, mc, mc.player.getOffhandItem(), true);
    }

    private static void card(GuiGraphics g, Minecraft mc, ItemStack gun, boolean left) {
        var slider = left ? SLIDE_OFF : SLIDE;
        int slide = slider.offset(gun.getItem(), 140) * (left ? -1 : 1);
        if (!(gun.getItem() instanceof GunItem))
            return;
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null)
            return;
        Font font = mc.font;
        int edge = left ? 12 : g.guiWidth() - 12, y = g.guiHeight() - 80;

        int loaded = GunAmmo.count(gun);
        var materials = ClientMaterials.on(mc.level);
        boolean over = GunAmmo.next(gun).flatMap(r -> design.firing(GunAmmo.count(gun)).bake(materials, r.at(mc.level.getGameTime())))
                .map(f -> f.overpressure() > 1).orElse(false);
        int colour = GunHazard.isSpent(gun) || loaded == 0 ? SKIN.warn() : over ? 0xFFFF8A3C : SKIN.ink();

        // A magazine gun with no magazine in has no capacity worth a figure: the count alone, and a word below.
        boolean noMagazine = design.internalCapacity().isEmpty() && !gun.has(FWeapons.MAGAZINE_SIZE.get());
        Component count = Component.literal(noMagazine ? Integer.toString(loaded)
                : loaded + " / " + GunAmmo.capacity(gun));
        if (colour == SKIN.ink()) // a warning colour is never masked by the flash
            colour = (left ? FLASH_OFF : FLASH).colour(0, count.getString(), SKIN.ink(), SKIN.accent());

        // Spare rounds of this gun's bore, loose or in magazines in the inventory.
        int spare = 0;
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(FWeapons.MAGAZINE_ITEM.get())) {
                var m = MagazineItem.rounds(s);
                if (m.boreMm() == design.boreMm())
                    spare += m.rounds().size();
            } else if (s.is(FWeapons.CARTRIDGE_ITEM.get())) {
                var c = s.get(FWeapons.CARTRIDGE.get());
                if (c != null && c.boreMm() == design.boreMm())
                    spare += s.getCount();
            }
        }
        Component reserve = Component.translatable("hud.feedback.gun.reserve", spare);
        FireMode mode = GunItem.mode(gun);
        Component modeText = Component.translatable("hud.feedback.gun.mode." + mode.getSerializedName());
        // A hand can tell warm from hot; the figure is a thermometer's to give (philosophy 8).
        float ignition = GunAmmo.next(gun).flatMap(r -> r.load(materials)).map(l -> l.ignitionTu())
                .orElse(FTuning.GUN_PROPELLANT_IGNITION_TU);
        float heat = GunCondition.heatFraction(gun, mc.level, ignition);
        Component barrel = Component.translatable("hud.feedback.gun.barrel",
                Component.translatable("hud.feedback.gun.heat." + GunCondition.feel(heat)));
        String state = GunHazard.isSpent(gun) ? "spent" : GunClient.reloading() ? "reloading"
                : noMagazine ? "no_magazine" : loaded == 0 ? "empty" : null;

        int w = Math.max(2 * font.width(count), Math.max(font.width(reserve), Math.max(font.width(modeText), font.width(barrel))));
        if (state != null)
            w = Math.max(w, font.width(Component.translatable("hud.feedback.gun." + state)));
        g.pose().pushPose();
        g.pose().translate(slide, 0, 0);
        if (left)
            GuiStyle.hudCard(g, edge - 6, y - 6, edge + w + 10, y + 64);
        else
            GuiStyle.hudCard(g, edge - w - 10, y - 6, edge + 6, y + 64);
        g.pose().pushPose();
        g.pose().translate(edge, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, count, left ? 0 : -font.width(count), 0, colour, false);
        g.pose().popPose();
        g.drawString(font, reserve, x(edge, font.width(reserve), left), y + 20, SKIN.dim(), false);
        if (state != null) {
            Component c = Component.translatable("hud.feedback.gun." + state);
            g.drawString(font, c, x(edge, font.width(c), left), y + 31, SKIN.accent(), false);
        }
        g.drawString(font, barrel, x(edge, font.width(barrel), left), y + 42,
                heat >= 0.9f ? SKIN.warn() : heat >= 0.6f ? 0xFFFF8A3C : SKIN.dim(), false);
        g.drawString(font, modeText, x(edge, font.width(modeText), left), y + 53, SKIN.dim(), false);
        g.pose().popPose();
    }

    /** Where text of width {@code w} starts: flush to the right edge, or to the left one. */
    private static int x(int edge, int w, boolean left) {
        return left ? edge : edge - w;
    }
}
