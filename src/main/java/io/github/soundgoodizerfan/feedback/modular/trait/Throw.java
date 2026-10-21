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
import io.github.soundgoodizerfan.feedback.modular.Throwing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Throwing a held weapon (spec 5.4): hold use to wind up, release to throw. The flight is
 * {@link Throwing}'s and {@link io.github.soundgoodizerfan.feedback.modular.ThrownWeapon}'s; this
 * trait only says that the item is held out and thrown on release, and what that costs. Params:
 * {@code min_charge_ticks}, {@code full_charge_ticks}, {@code damage_per_block} and {@code max_mult}
 * (the blow grows with the distance flown, to a cap), {@code wear} per throw, {@code tumble} (end over
 * end rather than point first), {@code return} (a loyalty-style return rate). A trident-shaped weapon
 * throws the same way through {@code ModularSpear}, whatever its own trait is.
 */
public final class Throw implements ModularTrait {

    private static final int WINDUP = 72000;

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        return InteractionResultHolder.consume(weapon);
    }

    @Override
    public int useDuration(ItemStack weapon, Params params) {
        return WINDUP;
    }

    @Override
    public UseAnim useAnimation(ItemStack weapon, Params params) {
        return UseAnim.SPEAR;
    }

    @Override
    public void releaseUsing(ItemStack weapon, Level level, LivingEntity user, int remaining, Params params) {
        if (user instanceof Player player)
            Throwing.release(weapon, level, player, WINDUP - remaining, params, 0.35f, 2.5f, 2);
    }
}
