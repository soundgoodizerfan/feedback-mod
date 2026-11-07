// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The attack speed bonus for {@link Wielding#dualWielding}, as a transient attribute modifier the server
 * adds and removes (the attribute syncs to the client by itself, so both sides' cooldowns agree).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class DualWieldBoost {

    private static final ResourceLocation ID = Feedback.id("dual_wield");

    private DualWieldBoost() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null)
            return;
        boolean dual = Wielding.dualWielding(player), has = speed.getModifier(ID) != null;
        if (dual && !has)
            speed.addTransientModifier(new AttributeModifier(ID, Wielding.DUAL_SPEED - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!dual && has)
            speed.removeModifier(ID);
    }
}
