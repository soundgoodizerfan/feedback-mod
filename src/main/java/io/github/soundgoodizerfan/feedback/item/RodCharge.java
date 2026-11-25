// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A rod that stores heat and gives it up slowly (a blaze rod). It is an {@link ItemHeat} item like any other: made, it is hot
 * ({@link ItemTuning#ROD_CHARGE_TU}) and its material's {@code cooling_rate} is so low that it takes about 20 minutes to cool to
 * {@code WARM_TU}. Over that line it is <em>charged</em>; at or under it, <em>spent</em>. Two states, read off the heat, never a
 * percentage: a rod that carries a stamp and is no longer hot is spent whether or not anything has turned it into the spent item yet.
 * A rod with no stamp at all is fresh from the world (loot, a trade, the creative menu) and charged: the first time a player's inventory
 * sweep sees it, it is stamped hot.
 * <p>
 * Nothing is ticked for this. A stamped rod becomes the Spent Blaze Rod the next time a player's inventory sweep meets it cold; until then
 * (in a chest, on the ground) it is already spent by what it is at, and everything that asks ({@link #spent}) says so.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RodCharge {

    private RodCharge() {
    }

    /**
     * A charged rod: the kind that arrives hot, unstamped (fresh from the world: loot, a trade, the creative menu) or still over the heat
     * that makes it hot. One rule for both the sweep and the grind: only a rod that carries a heat stamp and has cooled is spent.
     */
    public static boolean charged(ItemStack stack, Level level) {
        return stack.is(FTags.CHARGED_RODS) && (!stack.has(FDataComponents.TEMPERATURE.get()) || ItemHeat.isHot(stack, level));
    }

    /** A spent rod: the spent item, or a charged-kind rod that was stamped and has gone cold. */
    public static boolean spent(ItemStack stack, Level level) {
        return stack.is(FTags.SPENT_RODS) || stack.is(FTags.CHARGED_RODS) && !charged(stack, level);
    }

    /** A blaze's drop comes out of it hot: the rod is made charged. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide)
            return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(FTags.CHARGED_RODS) && !stack.has(FDataComponents.TEMPERATURE.get()))
                ItemHeat.set(stack, ItemTuning.ROD_CHARGE_TU, level);
        }
    }

    /**
     * Once a second, what a player carries: a rod with no heat stamp is fresh from the world and is stamped hot the first time it is seen;
     * a stamped rod that has gone cold becomes the spent rod, keeping its other components. Only the rod stack itself is looked at, never
     * a rod that is a part of a modular item.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide || (player.tickCount + player.getId()) % 20 != 0)
            return;
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.is(FTags.CHARGED_RODS))
                continue;
            if (!stack.has(FDataComponents.TEMPERATURE.get()))
                ItemHeat.set(stack, ItemTuning.ROD_CHARGE_TU, player.level());
            else if (!ItemHeat.isHot(stack, player.level())) {
                ItemStack spent = stack.transmuteCopy(FItems.SPENT_BLAZE_ROD.get(), stack.getCount());
                ItemHeat.clear(spent);
                items.set(i, spent);
            }
        }
    }
}
