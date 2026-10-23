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
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Thermal fit: a sleeve shrunk onto the barrel. Cold it sits loose and the group opens ({@code cold_spread});
 * as the barrel heats the sleeve closes on it and the group tightens to {@code tight_spread} at {@code fit_at}
 * (a share of the way from the room to the round's ignition); past {@code bind_at} it grips the bore and squeezes
 * the chamber's rating down to {@code bind_rating} at cook-off. Read live off the stack, so the client predicts
 * the shot. The sleeve's metal does not matter: shortcut, one expansion for every metal, split it when metals differ.
 */
public class GunThermalFit implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float ambient = AmbientTable.at(shooter.level()).value();
        float heat = Math.max(0f, (GunCondition.temperature(gun, shooter.level()).value() - ambient)
                / (FTuning.GUN_PROPELLANT_IGNITION_TU - ambient));
        float fitAt = p.get("fit_at", 0.4f), bindAt = p.get("bind_at", 0.7f);
        float spread = heat < fitAt
                ? Mth.lerp(heat / fitAt, p.get("cold_spread", 1.3f), p.get("tight_spread", 0.6f))
                : p.get("tight_spread", 0.6f);
        f = f.withSpread(spread);
        if (heat > bindAt)
            f = f.withRating(Mth.lerp(Math.min(1f, (heat - bindAt) / (1f - bindAt)), 1f, p.get("bind_rating", 0.8f)));
        return f;
    }
}
