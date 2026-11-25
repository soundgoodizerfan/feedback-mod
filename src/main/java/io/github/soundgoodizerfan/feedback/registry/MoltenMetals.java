// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
 * {@code CastingTable}). Only alloys, glasses and slags that no material file names keep a
 * hand-registered fluid in {@link FFluids}.
 * Steel is made by a process but melts like any metal once made; its tempers (a state of the
 * solid) are not listed.
 */
public final class MoltenMetals {

    /** Material ids that get a generated molten fluid. A new metal adds its id here. */
    public static final List<String> IDS = Stream.of(List.of(
            "tin", "copper", "zinc", "lead", "gold", "silver", "iron", "uranium",
            "chromium", "nickel", "cobalt", "manganese", "aluminium", "titanium", "tungsten", "thorium",
            "silicon", "silicon_crude", "silica_glass", "optical_glass", "steel", "stainless_steel", "manganese_steel",
            "tungsten_carbide", "titanium_carbide", "thorium_tungsten", "cermet", "redstone_chromium",
            "magnesium", "lithium", "bismuth", "beryllium", "platinum", "gallium", "molybdenum", "palladium", "cadmium",
            "duralumin", "nichrome", "woods_metal", "beryllium_copper", "invar", "electrum", "amethyst_bronze", "borosilicate_glass", "uranium_glass",
            "antimony", "vanadium", "indium", "mercury", "galinstan", "constantan", "cupronickel", "alnico", "high_speed_steel", "titanium_6al_4v", "stellite", "pewter", "babbitt", "bell_metal", "shakudo", "aluminium_foam", "lumen_glass",
            "thaumium", "ruby", "jade", "garnet", "tourmaline"), essentiaIds()).flatMap(List::stream).toList();

    /** Each Thaumaturge aspect's essentia and the iron alloy it makes ({@code scripts/gen_essentia_materials.py} writes the files). */
    private static List<String> essentiaIds() {
        // A method, not a constant: IDS is initialised above this point in the file.
        return Stream.of("aer", "terra", "ignis", "aqua", "ordo", "perditio", "vacuos", "lux", "motus", "gelum", "vitreus", "metallum",
                "victus", "permutatio", "potentia", "volatus", "vinculum", "tenebrae", "mortuus", "herba", "praecantatio", "instrumentum",
                "alienis", "spiritus", "auram", "alkimia", "vitium", "fabrico", "machina", "exanimis", "bestia", "cognitio", "sensus",
                "aversio", "praemunio", "desiderium", "humanus")
                .flatMap(a -> Stream.of("essentia_" + a, "thaumium_" + a)).toList();
    }

    /**
     * Materials that are liquid at room temperature: their fluid is a cool, dark, runny liquid held in vessels and buckets, not a glowing
     * melt, and a loose solid piece of one (frozen in a Cryocooler basin) melts back where it lies ({@code FormItem}).
     */
    // Named here because fluids register before any datapack material is read, so the melting point cannot be asked for yet; FormItem reads the real melting point.
    public static final Set<String> LIQUID_AT_AMBIENT = Set.of("mercury", "galinstan");

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Feedback.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Feedback.MOD_ID);

    private static final Map<String, DeferredHolder<FluidType, FluidType>> TYPE_HOLDERS = new LinkedHashMap<>();

    static {
        for (String id : IDS) {
            // Essentia and thaumium exist only with Thaumaturge (their material files carry the condition).
            if ((id.startsWith("thaumium") || id.startsWith("essentia_")) && !io.github.soundgoodizerfan.feedback.core.LoadConditions.DATAGEN && !net.neoforged.fml.ModList.get().isLoaded("thaumaturge"))
                continue;
            String name = "molten_" + id;
            boolean cool = LIQUID_AT_AMBIENT.contains(id);
            DeferredHolder<FluidType, FluidType> type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.feedback." + name)
                    .lightLevel(cool ? 0 : 15)
                    .density(cool ? 13530 : 7000)
                    .viscosity(cool ? 1550 : 6000)
                    .temperature(cool ? 293 : 1300)
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
