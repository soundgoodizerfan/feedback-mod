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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The vial: using it pours every point it holds into the player, whose levels follow vanilla's curve. */
public class XpContainerItem extends ModularItem {

    public XpContainerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        XpStore held = stack.get(FDataComponents.XP_STORE.get());
        if (held == null || held.points() <= 0 || player.isShiftKeyDown())
            return super.use(level, player, hand);
        if (!level.isClientSide) {
            player.giveExperiencePoints(held.points());
            stack.set(FDataComponents.XP_STORE.get(), held.with(0));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
