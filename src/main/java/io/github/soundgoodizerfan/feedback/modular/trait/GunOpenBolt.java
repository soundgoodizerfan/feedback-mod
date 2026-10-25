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

import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Open bolt (spec 8.4): the gun fires from an open bolt, so there is no round in the hot chamber to cook off
 * ({@link io.github.soundgoodizerfan.feedback.modular.ModularTraits#cooksOff}) and the air cools it, paid in a
 * poorer first shot: after {@code idle_ms} without firing the bolt has to slam forward and the first shot's
 * spread is {@code first_shot_spread} times wider; once it is running, {@link Figure#RUNNING_BOLT} steadies it.
 * (Server-resolved: the client does not know when you last fired.)
 */
public class GunOpenBolt implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (GunShots.msSinceShot(shooter) > p.get("idle_ms", 1500f))
            return f.withSpread(p.get("first_shot_spread", 2f));
        // Running Bolt (Onslaught): a bolt kept cycling settles; clamped so stacking never zeroes spread
        float steady = 1 - Math.min(0.5f, Skills.modifier(shooter, Figure.RUNNING_BOLT));
        return steady < 1 ? f.withSpread(steady).withKick(steady) : f;
    }
}
