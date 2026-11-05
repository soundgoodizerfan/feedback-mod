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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Obturator: a soft ring that seals a loose chamber. The softer the stuff, the better it seals ({@code seal} runs from
 * 1 at no hardness to 0 at {@code soft_limit}): kick falls by {@code kick_cut} and the group tightens by
 * {@code spread_cut}, both times the seal. Once the barrel passes the stuff's oxidation temperature it has burned to
 * nothing and leaks: kick and spread climb past a bare gun's ({@code burnt_kick}, {@code burnt_spread}). A stuff with no
 * oxidation temperature never burns. shortcut: read live off the barrel, so a burnt ring seals again once the barrel
 * cools; stamp the burn into the stack when it should be permanent.
 */
public class GunObturator implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var m = GunFitting.of(gun, "obturator");
        if (m == null)
            return f;
        boolean burnt = m.thermal().oxidationTemperature()
                .map(t -> GunCondition.temperature(gun, shooter.level()).value() >= t.value()).orElse(false);
        if (burnt)
            return f.withKick(p.get("burnt_kick", 1.1f)).withSpread(p.get("burnt_spread", 1.2f));
        float seal = Math.max(0f, 1f - m.hardness().value() / p.get("soft_limit", 1f));
        return f.withKick(1f - p.get("kick_cut", 0.25f) * seal).withSpread(1f - p.get("spread_cut", 0.15f) * seal);
    }
}
