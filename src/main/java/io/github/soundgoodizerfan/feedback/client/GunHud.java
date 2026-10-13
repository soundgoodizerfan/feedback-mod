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
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
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

    private GunHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("gun_ammo"), GunHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
            return;
        ItemStack gun = mc.player.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem))
            return;
        GunDesign design = gun.get(FWeapons.GUN_DESIGN.get());
        if (design == null)
            return;
        Font font = mc.font;
        int right = g.guiWidth() - 12, y = g.guiHeight() - 80;

        int loaded = GunAmmo.count(gun);
        var materials = ClientMaterials.on(mc.level);
        boolean over = GunAmmo.next(gun).flatMap(r -> design.bake(materials, r))
                .map(f -> f.overpressure() > 1).orElse(false);
        int colour = GunHazard.isSpent(gun) || loaded == 0 ? 0xFFAA2222 : over ? 0xFFFF6644 : 0xFFFFFFFF;

        Component count = Component.literal(Integer.toString(loaded)
                + " / " + (FTuning.GUN_MAGAZINE_CAPACITY + 1));
        g.pose().pushPose();
        g.pose().translate(right, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, count, -font.width(count), 0, colour, true);
        g.pose().popPose();

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
        g.drawString(font, reserve, right - font.width(reserve), y + 20, 0xFFCCCCCC, true);

        FireMode mode = gun.getOrDefault(FWeapons.FIRE_MODE.get(), FireMode.AUTO);
        Component modeText = Component.translatable("hud.feedback.gun.mode." + mode.getSerializedName());
        g.drawString(font, modeText, right - font.width(modeText), y + 53, 0xFFCCCCCC, true);

        // A hand can tell warm from hot; the figure is a thermometer's to give (philosophy 8).
        float ignition = GunAmmo.next(gun).flatMap(r -> r.load(materials)).map(l -> l.ignitionTu())
                .orElse(FTuning.GUN_PROPELLANT_IGNITION_TU);
        float heat = GunCondition.heatFraction(gun, mc.level, ignition);
        Component barrel = Component.translatable("hud.feedback.gun.barrel",
                Component.translatable("hud.feedback.gun.heat." + GunCondition.feel(heat)));
        g.drawString(font, barrel, right - font.width(barrel), y + 42,
                heat >= 0.9f ? 0xFFFF5533 : heat >= 0.6f ? 0xFFFFAA44 : 0xFFCCCCCC, true);

        String state = GunHazard.isSpent(gun) ? "spent" : GunClient.reloading() ? "reloading" : loaded == 0 ? "empty" : null;
        if (state != null) {
            Component c = Component.translatable("hud.feedback.gun." + state);
            g.drawString(font, c, right - font.width(c), y + 31, 0xFFFFCC44, true);
        }
    }
}
