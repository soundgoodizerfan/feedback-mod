// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * list rather than a table -- unlike {@code Material} or {@code ThermalProcess}, a fluid's identity
 * has to be code (registration happens at class load), so there is no datapack version of this
 * one to keep parallel.
 */
public final class FFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_COPPER = moltenType("molten_copper");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_GOLD = moltenType("molten_gold");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_WROUGHT_IRON = moltenType("molten_wrought_iron");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_IRON = moltenType("molten_iron");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_CAST_IRON = moltenType("molten_cast_iron");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_TIN = moltenType("molten_tin");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_ZINC = moltenType("molten_zinc");
    /** Copper's alloys -- see {@code data/feedback/mixture/}. Each is its own real fluid, not a
     * tint of copper's, the same one-type-per-metal rule the pure metals already use. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_BRONZE = moltenType("molten_bronze");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_BRASS = moltenType("molten_brass");
    /** Tin's solders -- basic electronics phase 3, see {@code data/feedback/mixture/solder_*.json}. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_SOLDER_LEADED = moltenType("molten_solder_leaded");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_SOLDER_FREE = moltenType("molten_solder_free");

    /** Glass melts and casts exactly like a metal -- see {@code FItems#GLASS_INGOT}. */
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_GLASS = moltenType("molten_glass");

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_LEAD = moltenType("molten_lead");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_SILVER = moltenType("molten_silver");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_URANIUM = moltenType("molten_uranium");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_CAST_NETHERITE = moltenType("molten_cast_netherite");
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_SLAG = moltenType("molten_slag");

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

    /** Petroleum and its cuts (petrochemistry spec 4.1): plain fluids, never placed; the grade and temperature ride on the stack. */
    public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL = petroType("crude_oil", 850, 3000, 300);
    public static final DeferredHolder<FluidType, FluidType> HEAVY_OIL = petroType("heavy_oil", 850, 3000, 300);
    public static final DeferredHolder<FluidType, FluidType> NAPHTHA = petroType("naphtha", 850, 3000, 300);
    public static final DeferredHolder<FluidType, FluidType> KEROSENE = petroType("kerosene", 850, 3000, 300);
    public static final DeferredHolder<FluidType, FluidType> DIESEL = petroType("diesel", 850, 3000, 300);
    public static final DeferredHolder<FluidType, FluidType> NAPHTHA_VAPOUR = petroType("naphtha_vapour", -3, 100, 400);
    public static final DeferredHolder<FluidType, FluidType> KEROSENE_VAPOUR = petroType("kerosene_vapour", -3, 100, 400);
    public static final DeferredHolder<FluidType, FluidType> DIESEL_VAPOUR = petroType("diesel_vapour", -3, 100, 400);

    public static final DeferredHolder<FluidType, FluidType> SOUL_OIL = petroType("soul_oil", 900, 4000, 300);
    public static final DeferredHolder<FluidType, FluidType> POLYETHYLENE_MELT = petroType("polyethylene_melt", 900, 4000, 300);
    public static final DeferredHolder<FluidType, FluidType> SOUL_RESIN_MELT = petroType("soul_resin_melt", 900, 4000, 300);
    public static final DeferredHolder<FluidType, FluidType> ETHYLENE = petroType("ethylene", -2, 100, 300);
    public static final DeferredHolder<FluidType, FluidType> WISP_GAS = petroType("wisp_gas", -2, 100, 300);
    public static final DeferredHolder<FluidType, FluidType> AIR = petroType("air", -1, 20, 300);
    public static final DeferredHolder<FluidType, FluidType> OXYGEN = petroType("oxygen", -1, 20, 300);
    public static final DeferredHolder<FluidType, FluidType> HYDROGEN = petroType("hydrogen", -4, 10, 300);

    /** Chemistry: acids, bases, brine, the chlorine and nitrogen gases, and the aluminium smelter's bath. Negative density rises. */
    public static final DeferredHolder<FluidType, FluidType> BRINE = petroType("brine", 1200, 1000, 300);
    public static final DeferredHolder<FluidType, FluidType> LYE = petroType("lye", 1430, 2000, 300);
    public static final DeferredHolder<FluidType, FluidType> HYDROCHLORIC_ACID = petroType("hydrochloric_acid", 1180, 1500, 300);
    public static final DeferredHolder<FluidType, FluidType> METHANE = petroType("methane", -3, 100, 300);
    public static final DeferredHolder<FluidType, FluidType> CHLOROFORM = petroType("chloroform", 1480, 600, 300);
    public static final DeferredHolder<FluidType, FluidType> CHLORODIFLUOROMETHANE = petroType("chlorodifluoromethane", -2, 100, 300);
    public static final DeferredHolder<FluidType, FluidType> HYDROFLUORIC_ACID = petroType("hydrofluoric_acid", 1150, 1500, 300);
    public static final DeferredHolder<FluidType, FluidType> TETRAFLUOROETHYLENE = petroType("tetrafluoroethylene", -2, 100, 300);
    public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID = petroType("sulfuric_acid", 1830, 2500, 300);
    public static final DeferredHolder<FluidType, FluidType> HYPOCHLORITE = petroType("hypochlorite", 1100, 1000, 300);
    public static final DeferredHolder<FluidType, FluidType> CHLORINE = petroType("chlorine", 3, 13, 300);
    public static final DeferredHolder<FluidType, FluidType> SULFUR_DIOXIDE = petroType("sulfur_dioxide", 3, 13, 300);
    public static final DeferredHolder<FluidType, FluidType> AMMONIA = petroType("ammonia", -1, 10, 300);
    /** A heavy, volatile, toxic gas (osmium tetroxide boils at 130 Tu): sinks, so it is positive like chlorine. */
    public static final DeferredHolder<FluidType, FluidType> OSMIUM_TETROXIDE = petroType("osmium_tetroxide", 4, 20, 300);
    public static final DeferredHolder<FluidType, FluidType> NITROGEN = petroType("nitrogen", -1, 20, 300);
    /** The air gases of the cryogenic route: argon (a gas) and the two cold liquids, liquid air and oxygen-rich liquid. */
    public static final DeferredHolder<FluidType, FluidType> ARGON = petroType("argon", -1, 20, 300);
    /** The lightest noble gas: lift gas that never burns. */
    public static final DeferredHolder<FluidType, FluidType> HELIUM = petroType("helium", -4, 10, 300);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_AIR = petroType("liquid_air", 870, 200, 80);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN = petroType("liquid_oxygen", 1140, 200, 90);
    /** The Nether arc: mixed magma melt (denser than any metal slag, hot) and wisp gas liquefied. */
    public static final DeferredHolder<FluidType, FluidType> MAGMA_MELT = petroType("magma_melt", 2800, 8000, 1900);
    public static final DeferredHolder<FluidType, FluidType> MAGMA_MELT_RICH = petroType("magma_melt_rich", 2900, 8000, 1950);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_WISP_GAS = petroType("liquid_wisp_gas", 600, 200, 90);
    public static final DeferredHolder<FluidType, FluidType> CRYOLITE_BATH = petroType("cryolite_bath", 2100, 6000, 1500);

    /** Refined chorus alkaloid in solution; what an Ender Port pays in (tag {@code feedback:teleportant}). */
    public static final DeferredHolder<FluidType, FluidType> TELEPORTANT = petroType("teleportant", 1050, 800, 300);

    /** The unstable spatial solvent boiled off a column's cut: a vapour, rises. */
    public static final DeferredHolder<FluidType, FluidType> UNSTABLE_SOLVENT_VAPOUR = petroType("unstable_solvent_vapour", -3, 100, 400);

    private static DeferredHolder<FluidType, FluidType> petroType(String name, int density, int viscosity, int temperature) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid.feedback." + name)
                .density(density)
                .viscosity(viscosity)
                .temperature(temperature)
                .canConvertToSource(false)
                .canSwim(false)
                .canDrown(false)
                .canExtinguish(false)
                .canHydrate(false)
                .supportsBoating(false)));
    }

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
