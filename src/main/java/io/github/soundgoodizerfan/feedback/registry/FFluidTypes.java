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

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One {@link FluidType} per moltenable metal, plus {@link #STEAM}.
 *
 * <h2>Why a type per metal rather than one shared type</h2>
 * Tint is a property of the {@link FluidType}, not of the {@link net.minecraft.world.level.material.Fluid}
 * instance -- {@code IClientFluidTypeExtensions} is registered per type (see
 * {@link io.github.soundgoodizerfan.feedback.client.FClientFluids}) -- so molten copper reading
 * orange and molten iron reading white-hot costs one type each. There is nothing else to say
 * about any of these that is not already said once in {@link FFluids}, so this class is a plain
 * list rather than a table -- unlike {@code Fuel} or {@code ThermalProcess}, a fluid's identity
 * has to be code (registration happens at class load), so there is no datapack version of this
 * one to keep parallel.
 */
public final class FFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_COPPER = moltenType("molten_copper");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_GOLD = moltenType("molten_gold");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_IRON = moltenType("molten_iron");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_TIN = moltenType("molten_tin");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_ZINC = moltenType("molten_zinc");
    /** Copper's alloys -- see {@code data/feedback/alloy/}. Each is its own real fluid, not a
     * tint of copper's, the same one-type-per-metal rule the pure metals already use. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_BRONZE = moltenType("molten_bronze");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_BRASS = moltenType("molten_brass");

    /** Glass melts and casts exactly like a metal -- see {@code FItems#GLASS_INGOT}. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_GLASS = moltenType("molten_glass");

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_LEAD = moltenType("molten_lead");

    /** Nether quartz's own melt -- see {@code FItems#QUARTZ_GLASS_INGOT}. No graphite fluid
     * type: graphitization never passes through a liquid phase, see {@code FItems#GRAPHITE}. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_QUARTZ_GLASS = moltenType("molten_quartz_glass");

    /**
     * The vehicle, not a fourth thermal state -- see {@code FTuning}'s {@code --- the boiler ---}
     * section. A gas rather than a liquid: negative density is the vanilla/Forge convention for
     * "rises, and machines reason about it as a gas," which nothing here reads today but is free
     * to record correctly now rather than retrofit later.
     */
    public static final DeferredHolder<FluidType, FluidType> STEAM = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.feedback.steam")
                    .lightLevel(0)
                    .density(-10)
                    .viscosity(200)
                    .temperature(400)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canHydrate(false)
                    .supportsBoating(false)));

    private static DeferredHolder<FluidType, FluidType> moltenType(String name) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
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
    }

    private FFluidTypes() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        FLUID_TYPES.register(modBus);
    }
}
