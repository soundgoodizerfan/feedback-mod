// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec.Seat;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

/**
 * What the wielder's own pulse gun sounds like between shots: a hold charge winding up while the trigger is down, a flywheel's hum
 * rising and falling with its spin, a seat gaining a charge back (a cylinder's chamber, a bank's refill), and a scope zoom coil or a
 * gauge coming on as the sight goes up. Own gun only; the shot and the cell sounds are played where they happen.
 */
public final class EGunSounds {

    private static SoundInstance charge;
    private static Spin spin;
    private static boolean wasAiming;
    private static boolean wasHeld;
    /** The refill reading last frame: chambers of a cylinder, pips of a bank; -1 while there is nothing to compare with. */
    private static float lastCharge = -1f;
    private static int lastSlot = -1;

    private EGunSounds() {
    }

    /** One client frame: {@code gun} is the held gun, or empty. */
    static void tick(Minecraft mc, LocalPlayer player, ItemStack gun, boolean aiming) {
        ElectricGunSpec spec = gun.isEmpty() ? null : ModularElectricGun.specOf(gun);
        // Drawn: the gun powers on; put away or swapped out: it powers off.
        if (spec == null) {
            if (wasHeld)
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FSounds.GUN_EGUN_POWER_OFF.get(), SoundSource.PLAYERS, 0.6f, 1f, false);
            wasHeld = false;
            stopAll(mc);
            return;
        }
        if (!wasHeld)
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FSounds.GUN_EGUN_POWER_ON.get(), SoundSource.PLAYERS, 0.6f, 1f, false);
        wasHeld = true;
        long now = mc.level.getGameTime();
        // A hold charge whines as long as the trigger is down: the whole wind-up is one sound, cut off when the trigger comes up.
        if (spec.holdCharge() && ModularElectricGun.held(gun)) {
            if (charge == null || !mc.getSoundManager().isActive(charge)) {
                if (charge == null || ModularElectricGun.holdShare(gun, now) < 0.2f) {
                    charge = SimpleSoundInstance.forUI(FSounds.GUN_EGUN_CHARGE.get(), 1f, 0.7f);
                    mc.getSoundManager().play(charge);
                }
            }
        } else if (charge != null) {
            mc.getSoundManager().stop(charge);
            charge = null;
        }
        // A flywheel hums as it spins.
        if (spec.seat() == Seat.FLYWHEEL) {
            float s = ModularElectricGun.spin(gun, now, spec);
            Spin.level = s;
            if (s > 0.03f && (spin == null || spin.isStopped())) {
                spin = new Spin(player);
                mc.getSoundManager().play(spin);
            }
        } else
            Spin.level = 0f;
        // A seat gaining a charge back: one more chamber in a cylinder, a bank full again.
        int slot = player.getInventory().selected;
        float have = switch (spec.seat()) {
            case CYLINDER -> (float) Math.floor(ModularElectricGun.chambersShown(gun, now));
            case CAPACITOR_BANK -> ModularElectricGun.pips(gun, now, 1);
            case BURST_BANK -> ModularElectricGun.pips(gun, now, ModularTuning.GUN_BURST_ROUNDS);
            default -> -1f;
        };
        if (have > lastCharge && lastCharge >= 0f && slot == lastSlot)
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FSounds.GUN_EGUN_REFILL.get(), SoundSource.PLAYERS,
                    0.5f, 0.9f + 0.15f * (have / Math.max(1f, ModularTuning.EGUN_CHAMBERS)), false);
        lastCharge = have;
        lastSlot = slot;
        // The sight going up powers the fittings that read it.
        if (aiming && !wasAiming) {
            if (spec.zoom())
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FSounds.GUN_EGUN_ZOOM.get(), SoundSource.PLAYERS, 0.5f, 1f, false);
            if (spec.gauge() || spec.darkAt() > 0f)
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FSounds.GUN_EGUN_BLIP.get(), SoundSource.PLAYERS, 0.5f, 1f, false);
        }
        wasAiming = aiming;
    }

    private static void stopAll(Minecraft mc) {
        if (charge != null) {
            mc.getSoundManager().stop(charge);
            charge = null;
        }
        Spin.level = 0f;
        lastCharge = -1f;
        wasAiming = false;
    }

    /** The flywheel's hum: one looping sound, its pitch and level read off the spin each tick, ended when the wheel stops. */
    private static final class Spin extends AbstractTickableSoundInstance {
        static float level;
        private final LocalPlayer player;

        Spin(LocalPlayer player) {
            super(FSounds.GUN_EGUN_SPIN.get(), SoundSource.PLAYERS, RandomSource.create());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0f;
            this.pitch = 0.6f;
            sync();
        }

        private void sync() {
            this.x = player.getX();
            this.y = player.getEyeY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (level <= 0.02f || player.isRemoved()) {
                stop();
                return;
            }
            sync();
            this.volume = 0.1f + 0.5f * level;
            this.pitch = 0.6f + 1.0f * level;
        }
    }
}
