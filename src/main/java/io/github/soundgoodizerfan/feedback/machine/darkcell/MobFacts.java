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
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * What a kill method needs to know about a mob, read from the entity type and its tags, never from its id: health, attack, mass, whether
 * it breathes, burns, falls or takes a shock, whether it is of the player lineage (zombies, skeletons and their kin), and the XP it is
 * worth. A modded mob joins by its tags.
 */
public record MobFacts(EntityType<?> type, float health, float attack, float mass, boolean fireImmune, boolean breathes,
                       boolean fallImmune, boolean electricImmune, boolean lineage, int xp) {

    public static final TagKey<EntityType<?>> LINEAGE = TagKey.create(Registries.ENTITY_TYPE, Feedback.id("lineage"));
    public static final TagKey<EntityType<?>> NON_BREATHERS = TagKey.create(Registries.ENTITY_TYPE, Feedback.id("non_breathers"));
    public static final TagKey<EntityType<?>> ELECTRIC_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Feedback.id("electric_immune"));

    private static final Map<ResourceLocation, MobFacts> CACHE = new ConcurrentHashMap<>();

    /** Invented. Mass in Mu per cubic block of a mob's bounding box: a body is mostly water. */
    private static final float DENSITY = 1f;

    public static MobFacts of(ServerLevel level, ResourceLocation id) {
        return CACHE.computeIfAbsent(id, k -> build(level, BuiltInRegistries.ENTITY_TYPE.get(k)));
    }

    private static MobFacts build(ServerLevel level, EntityType<?> type) {
        float health = 20f, attack = 1f;
        try {
            @SuppressWarnings("unchecked")
            var supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
            if (supplier.hasAttribute(Attributes.MAX_HEALTH))
                health = (float) supplier.getBaseValue(Attributes.MAX_HEALTH);
            if (supplier.hasAttribute(Attributes.ATTACK_DAMAGE))
                attack = (float) supplier.getBaseValue(Attributes.ATTACK_DAMAGE);
        } catch (RuntimeException ignored) {
            // A type with no attribute set keeps the defaults.
        }
        var dims = type.getDimensions();
        float mass = Math.max(0.1f, dims.width() * dims.width() * dims.height() * DENSITY);
        int xp = 5;
        var dummy = type.create(level);
        if (dummy instanceof Mob mob)
            xp = Math.max(0, mob.getExperienceReward(level, null));
        if (dummy != null)
            dummy.discard();
        return new MobFacts(type, health, Math.max(0f, attack), mass, type.fireImmune(),
                !type.is(EntityTypeTags.UNDEAD) && !type.is(NON_BREATHERS), type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE),
                type.is(ELECTRIC_IMMUNE), type.is(LINEAGE), xp);
    }
}
