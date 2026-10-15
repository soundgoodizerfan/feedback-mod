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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
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

    private Hazards() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Feedback.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }
}
