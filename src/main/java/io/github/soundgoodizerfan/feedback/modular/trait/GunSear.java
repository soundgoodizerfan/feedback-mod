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

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Magnetic sear: a permanent magnet holds the sear, so the trigger breaks clean and the sear resets fast, and nothing
 * draws power waiting. The magnet's pull falls from full at the room to nothing at its Curie temperature, read off the
 * barrel's heat: spread tightens by {@code spread_cut} and the cycle quickens by {@code cycle_cut}, times the pull. A
 * non-magnetic stuff has no pull and does nothing.
 */
public class GunSear implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var m = GunFitting.of(gun, "magnetic_sear");
        if (m == null || !m.separation().magnetic())
            return f;
        float ambient = AmbientTable.at(shooter.level()).value();
        float curie = m.separation().curieTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE);
        float hold = Math.max(0f, 1f - (GunCondition.temperature(gun, shooter.level()).value() - ambient) / Math.max(1f, curie - ambient));
        return f.withSpread(1f - p.get("spread_cut", 0.15f) * hold).withCycle(1f - p.get("cycle_cut", 0.1f) * hold);
    }
}
