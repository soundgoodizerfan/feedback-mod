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

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Great helm (spec 5.4, armour): a projectile does only {@code projectile_mult} of its damage to
 * the wearer, but a polished bucket on the head is seen from far off: mobs judge the wearer
 * {@code visibility_mult} as visible, so they notice from further away. Worn: counts once.
 */
public final class ClosedVisor implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            event.setAmount(event.getAmount() * params.get("projectile_mult", 0.6f));
    }

    @Override
    public void onWornVisibility(LivingEvent.LivingVisibilityEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.modifyVisibility(params.get("visibility_mult", 1.5f));
    }
}
