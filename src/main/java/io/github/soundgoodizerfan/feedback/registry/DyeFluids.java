// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One dye fluid per dye colour ({@code dye_red}, ...): a dye item dipped in a vessel of water dissolves into it
 * ({@code dip/dye_*.json}), and glass dipped in a vessel of it comes out stained ({@code dip/stain_*.json}).
 * Contained fluids only, like {@link MoltenMetals}: no block, no bucket.
 */
public final class DyeFluids {

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Feedback.MOD_ID);

    private static final Map<DyeColor, DeferredHolder<FluidType, FluidType>> TYPE_HOLDERS = new LinkedHashMap<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "dye_" + color.getName();
            DeferredHolder<FluidType, FluidType> type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.feedback." + name)
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canHydrate(false)
                    .supportsBoating(false)));
            TYPE_HOLDERS.put(color, type);
            // The holders refer to each other, so the properties are built lazily from a shared box.
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

    private DyeFluids() {}

    /** Type holder per colour, for client tinting. */
    public static Map<DyeColor, DeferredHolder<FluidType, FluidType>> types() {
        return TYPE_HOLDERS;
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
