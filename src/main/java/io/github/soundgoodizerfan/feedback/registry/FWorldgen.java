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
import io.github.soundgoodizerfan.feedback.worldgen.OreVeinFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FWorldgen {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Feedback.MOD_ID);

    /** The one feature every deposit in {@code data/<ns>/ore_vein/} is placed by. */
    public static final DeferredHolder<Feature<?>, OreVeinFeature> ORE_VEIN = FEATURES.register("ore_vein",
            () -> new OreVeinFeature(NoneFeatureConfiguration.CODEC));

    /** Places the oil basins' seeps and tar patches (petrochemistry spec 2.3); the basin itself is data, not blocks. */
    public static final DeferredHolder<Feature<?>, io.github.soundgoodizerfan.feedback.geology.BasinFeature> BASIN = FEATURES.register("basin",
            () -> new io.github.soundgoodizerfan.feedback.geology.BasinFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
