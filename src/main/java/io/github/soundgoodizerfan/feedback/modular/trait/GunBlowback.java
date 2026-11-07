// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Blowback (spec 8.4): the bolt's mass alone holds the breech shut, so past the pressure it was made for the
 * bolt is thrown back hard. A round that makes more than {@code pressure_limit} times the reference charge's
 * kicks harder in proportion, and wears the bore faster by the excess (misuse, with a cause on the sheet: the
 * pressure is on the spec sheet). Pistol calibres only, by the numbers rather than a rule.
 */
public class GunBlowback implements ModularTrait {

    private static float excess(GunFigures f, Params p) {
        float limit = p.get("pressure_limit", 1.2f) * FTuning.GUN_PROPELLANT_PEAK_PU;
        return Math.max(1f, f.peakPressure().value() / limit);
    }

    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        return f.withKick(excess(f, p));
    }

    @Override
    public void gunFired(ServerPlayer shooter, ItemStack gun, GunFigures f, Params p) {
        float over = excess(f, p) - 1f;
        if (over > 0)
            gun.set(FWeapons.GUN_WEAR.get(), gun.getOrDefault(FWeapons.GUN_WEAR.get(), 0f)
                    + f.muzzleEnergy() * FTuning.GUN_WEAR_PER_WORK * over * p.get("wear_mult", 2f));
    }
}
