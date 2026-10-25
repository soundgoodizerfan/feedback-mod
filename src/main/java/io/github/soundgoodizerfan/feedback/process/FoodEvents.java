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
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.HudReadouts;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.DishBake;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Where food starts to spoil and where it is judged (food spec 4.2, 8). Nothing here ticks an item.
 *
 * <p>A food that has never been stamped is timeless, so a dropped food (a mob's drop, a harvest, a
 * thrown item) is stamped as it enters the world. Food generated straight into a chest or a pocket
 * is stamped by the first machine that touches it ({@link Cooking#tick}, the ice box, the rack).
 * Eating advances the spoilage first, so what you eat is what it has become.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FoodEvents {

    private FoodEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ItemEntity item))
            return;
        ItemStack stack = item.getItem();
        if (stack.has(FDataComponents.FOOD_STATE.get())) {
            // A dish dropped before any pack read it starts its clock here.
            if (stack.has(FDataComponents.DISH_FIGURES.get()))
                Cooking.settle(stack, event.getLevel(), AmbientTable.at(event.getLevel()).value());
            return;
        }
        Cooking.find(event.getLevel(), stack).ifPresent(entry -> stack.set(FDataComponents.FOOD_STATE.get(), Cooking.fresh(entry, event.getLevel())));
    }

    /** A dish picked up is carried: if it is overfull it spills (dish spec 2.2). Once per pickup, nothing ticked. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!event.getPlayer().level().isClientSide)
            DishBake.carry(event.getItemEntity().getItem());
    }

    /** A dish thrown down is carried too. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!event.getPlayer().level().isClientSide)
            DishBake.carry(event.getEntity().getItem());
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Start event) {
        var level = event.getEntity().level();
        ItemStack stack = event.getItem();
        if (!level.isClientSide && stack.has(FDataComponents.CANNED.get())) {
            Canning.settle(stack, level);
            return;
        }
        if (level.isClientSide || !stack.has(FDataComponents.FOOD_STATE.get()))
            return;
        Cooking.findPerishable(level, stack).ifPresent(entry -> {
            FoodState now = Cooking.current(stack, entry, level, AmbientTable.at(level).value());
            stack.set(FDataComponents.FOOD_STATE.get(), now);
            Cooking.apply(stack, entry, now);
        });
    }

    /**
     * The bite (dish spec 5): sneak and use a food to take a nibble. It applies the food's hazard effects as they are now, at
     * {@link FTuning#BITE_SHARE} of the chance and duration, and says which class they are in words. It eats nothing and never
     * refuses; the item's own cooldown keeps it from being spammed, and it stands in for eating while sneaking.
     */
    /**
     * Sneak and tap: a nibble. Sneak and hold eats as usual. Vanilla starts eating on the press, so a release within
     * {@link FTuning#BITE_TAP_TICKS} of starting is the tap ({@link #onTap}); when the player cannot eat now (full) there is
     * no eating to start, so the press itself is the nibble.
     */
    @SubscribeEvent
    public static void onBite(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        var food = stack.get(DataComponents.FOOD);
        if (!event.getEntity().isShiftKeyDown() || food == null || event.getEntity().canEat(food.canAlwaysEat()))
            return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player)
            bite(player, stack);
    }

    @SubscribeEvent
    public static void onTap(LivingEntityUseItemEvent.Stop event) {
        ItemStack stack = event.getItem();
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !stack.has(DataComponents.FOOD) || stack.getUseDuration(player) - event.getDuration() > FTuning.BITE_TAP_TICKS)
            return;
        bite(player, stack);
    }

    private static void bite(ServerPlayer player, ItemStack stack) {
        var level = player.level();
        if (player.getCooldowns().isOnCooldown(stack.getItem()))
            return;
        player.getCooldowns().addCooldown(stack.getItem(), FTuning.BITE_COOLDOWN_TICKS);
        if (stack.has(FDataComponents.CANNED.get()))
            Canning.settle(stack, level);
        boolean spoiled = false;
        if (stack.has(FDataComponents.FOOD_STATE.get()))
            spoiled = Cooking.findPerishable(level, stack).map(entry -> {
                FoodState now = Cooking.current(stack, entry, level, AmbientTable.at(level).value());
                stack.set(FDataComponents.FOOD_STATE.get(), now);
                Cooking.apply(stack, entry, now);
                return Cooking.spoilage(now) == 2;
            }).orElse(false);
        var effects = stack.get(DataComponents.FOOD).effects();
        String hazard = spoiled ? "spoiled" : "fine";
        for (var possible : effects) {
            MobEffectInstance effect = possible.effect();
            if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.POISON))
                hazard = "poison";
            else if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.HUNGER))
                hazard = "live";
            else if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.CONFUSION))
                hazard = "burnt";
            if (level.random.nextFloat() < possible.probability() * FTuning.BITE_SHARE)
                player.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(1, Math.round(effect.getDuration() * FTuning.BITE_SHARE)), effect.getAmplifier()));
        }
        HudReadouts.send(player, Component.translatable("message.feedback.bite." + hazard), FTuning.BITE_HUD_TICKS);
    }
}
