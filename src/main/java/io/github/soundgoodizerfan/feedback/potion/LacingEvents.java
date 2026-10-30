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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Taste lacing onto a dish or ladled serving (potions spec 7, 14.9). Sneak-using a food that carries a {@code meal} with a drinkable potion
 * in the other hand pours the bottle over it. The pot takes a bottle in {@code StockpotBlock}. Runs before the nibble, which also answers a sneak-use on food.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class LacingEvents {

    private LacingEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPour(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide || !player.isShiftKeyDown())
            return;
        ItemStack food = event.getItemStack();
        InteractionHand other = event.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack bottle = player.getItemInHand(other);
        if (!food.has(FDataComponents.MEAL.get()) || !(bottle.getItem() instanceof PotionItem))
            return;
        PotionState state = bottle.get(FDataComponents.POTION_STATE.get());
        var lacing = state == null || food.has(FDataComponents.LACING.get()) ? java.util.Optional.<Lacing>empty() : Lacing.of(state, event.getLevel(), 1);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (lacing.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.lace_refused"), true);
            return;
        }
        // One portion takes the potion; the rest of a stack stays plain.
        ItemStack laced = food.copyWithCount(1);
        laced.set(FDataComponents.LACING.get(), lacing.get());
        food.shrink(1);
        if (food.isEmpty())
            player.setItemInHand(event.getHand(), laced);
        else if (!player.getInventory().add(laced))
            player.drop(laced, false);
        if (!player.getAbilities().instabuild)
            player.setItemInHand(other, new ItemStack(Items.GLASS_BOTTLE));
        event.getLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5f, 1.1f);
    }
}
