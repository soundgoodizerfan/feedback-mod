// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lambdynlights;

import java.util.function.Function;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;

import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A hot piece gives off dynamic light: the brightest glowing stack a creature holds (any equipment slot) or a dropped item is, as block light
 * from the same table a glowing block reads ({@link Incandescence#light}), by the item's live temperature ({@link ItemHeat#get}), so a
 * cooled ingot stops glowing. Registered through LambDynamicLights static entity handler API for the creatures that carry things (players, villagers, humanoid mobs, armour stands) and for dropped
 * items; its own throttling decides how often the handlers are asked.
 *
 * <p>LambDynamicLights (Lambda License), read for design only with the author's permission, nothing copied. Taken: an integer light level
 * 0 to 15 per stack over every equipment slot and dropped item entities. Rejected: its item JSON match (a flat rate, so a cooled ingot would
 * glow as a hot one), its initializer entrypoint (its discovery on NeoForge goes through another library; the static handler API needs none), and colour (it has none; a blackbody tint would need a mixin).
 */
final class HotGlow {

    private HotGlow() {
    }

    /** The creatures that carry things in a hand: players, villagers, the humanoid mobs and armour stands. */
    private static final java.util.List<EntityType<? extends LivingEntity>> CARRIERS = java.util.List.of(EntityType.PLAYER, EntityType.VILLAGER,
            EntityType.WANDERING_TRADER, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON,
            EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.VINDICATOR,
            EntityType.EVOKER, EntityType.ARMOR_STAND);

    @SuppressWarnings("removal")
    static void register() {
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.ITEM, DynamicLightHandler.makeHandler(
                (Function<net.minecraft.world.entity.item.ItemEntity, Integer>) e -> light(e.getItem(), e), e -> false));
        for (EntityType<? extends LivingEntity> type : CARRIERS)
            registerCarrier(type);
    }

    @SuppressWarnings({"removal", "unchecked"})
    private static <T extends LivingEntity> void registerCarrier(EntityType<? extends LivingEntity> type) {
        DynamicLightHandlers.registerDynamicLightHandler((EntityType<T>) type, DynamicLightHandler.makeHandler(
                (Function<T, Integer>) HotGlow::held, e -> false));
    }

    private static int held(LivingEntity e) {
        int best = 0;
        for (ItemStack stack : e.getAllSlots())
            best = Math.max(best, light(stack, e));
        return best;
    }

    private static int light(ItemStack stack, Entity in) {
        if (stack.isEmpty())
            return 0;
        var found = Material.find(ClientMaterials.on(in.level()), stack);
        if (found.isEmpty() || !Incandescence.glows(found.get().material()))
            return 0;
        return Incandescence.light(ItemHeat.get(stack, in.level()).value());
    }
}
