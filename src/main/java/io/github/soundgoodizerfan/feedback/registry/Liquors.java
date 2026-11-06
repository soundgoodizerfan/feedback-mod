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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One leach liquor per metal ({@code copper_sulfate}, {@code gold_chloride}, ...): the metal-bearing solution a leach row makes from an ore
 * concentrate and a reagent (and the carbon dioxide a carbonate ore vents), and an electrowinning or precipitation row turns into metal (the ore ladder, {@code OreLadder}). No Java
 * per metal: a {@code material/<id>.json} (kind fluid, density, composition, colour) gives it its page and tint, and an id added to
 * {@link #IDS} gives it a fluid. Contained fluids only, like {@link DyeFluids}: no block, no bucket.
 */
public final class Liquors {

    /** Material ids that get a liquor fluid. A new metal's liquor adds its id here. */
    public static final List<String> IDS = List.of(
            "copper_sulfate", "zinc_sulfate", "gold_chloride", "silver_chloride", "nickel_sulfate", "cobalt_sulfate",
            "sodium_aluminate", "titanyl_sulfate", "sodium_tungstate", "uranyl_sulfate", "thorium_hydroxide", "carbon_dioxide");

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Feedback.MOD_ID);

    private static final Map<String, DeferredHolder<FluidType, FluidType>> TYPE_HOLDERS = new LinkedHashMap<>();

    static {
        for (String id : IDS) {
            DeferredHolder<FluidType, FluidType> type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.feedback." + id)
                    .density(1400)
                    .viscosity(1200)
                    .temperature(300)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canHydrate(false)
                    .supportsBoating(false)));
            TYPE_HOLDERS.put(id, type);
            // The holders refer to each other, so the properties are built lazily from a shared box.
            DeferredHolder<Fluid, Fluid>[] box = new DeferredHolder[2];
            BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
            java.util.function.Supplier<BaseFlowingFluid.Properties> properties = () -> {
                if (props[0] == null)
                    props[0] = new BaseFlowingFluid.Properties(type, box[0], box[1]);
                return props[0];
            };
            box[0] = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties.get()));
            box[1] = FLUIDS.register(id + "_flowing", () -> new BaseFlowingFluid.Flowing(properties.get()));
        }
    }

    private Liquors() {}

    /** Type holder per material id, for client tinting. */
    public static Map<String, DeferredHolder<FluidType, FluidType>> types() {
        return TYPE_HOLDERS;
    }

    /** The material's colour as ARGB, or a neutral grey. */
    public static int tint(String id) {
        var material = FormItems.jarMaterials().get(ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, id));
        return 0xFF000000 | (material == null ? 0xB0B0B0 : material.color().orElse(0xB0B0B0));
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
