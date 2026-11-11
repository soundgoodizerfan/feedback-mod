// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Engineering Table's registrations: its {@code feedback:engineering} recipe type and serializer. */
public final class FEngineering {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Feedback.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EngineeringRecipe>> TYPE = TYPES.register("engineering", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return "feedback:engineering";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, EngineeringRecipe.Serializer> SERIALIZER =
            SERIALIZERS.register("engineering", EngineeringRecipe.Serializer::new);

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private FEngineering() {
    }
}
