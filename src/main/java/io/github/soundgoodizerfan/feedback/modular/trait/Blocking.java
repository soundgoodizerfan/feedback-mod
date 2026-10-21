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

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * A crossguard that takes a blow (spec 5.4): hold use to raise the blade and the guard catches
 * {@code block_mult} of what lands in front of you, as the sword-block of old versions did. The
 * cost is that it is a bar of metal and not a shield: it stops only that share, the rest comes
 * through, and each blow caught costs the blade {@code wear} points. Nothing here reads the
 * blade's material; a better guard is a heavier or tougher one, which the bake already prices in
 * mass and swing.
 */
public final class Blocking implements ModularTrait {

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        return ModularStacks.broken(weapon) ? InteractionResultHolder.pass(weapon) : InteractionResultHolder.consume(weapon);
    }

    @Override
    public int useDuration(ItemStack weapon, Params params) {
        return 72000;
    }

    @Override
    public UseAnim useAnimation(ItemStack weapon, Params params) {
        return UseAnim.BLOCK;
    }

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack weapon, Params params) {
        if (!event.getBlocked() || ModularStacks.broken(weapon))
            return;
        event.setBlockedDamage(event.getOriginalBlockedDamage() * params.get("block_mult", 0.5f));
        event.setShieldDamage(params.get("wear", 1f));
    }
}
