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
 * A molten fluid for every metal listed in {@link #IDS}, with no per-metal Java: the
 * {@code molten_<id>} type, source and flowing fluid, tinted by the material's own colour. A
 * metal that is liquid when hot ought to be castable without anyone writing a fluid, a melt and a
 * casting for it, and the melt and casting are derived too (see {@code ThermalProcessTable} and
 * {@code CastingTable}). The older metals keep their hand-registered fluids in {@link FFluids}.
 * Steel is made by a process but melts like any metal once made; its tempers (a state of the
 * solid) are not listed.
 */
public final class MoltenMetals {

    /** Material ids that get a generated molten fluid. A new metal adds its id here. */
    public static final List<String> IDS = List.of(
            "chromium", "nickel", "cobalt", "manganese", "aluminium", "titanium", "tungsten", "thorium",
            "silicon", "silicon_crude", "silica_glass", "optical_glass", "steel", "stainless_steel", "manganese_steel",
            "tungsten_carbide", "titanium_carbide", "thorium_tungsten", "cermet", "redstone_chromium");

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Feedback.MOD_ID);

    private static final Map<String, DeferredHolder<FluidType, FluidType>> TYPE_HOLDERS = new LinkedHashMap<>();

    static {
        for (String id : IDS) {
            String name = "molten_" + id;
            DeferredHolder<FluidType, FluidType> type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.feedback." + name)
                    .lightLevel(15)
                    .density(7000)
                    .viscosity(6000)
                    .temperature(1300)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canHydrate(false)
                    .supportsBoating(false)));
            TYPE_HOLDERS.put(id, type);
            // The three holders refer to each other, so the properties are built lazily from a shared box.
            DeferredHolder<Fluid, Fluid>[] box = new DeferredHolder[2];
            BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
            java.util.function.Supplier<BaseFlowingFluid.Properties> properties = () -> {
                if (props[0] == null)
                    props[0] = new BaseFlowingFluid.Properties(type, box[0], box[1]);
                return props[0];
            };
            box[0] = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties.get()));
            box[1] = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(properties.get()));
        }
    }

    private MoltenMetals() {}

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
