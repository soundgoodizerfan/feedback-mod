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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
