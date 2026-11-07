// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Rapier (spec 5.4): consecutive hits on the same target inside {@code window} ticks stack
 * {@code bonus} each up to {@code max_stacks}. The cost is the discipline: a miss, a new target
 * or taking a hit starts over, and the blade itself strikes lightly.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Flurry implements ModularTrait {

    private record Run(java.util.UUID target, long last, int stacks) {
    }

    private static final WielderState<Run> RUNS = new WielderState<>();

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        long now = attacker.level().getGameTime();
        Run run = RUNS.get(attacker.getUUID());
        int stacks = run != null && run.target.equals(event.getEntity().getUUID())
                && now - run.last <= params.get("window", 25f) ? run.stacks + 1 : 1;
        stacks = Math.min(stacks, Math.round(params.get("max_stacks", 5f)));
        RUNS.put(attacker.getUUID(), new Run(event.getEntity().getUUID(), now, stacks));
        event.setAmount(event.getAmount() * (1 + params.get("bonus", 0.12f) * (stacks - 1)));
        if (stacks > 1 && attacker instanceof net.minecraft.server.level.ServerPlayer player)
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(player, Component.translatable("trait.feedback.flurry.stacks", stacks), 40);
    }

    /** Taking damage breaks the rhythm. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide)
            RUNS.remove(event.getEntity().getUUID());
    }
}
