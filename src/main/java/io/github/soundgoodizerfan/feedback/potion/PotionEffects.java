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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * What an effect node's {@code effect} id does to a body. A registered {@code MobEffect} is applied as one; an id no registry holds is a
 * special the potion map can name without a new class: {@link #IGNITE} sets the body alight, {@link #EXPLOSION} is a throw-only burst
 * (see {@link ThrownPotionBottle}) and does nothing to a body by itself.
 */
public final class PotionEffects {

    public static final ResourceLocation IGNITE = Feedback.id("ignite"), EXPLOSION = Feedback.id("explosion");

    private PotionEffects() {}

    /** Applies the effect to {@code target} for {@code ticks} at level {@code amplifier} (0 is I). */
    public static void apply(ResourceLocation id, LivingEntity target, int amplifier, int ticks) {
        if (IGNITE.equals(id))
            target.igniteForSeconds(ticks / 20f);
        else
            BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(e -> target.addEffect(new MobEffectInstance(e, ticks, amplifier)));
    }

    public static Component name(ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> h.value().getDisplayName()).orElseGet(() -> Component.translatable(id.toLanguageKey("effect")));
    }
}
