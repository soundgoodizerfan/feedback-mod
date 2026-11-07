// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * What the world's equipment can do to a body that handles it carelessly, and the damage types it
 * does it with.
 *
 * <h2>Three kinds, and what they share</h2>
 * Hot work, steam and a live conductor are different physics and the same <em>shape</em>: a state
 * the machine already holds (a temperature, a pressure, a voltage) is above a threshold, and
 * whoever is in contact pays. None of it is uptime and none of it is a machine's own wear -- it is
 * the consequence of a person touching something they had no business touching bare. The
 * thresholds are named bands (philosophy 3), the protections are physical things a player can
 * point at (gloves, tongs), and nothing here asks what an item <em>is</em>.
 *
 * <h2>Fire-tagged on purpose</h2>
 * {@link #HOT_CONTACT} and {@link #SCALD} are in {@code minecraft:is_fire}, so fire resistance
 * answers them the way it should: a potion of it is a real, bought way to handle red-hot work.
 * {@link #SHOCK} bypasses armour (it is in {@code bypasses_armor}), because a chestplate is not
 * insulation.
 */
public final class Hazards {

    public static final ResourceKey<DamageType> HOT_CONTACT = key("hot_contact");
    public static final ResourceKey<DamageType> SCALD = key("scald");
    public static final ResourceKey<DamageType> SHOCK = key("shock");
    public static final ResourceKey<DamageType> GUN_RECOIL = key("gun_recoil");

    private Hazards() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Feedback.id(name));
    }

    /** Death messages per type: lang keys {@code death.attack.feedback.<type>.1..n}, plus {@code .player} when someone caused it. */
    public static final int VARIANTS = 8;

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return source(level, type, null);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity attacker) {
        return new Funny(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    /** A source whose death message is one of {@link #VARIANTS} picked at random. */
    private static final class Funny extends DamageSource {
        Funny(net.minecraft.core.Holder<DamageType> type, Entity attacker) {
            super(type, attacker);
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            String key = "death.attack." + type().msgId() + "." + (1 + victim.getRandom().nextInt(VARIANTS));
            Entity killer = getEntity();
            return killer == null ? Component.translatable(key, victim.getDisplayName())
                    : Component.translatable(key + ".player", victim.getDisplayName(), killer.getDisplayName(), weapon(killer));
        }

        /** The killer's held item, for lines that name the gun; a bare hand reads "a weapon". */
        private static Component weapon(Entity killer) {
            return killer instanceof LivingEntity living && !living.getMainHandItem().isEmpty()
                    ? living.getMainHandItem().getDisplayName() : Component.translatable("death.attack.feedback.no_weapon");
        }
    }
}
