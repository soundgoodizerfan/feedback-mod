// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.MagazineItem;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
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

    public static void register(RegisterGuiLayersEvent event) {
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
        // A hand can tell warm from hot; the figure is a thermometer's to give (philosophy 10).
        float ignition = GunAmmo.next(gun).flatMap(r -> r.load(materials)).map(l -> l.ignitionTu())
                .orElse(FTuning.GUN_PROPELLANT_IGNITION_TU);
        float heat = GunCondition.heatFraction(gun, mc.level, ignition);
        Component barrel = Component.translatable("hud.feedback.gun.barrel",
                Component.translatable("hud.feedback.gun.heat." + GunCondition.feel(heat)));
        String state = GunHazard.isSpent(gun) ? "spent" : GunClient.reloading() ? "reloading"
                : noMagazine ? "no_magazine" : loaded == 0 ? "empty" : null;

        draw(g, font, left, slide, edge, y, count, colour, reserve, state, barrel, heat, modeText);
    }

    private static final int CARD_W = 76, DARK = 0xFF202428;
    private static final net.minecraft.resources.ResourceLocation VIAL = Feedback.id("textures/gui/hud/vial.png"),
            CELL = Feedback.id("textures/gui/hud/cell.png"), THERMO = Feedback.id("textures/gui/hud/thermo.png"),
            BATTERY = Feedback.id("textures/gui/hud/battery.png");

    /**
     * A pulse gun's card, one instrument cluster: a battery icon and its charge, the seat's readout under it (a glass vial for each
     * chamber of a cylinder, a capacitor cell for each step of a bank or a flywheel's spin), a thermometer for the barrel's heat down the
     * right (with a percentage under a heat gauge), and the state and mode below with a lamp.
     */
    private static void electricCard(GuiGraphics g, Minecraft mc, ItemStack gun, boolean left, int slide) {
        Font font = mc.font;
        var spec = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.specOf(gun);
        long game = mc.level.getGameTime();
        int edge = left ? 12 : g.guiWidth() - 12, y = g.guiHeight() - 80;
        boolean tether = spec != null && spec.seat() == io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat.BACKPACK_TETHER;
        float charge = tether ? tetherCharge(mc, spec) : io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.chargeFraction(gun);
        Component count = Component.literal(charge < 0f ? "--" : Math.round(charge * 100f) + "%");
        float heat = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.heatFraction(gun, game);
        Component modeText = Component.translatable("hud.feedback.gun.mode." + GunItem.mode(gun).getSerializedName());
        String state = charge < 0f ? (tether ? "no_tether" : "no_cell") : charge <= 0f ? "flat" : null;
        int w = CARD_W, x0 = left ? edge : edge - w;
        g.pose().pushPose();
        g.pose().translate(slide, 0, 0);
        GuiStyle.hudCard(g, x0 - 8, y - 7, x0 + w + 8, y + 54);
        // The battery and its charge: a cell filled to the charge, green while it is well filled, amber then red as it runs down.
        int cellColour = charge > 0.5f ? 0xFF40FF60 : charge > 0.2f ? 0xFFFFB030 : 0xFFFF4030;
        g.fill(x0 + 2, y + 3, x0 + 10, y + 7, DARK);
        if (charge > 0f)
            g.fill(x0 + 2, y + 3, x0 + 2 + Math.max(1, Math.round(8 * Mth.clamp(charge, 0f, 1f))), y + 7, cellColour);
        g.blit(BATTERY, x0, y + 1, 0, 0, 14, 8, 14, 8);
        if (state != null) {
            // With no charge to read, the state word takes the figure's place.
            Component c = Component.translatable("hud.feedback.gun." + state);
            g.drawString(font, c, x0 + 18, y + 1, SKIN.accent(), false);
        } else {
            g.pose().pushPose();
            g.pose().translate(x0 + 18, y, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawString(font, count, 0, 0, SKIN.ink(), false);
            g.pose().popPose();
        }
        if (spec != null)
            seatReadout(g, spec, gun, game, x0, y + 20, w - 14);
        thermometer(g, heat, x0 + w - 9, y);
        // The mode, with a lamp: lit while the gun can fire, red when it cannot; a heat gauge's figure at the other end.
        g.fill(x0, y + 40, x0 + 4, y + 44, state != null ? 0xFFFF4030 : 0xFF40FF60);
        g.drawString(font, modeText, x0 + 8, y + 39, SKIN.dim(), false);
        if (spec != null && spec.gauge()) {
            String pct = Math.round(heat * 100f) + "%";
            g.drawString(font, pct, x0 + w - 12 - font.width(pct), y + 39, heat >= 0.9f ? SKIN.warn() : SKIN.dim(), false);
        }
        g.pose().popPose();
    }

    /** The colour of a lit lamp: its own, or the hold or pump charge's state (green, yellow, red, blinking red) when one is fitted. */
    private static int lamp(io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec spec, ItemStack gun, long game, int plain) {
        float c = -1f;
        if (spec.holdCharge())
            c = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.holdShare(gun, game);
        if (spec.pump())
            c = Math.max(c, io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.pumps(gun)
                    / (float) io.github.soundgoodizerfan.feedback.modular.ModularTuning.EGUN_PUMP_MAX);
        return io.github.soundgoodizerfan.feedback.client.gun.ChargeColour.of(c, plain);
    }

    /** A tethered gun's source: a suit's bay battery, else the pack's fitting, as the share charged; -1 with none. */
    private static float tetherCharge(Minecraft mc, io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec spec) {
        var src = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.source(mc.player, mc.player.getMainHandItem(), spec);
        return src.battery().isEmpty() ? -1f
                : io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.stateOfCharge(io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.state(src.battery()));
    }

    /** What the seat holds: a glass vial for each chamber of a cylinder, a capacitor cell for each of six steps of a bank's charge or a flywheel's spin. */
    private static void seatReadout(GuiGraphics g, io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec spec, ItemStack gun, long game,
                                    int x, int y, int span) {
        switch (spec.seat()) {
            case CYLINDER -> {
                float held = io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.chambersShown(gun, game);
                int n = io.github.soundgoodizerfan.feedback.modular.ModularTuning.EGUN_CHAMBERS, full = lamp(spec, gun, game, 0xFF40FF60);
                for (int i = 0; i < n; i++) {
                    int vx = x + Math.round(i * (span - 7) / 5f);
                    float fill = Mth.clamp(held - i, 0f, 1f);
                    g.fill(vx + 2, y + 3, vx + 5, y + 11, DARK);
                    int rows = Math.round(8 * fill);
                    if (rows > 0)
                        g.fill(vx + 2, y + 11 - rows, vx + 5, y + 11, fill >= 1f ? full : 0xFFFFB030);
                    g.blit(VIAL, vx, y, 0, 0, 7, 14, 7, 14);
                }
            }
            case CAPACITOR_BANK, BURST_BANK, CORE_BREECH, FLYWHEEL -> {
                float share = spec.seat() == io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat.FLYWHEEL
                        ? io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.spinShown(gun, game)
                        : io.github.soundgoodizerfan.feedback.modular.ModularElectricGun.bankShown(gun, game);
                int lit = lamp(spec, gun, game, spec.seat() == io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat.FLYWHEEL ? 0xFFE0E0E8 : 0xFF40C8FF);
                for (int i = 0; i < 6; i++) {
                    int cx = x + Math.round(i * (span - 9) / 5f);
                    float fill = Mth.clamp(share * 6f - i, 0f, 1f);
                    g.fill(cx + 2, y + 3, cx + 7, y + 9, DARK);
                    int rows = Math.round(6 * fill);
                    if (rows > 0)
                        g.fill(cx + 2, y + 9 - rows, cx + 7, y + 9, fill >= 1f ? lit : 0xFF2A6C90);
                    g.blit(CELL, cx, y, 0, 0, 9, 12, 9, 12);
                }
            }
            default -> {
            }
        }
    }

    /** The barrel's heat as a thermometer: the bulb always full, the column rising and going from cool to red, shimmering near overheat. */
    private static void thermometer(GuiGraphics g, float heat, int x, int y) {
        int colour = heat >= 0.9f ? (System.currentTimeMillis() / 200L % 2L == 0L ? 0xFFFF4030 : 0xFFFF8A6C) : heat >= 0.6f ? 0xFFFF8A3C : 0xFF40C8FF;
        g.fill(x + 3, y + 2, x + 6, y + 40, DARK);
        g.fill(x + 2, y + 40, x + 7, y + 45, DARK);
        int rows = Math.round(38 * Mth.clamp(heat, 0f, 1f));
        g.fill(x + 2, y + 40, x + 7, y + 45, colour);
        if (rows > 0)
            g.fill(x + 3, y + 40 - rows, x + 6, y + 40, colour);
        g.blit(THERMO, x, y, 0, 0, 9, 47, 9, 47);
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
