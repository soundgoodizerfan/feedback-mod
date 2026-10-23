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

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Counterweight: a dense mass running against the bolt. It cancels kick ({@code kick_mult}) and slows the cycle
 * ({@code cycle_mult}); as the bore wears the stroke is cut short and the mass slaps the receiver, so the kick
 * climbs to {@code slap_kick} at full wear, worse than bare. Read live off the stack so the client predicts it.
 */
public class GunCounterweight implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float kick = Mth.lerp(Math.min(1f, GunCondition.wear(gun)), p.get("kick_mult", 0.85f), p.get("slap_kick", 1.5f));
        return f.withKick(kick).withCycle(p.get("cycle_mult", 1.25f));
    }
}
