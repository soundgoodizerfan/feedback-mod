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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The hunting bow (spec 5.4): the shot makes no sound ({@code quiet}: {@code ModularBow} hushes the
 * level's arrow-shoot sound for the length of the release; the arrow's own hit is still heard),
 * and an arrow that strikes an animal ({@code MobCategory.CREATURE}) at the head, above its eye
 * line less {@code head_reach} blocks, does {@code head_mult} damage. Cost: a light draw: the
 * arrow flies at {@code velocity_mult} and the draw takes {@code draw_mult} as long; against
 * anything that is not an animal it is a weaker bow.
 */
public final class Stalker implements ModularTrait {

    @Override
    public void onArrowHit(LivingIncomingDamageEvent event, AbstractArrow arrow, ItemStack weapon, Params params) {
        LivingEntity target = event.getEntity();
        if (target.getType().getCategory() == MobCategory.CREATURE
                && arrow.getY() > target.getEyeY() - params.get("head_reach", 0.3f))
            event.setAmount(event.getAmount() * params.get("head_mult", 1.5f));
    }
}
