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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ThrownWeapon;
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

    private FEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
