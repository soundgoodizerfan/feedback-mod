// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
import net.minecraft.util.Mth;
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

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final GuiStyle.Slide SLIDE = new GuiStyle.Slide(), SLIDE_OFF = new GuiStyle.Slide();
    /** The count, flashing as it changes: a shot, a round loaded. */
    private static final GuiStyle.Flash FLASH = new GuiStyle.Flash(1), FLASH_OFF = new GuiStyle.Flash(1);

    /** A shot gone wrong (a flash in the pan, a cook-off, a chain fire), said on the card in place of its state line. */
    private static Component cue = Component.empty();
    private static long cueBorn, cueUntil;

    private GunHud() {
    }

    public static void cue(Component text, int ticks) {
        cue = text;
        cueBorn = net.minecraft.Util.getMillis();
        cueUntil = cueBorn + ticks * 50L;
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
        if (gun.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularElectricGun) {
            electricCard(g, mc, gun, left, slide);
            return;
        }
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null)
            return;
        Font font = mc.font;
        int edge = left ? 12 : g.guiWidth() - 12, y = g.guiHeight() - 80;

        int loaded = GunAmmo.count(gun);
        var materials = ClientMaterials.on(mc.level);
        boolean over = GunAmmo.next(gun).flatMap(r -> design.firing(GunAmmo.count(gun)).bake(materials, r))
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

        draw(g, font, left, slide, edge, y, count, colour, reserve, state, barrel, heat, modeText);
    }

    /**
     * A pulse gun's card: the battery's level where a firearm shows its rounds, the seat's charge (six pips for a cylinder, a bar for a bank
     * or a flywheel's spin), the barrel's heat as a bar (finer, with a scale, under a heat gauge) and the mode.
     */
    private static void electricCard(GuiGraphics g, Minecraft mc, ItemStack gun, boolean left, int slide) {
        Font font = mc.font;
        var spec = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.specOf(gun);
        long game = mc.level.getGameTime();
        int edge = left ? 12 : g.guiWidth() - 12, y = g.guiHeight() - 80;
        float charge = spec != null && spec.seat() == io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat.BACKPACK_TETHER
                ? tetherCharge(mc, spec) : io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.chargeFraction(gun);
        Component count = Component.literal(charge < 0f ? "--" : Math.round(charge * 100f) + "%");
        int colour = charge <= 0f ? SKIN.warn() : SKIN.ink();
        float heat = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.heatFraction(gun, game);
        Component modeText = Component.translatable("hud.feedback.gun.mode." + GunItem.mode(gun).getSerializedName());
        String state = charge < 0f ? (spec != null && spec.seat() == io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat.BACKPACK_TETHER
                ? "no_tether" : "no_cell") : charge <= 0f ? "flat" : null;
        int w = Math.max(70, Math.max(2 * font.width(count), font.width(modeText)));
        g.pose().pushPose();
        g.pose().translate(slide, 0, 0);
        if (left)
            GuiStyle.hudCard(g, edge - 6, y - 6, edge + w + 10, y + 64);
        else
            GuiStyle.hudCard(g, edge - w - 10, y - 6, edge + 6, y + 64);
        int x0 = left ? edge : edge - w;
        g.pose().pushPose();
        g.pose().translate(edge, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, count, left ? 0 : -font.width(count), 0, colour, false);
        g.pose().popPose();
        if (spec != null)
            seatReadout(g, spec, gun, game, x0, y + 20, w);
        if (state != null) {
            Component c = Component.translatable("hud.feedback.gun." + state);
            g.drawString(font, c, x(edge, font.width(c), left), y + 31, SKIN.accent(), false);
        }
        heatBar(g, font, heat, spec != null && spec.gauge(), x0, y + 42, w);
        g.drawString(font, modeText, x(edge, font.width(modeText), left), y + 53, SKIN.dim(), false);
        g.pose().popPose();
    }

    /** A tethered gun's source: a suit's bay battery, else the pack's fitting, as the share charged; -1 with none. */
    private static float tetherCharge(Minecraft mc, io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec spec) {
        var src = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.source(mc.player, mc.player.getMainHandItem(), spec);
        return src.battery().isEmpty() ? -1f
                : io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.stateOfCharge(io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.state(src.battery()));
    }

    /** What the seat holds: six pips for a cylinder, a bar for a bank (its charge) or a flywheel (its spin). */
    private static void seatReadout(GuiGraphics g, io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec spec, ItemStack gun, long game,
                                    int x, int y, int w) {
        switch (spec.seat()) {
            case CYLINDER -> {
                float held = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.chambersShown(gun, game);
                int n = io.github.soundgoodizerfan.feedback.modular.ModularTuning.EGUN_CHAMBERS, pw = (w - 2 * (n - 1)) / n;
                for (int i = 0; i < n; i++) {
                    int px = x + i * (pw + 2);
                    g.fill(px, y, px + pw, y + 8, 0xFF202428);
                    float fill = Mth.clamp(held - i, 0f, 1f);
                    if (fill > 0f)
                        g.fill(px, y + 8 - Math.round(8 * fill), px + pw, y + 8, fill >= 1f ? 0xFF40FF60 : 0xFFFFB030);
                }
            }
            case CAPACITOR_BANK, BURST_BANK -> bar(g, x, y, w, 8, io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.bankShown(gun, game), 0xFF40C8FF);
            case FLYWHEEL -> bar(g, x, y, w, 8, io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.spinShown(gun, game), 0xFFE0E0E8);
            default -> {
            }
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float fill, int colour) {
        g.fill(x, y, x + w, y + h, 0xFF202428);
        g.fill(x, y, x + Math.round(w * Mth.clamp(fill, 0f, 1f)), y + h, colour);
    }

    /** The barrel's heat as a bar that goes from cool to red; with a heat gauge it carries a scale and the figure. */
    private static void heatBar(GuiGraphics g, Font font, float heat, boolean gauge, int x, int y, int w) {
        int colour = heat >= 0.9f ? 0xFFFF4030 : heat >= 0.6f ? 0xFFFF8A3C : 0xFF40C8FF;
        bar(g, x, y, w, gauge ? 8 : 5, heat, colour);
        if (gauge) {
            for (int i = 1; i < 10; i++)
                g.fill(x + w * i / 10, y, x + w * i / 10 + 1, y + (i == 5 ? 8 : 4), 0xAA000000);
            g.drawString(font, Math.round(heat * 100f) + "%", x + w + 3, y, SKIN.dim(), false);
        }
    }

    private static void draw(GuiGraphics g, Font font, boolean left, int slide, int edge, int y, Component count, int colour,
                             Component reserve, String state, Component barrel, float heat, Component modeText) {
        int w = Math.max(2 * font.width(count), Math.max(font.width(reserve), Math.max(font.width(modeText), font.width(barrel))));
        if (state != null)
            w = Math.max(w, font.width(Component.translatable("hud.feedback.gun." + state)));
        if (cueUntil > net.minecraft.Util.getMillis())
            w = Math.max(w, font.width(cue));
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
        long now = net.minecraft.Util.getMillis(), cueLeft = cueUntil - now;
        if (cueLeft > 0) {
            // Blinks for its first half second, holds, then fades over its last third of a second.
            boolean dark = now - cueBorn < 500 && (now - cueBorn) / 100 % 2 == 1;
            int alpha = Math.round(Math.min(1f, cueLeft / 300f) * 255);
            if (alpha > 8 && !dark)
                g.drawString(font, cue, x(edge, font.width(cue), left), y + 31, alpha << 24 | SKIN.warn() & 0xFFFFFF, false);
        } else if (state != null) {
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
