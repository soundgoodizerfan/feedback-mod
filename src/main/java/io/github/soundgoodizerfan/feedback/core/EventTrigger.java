// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's one advancement criterion trigger, {@code feedback:event}: an advancement names a string
 * ({@code "conditions": {"event": "combat/size_up"}}) and code fires that string at a player through {@link #fire}.
 * A new event-driven advancement is a JSON file and one {@code EventTrigger.fire(player, "...")} call at the moment
 * the act happens; no new trigger class. Acts the game already counts (items held, blocks placed) use vanilla
 * triggers instead.
 */
public final class EventTrigger extends SimpleCriterionTrigger<EventTrigger.Instance> {

    private static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Feedback.MOD_ID);

    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, EventTrigger> EVENT =
            TRIGGERS.register("event", EventTrigger::new);

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    /** Fire {@code event} at {@code player}; a no-op for a client player or an event no advancement names. */
    public static void fire(net.minecraft.world.entity.player.Player player, String event) {
        if (player instanceof ServerPlayer sp)
            EVENT.get().trigger(sp, i -> i.event().equals(event));
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
    }
}
