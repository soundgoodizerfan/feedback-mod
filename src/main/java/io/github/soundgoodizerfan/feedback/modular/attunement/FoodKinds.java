// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * What an attuned container of a dish, soup, pie, bread, skewer or wrap does for the food in it. Rot runs through
 * {@link #rotShare} (the lazy clock in {@code Cooking.current}), and eating the dish runs the rest. Held, the container's
 * lure, glow and the like act through the main-hand readers like any stack's.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FoodKinds {

    /** Share of the rot rate one level turns away (a seal, a pest ward, a cure field, a lamp's light) or adds (kin of pests, a warm container). Placeholder. */
    private static final float SLOW_PER_LEVEL = 0.15f, QUICKEN_PER_LEVEL = 0.15f, SLOW_FLOOR = 0.2f;
    /** Food points one level of nourish adds or thin takes; experience points one level of tithe pays a meal; durability one level of restore closes on worn gear. */
    private static final int FOOD_PER_LEVEL = 1, TITHE_XP_PER_LEVEL = 3, RESTORE_PER_LEVEL = 2;

    private FoodKinds() {
    }

    private static int level(ItemStack stack, String kind) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind))
                n += a.level();
        return n;
    }

    /** Multiplier on a dish's rot rate from its container: sealed, pest-warded, cured or lit slows it; pests drawn or heat held speeds it. 1 for a plain container. */
    public static float rotShare(ItemStack dish) {
        if (dish.isEmpty())
            return 1f;
        int slow = 0, quick = 0, insulate = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(dish))
            switch (a.kind()) {
                case "seal", "pest_ward", "cure", "glow", "insulate" -> slow += a.level();
                case "pest_lure", "burn_wielder" -> quick += a.level();
                default -> {
                }
            }
        return Math.max(SLOW_FLOOR, 1f - SLOW_PER_LEVEL * slow + QUICKEN_PER_LEVEL * quick);
    }

    /** Eating a dish: nourish and thin change the meal's food points, restore closes wear on what the eater wears, tithe pays experience. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack dish = event.getItem();
        if (dish.isEmpty() || MaterialPerks.attunements(dish).isEmpty())
            return;
        int nourish = level(dish, "nourish"), thin = level(dish, "thin"), restore = level(dish, "restore"), tithe = level(dish, "tithe_meal");
        if (nourish != thin)
            player.getFoodData().setFoodLevel(Math.max(0, Math.min(20, player.getFoodData().getFoodLevel() + (nourish - thin) * FOOD_PER_LEVEL)));
        if (restore > 0)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.isDamaged() && slot != EquipmentSlot.BODY)
                    worn.setDamageValue(Math.max(0, worn.getDamageValue() - restore * RESTORE_PER_LEVEL));
            }
        if (tithe > 0)
            ExperienceOrb.award((ServerLevel) player.level(), player.position(), tithe * TITHE_XP_PER_LEVEL);
    }
}
