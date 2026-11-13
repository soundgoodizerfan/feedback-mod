// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's one crafting-table recipe serializer, and it is likely to stay the only one.
 *
 * <p>Feedback's processes are datapack tables rather than {@code RecipeType}s, on purpose -- a
 * machine that owned a recipe list would know when it had finished, which is the one thing this
 * mod's machines must never do. What lives here is the exception that proves it: hand work, where
 * the crafting table is vanilla's recipe machine and the player is the one who decides to stop.
 *
 * @see io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe
 */
public class FRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Feedback.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<HandDeformationRecipe>>
            HAND_DEFORMATION = SERIALIZERS.register("hand_deformation",
                    () -> new SimpleCraftingRecipeSerializer<>(HandDeformationRecipe::new));

    /** A magazine and loose rounds -> that magazine, fuller (guns spec section 5). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe>>
            MAGAZINE_LOAD = SERIALIZERS.register("magazine_load",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe::new));

    /** A hammer and hot metal pieces -> a wrench or a crowbar (heat spec section 2). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<io.github.soundgoodizerfan.feedback.item.HotToolRecipe>>
            HOT_TOOL = SERIALIZERS.register("hot_tool", io.github.soundgoodizerfan.feedback.item.HotToolRecipe.Serializer::new);

    /** A ringing crystal plate and a metal plate -> a mass probe carrying the plate's cut. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.MassProbeRecipe>>
            MASS_PROBE = SERIALIZERS.register("mass_probe",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.MassProbeRecipe::new));

    /** Substrate, trace stock and solder -> an etched, printed or pasted board (basic electronics phase 3). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.BoardRecipe>>
            BOARD = SERIALIZERS.register("board",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.BoardRecipe::new));

    /** An instrument and a live circuit -> that instrument fitted with the circuit. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.InstrumentFitRecipe>>
            INSTRUMENT_FIT = SERIALIZERS.register("instrument_fit",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.InstrumentFitRecipe::new));

    /** A Slider Joint and four wires of a metal -> that slider with the metal as its spring. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.bearing.SliderSpringRecipe>>
            SLIDER_SPRING = SERIALIZERS.register("slider_spring",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.bearing.SliderSpringRecipe::new));

    /** A grapple and a fitting (extra coil, winch, barbed head) -> that grapple carrying it. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.GrappleUpgradeRecipe>>
            GRAPPLE_UPGRADE = SERIALIZERS.register("grapple_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.GrappleUpgradeRecipe::new));

    /** Two rods of a metal and a grip -> tongs made of exactly those (hot-contact protection). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.TongsRecipe>>
            TONGS = SERIALIZERS.register("tongs",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.TongsRecipe::new));

    /** Wooden and stone pickaxe, axe, shovel -> the canonical modular tool (the bootstrap exception). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.modular.item.BootstrapToolRecipe>>
            BOOTSTRAP_TOOL = SERIALIZERS.register("bootstrap_tool",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.modular.item.BootstrapToolRecipe::new));

    /** The plain crafting table's assembly of every simple modular type from its parts. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.modular.engineering.ModularCraftingRecipe>>
            MODULAR_ASSEMBLY = SERIALIZERS.register("modular_assembly",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.modular.engineering.ModularCraftingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.modular.engineering.ModularSwapRecipe>>
            MODULAR_SWAP = SERIALIZERS.register("modular_swap",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.modular.engineering.ModularSwapRecipe::new));

    /** A grid -> one prebaked specimen of a treatise-gated modular type, so taking it apart can write the treatise. */
    public static final DeferredHolder<RecipeSerializer<?>, io.github.soundgoodizerfan.feedback.modular.treatise.BootstrapSpecimenRecipe.Serializer>
            BOOTSTRAP_SPECIMEN = SERIALIZERS.register("bootstrap_specimen",
                    io.github.soundgoodizerfan.feedback.modular.treatise.BootstrapSpecimenRecipe.Serializer::new);

    /** Plates and straps -> a bracer, sash or back piece baked from them (combat gear). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.combat.gear.GearRecipe>>
            GEAR = SERIALIZERS.register("gear",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.combat.gear.GearRecipe::new));

    /** Three items of one XP-conducting material in a row -> XP wire in that material. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.xp.XpWireRecipe>>
            XP_WIRE = SERIALIZERS.register("xp_wire",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.xp.XpWireRecipe::new));

    /** Three plates of one material over a stick -> a skillet in that material. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.skillet.SkilletRecipe>>
            SKILLET = SERIALIZERS.register("skillet",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.skillet.SkilletRecipe::new));

    /** Five plates of one material in a U -> a stockpot in that material. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotRecipe>>
            STOCKPOT = SERIALIZERS.register("stockpot",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotRecipe::new));

    /** Three wires of one conductor and three paper -> cable in that conductor. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeTipRecipe>>
            ARC_ELECTRODE_TIP = SERIALIZERS.register("arc_electrode_tip",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeTipRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeRecipe>>
            ARC_ELECTRODE = SERIALIZERS.register("arc_electrode",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe>>
            CABLE = SERIALIZERS.register("cable",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe::new));

    /** The breaker with cable in place of wire -> a breaker of the cable's gauge. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.breaker.BreakerRecipe>>
            BREAKER = SERIALIZERS.register("breaker",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.breaker.BreakerRecipe::new));

    /** Anything that burns on a stick -> four torches. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.process.TorchRecipe>>
            TORCH = SERIALIZERS.register("torch",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.process.TorchRecipe::new));

    /** A cable on two rods -> two fence posts carrying that cable's conductor. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.cable.FenceRecipe>>
            FENCE = SERIALIZERS.register("fence",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.cable.FenceRecipe::new));

    /** Two or more of one food -> that many, with the lowest spoilage of them, so they stack. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.process.FoodMergeRecipe>>
            FOOD_MERGE = SERIALIZERS.register("food_merge",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.process.FoodMergeRecipe::new));

    /** One food and one salt -> that food, salted (food spec 8). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.process.FoodSaltingRecipe>>
            FOOD_SALTING = SERIALIZERS.register("food_salting",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.process.FoodSaltingRecipe::new));

    /** One jar and one food or serving -> a jar of it (food spec 8, Canning). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.process.JarFillRecipe>>
            JAR_FILL = SERIALIZERS.register("jar_fill",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.process.JarFillRecipe::new));

    public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES, Feedback.MOD_ID);

    /** Any item of a form whose material's figure is in range -- see {@code MaterialIngredient}. */
    public static final DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>,
            net.neoforged.neoforge.common.crafting.IngredientType<io.github.soundgoodizerfan.feedback.process.MaterialIngredient>>
            MATERIAL_INGREDIENT = INGREDIENT_TYPES.register("material",
                    () -> io.github.soundgoodizerfan.feedback.process.MaterialIngredient.TYPE);

    /** One pearl and two metal plates -> a bonded pair of ender ports. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.ender.EnderPairRecipe>>
            ENDER_PAIR = SERIALIZERS.register("ender_pair",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.ender.EnderPairRecipe::new));

    /** A carrier and a dye -> the carrier addressed in that colour. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.tube.CarrierDyeRecipe>>
            CARRIER_DYE = SERIALIZERS.register("carrier_dye",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.tube.CarrierDyeRecipe::new));

    private FRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
        INGREDIENT_TYPES.register(modBus);
    }
}
