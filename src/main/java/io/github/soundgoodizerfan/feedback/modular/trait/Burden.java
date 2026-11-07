// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The weight of a drawn longbow or a raised tower shield: a slowing modifier on the wielder's
 * movement speed, put on by the trait every tick the item is held out and taken off the first tick
 * it is not. Transient, so it is never saved; nothing is kept per wielder.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Burden {

    private Burden() {
    }

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "burden");

    /** Slow {@code entity} by the fraction {@code slow} (0.3 is thirty percent) while it keeps using the item. */
    public static void apply(LivingEntity entity, float slow) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (slow > 0 && speed != null && !speed.hasModifier(ID))
            speed.addTransientModifier(new AttributeModifier(ID, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isUsingItem())
            return;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(ID))
            speed.removeModifier(ID);
    }
}
