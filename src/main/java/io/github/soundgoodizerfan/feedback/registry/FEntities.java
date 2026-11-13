// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.combat.ThrownWeapon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types. */
public final class FEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Feedback.MOD_ID);

    /** A thrown modular weapon: the hatchet, and every trident-shaped one. Updates every tick so a reeled or returning weapon moves smoothly. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeapon>> THROWN_WEAPON = ENTITIES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(8).updateInterval(1).build("thrown_weapon"));

    /** The grapple's hook; see {@code combat/tether/GrappleHook}. */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook>> GRAPPLE_HOOK =
            ENTITIES.register("grapple_hook", () -> EntityType.Builder.<io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook>of(
                    io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(8).updateInterval(1).build("grapple_hook"));

    /** A thrown bottle of potion; see {@code potion/ThrownPotionBottle}. */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.soundgoodizerfan.feedback.potion.ThrownPotionBottle>> POTION_BOTTLE =
            ENTITIES.register("potion_bottle", () -> EntityType.Builder.<io.github.soundgoodizerfan.feedback.potion.ThrownPotionBottle>of(
                    io.github.soundgoodizerfan.feedback.potion.ThrownPotionBottle::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10).build("potion_bottle"));

    /** A thrown demolition charge; see {@code weapon/GrenadeEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.soundgoodizerfan.feedback.weapon.GrenadeEntity>> GRENADE =
            ENTITIES.register("grenade", () -> EntityType.Builder.<io.github.soundgoodizerfan.feedback.weapon.GrenadeEntity>of(
                    io.github.soundgoodizerfan.feedback.weapon.GrenadeEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10).build("grenade"));

    /** The steam a pulse gun's forced vent lets go; see {@code weapon/SteamCloud}. */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.soundgoodizerfan.feedback.weapon.SteamCloud>> STEAM_CLOUD =
            ENTITIES.register("steam_cloud", () -> EntityType.Builder.<io.github.soundgoodizerfan.feedback.weapon.SteamCloud>of(
                    io.github.soundgoodizerfan.feedback.weapon.SteamCloud::new, MobCategory.MISC).sized(1.6f, 1.2f)
                    .clientTrackingRange(8).updateInterval(5).build("steam_cloud"));

    private FEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
