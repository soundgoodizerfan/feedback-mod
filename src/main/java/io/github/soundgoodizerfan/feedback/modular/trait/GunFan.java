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
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fan (spec 8.4): a hammer and a cylinder. Crouched, the shooter dumps the cylinder as fast as the hand can slap
 * the hammer -- a rate no cycle limit gives, {@code fan_ms} between shots, held by the attack button -- paid in
 * spread ({@code spread_mult}), kick ({@code kick_mult}) and a hot cylinder ({@code heat_mult}). Simplified: the
 * cost is a flat multiple while fanning, not one that grows with each shot of the streak.
 */
public class GunFan implements ModularTrait {

    /** Whether {@code shooter} is fanning now: crouched. The client's fire loop asks the same. */
    public static boolean fanning(Player shooter) {
        return shooter.isCrouching();
    }

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!fanning(shooter))
            return f;
        return f.withCycleMs(Math.round(p.get("fan_ms", 140f))).withSpread(p.get("spread_mult", 2f))
                .withKick(p.get("kick_mult", 1.3f)).withHeat(p.get("heat_mult", 1.5f));
    }
}
