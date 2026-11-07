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
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.process.ClientDeformations;
import io.github.soundgoodizerfan.feedback.process.ClientRemovals;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.process.ClientThermalProcesses;
import io.github.soundgoodizerfan.feedback.process.Deformation;
import io.github.soundgoodizerfan.feedback.process.Removal;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.process.VanillaFallback;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Puts the deformation table and the thermal process table in the recipe browser without
 * pretending either is a recipe book.
 *
 * <h2>No vanilla recipe type, deliberately</h2>
 * JEI's {@code IRecipeCategory<T>} is generic over any type at all, so a category can be built
 * straight from {@link Deformation} -- which means the mod never has to register a vanilla
 * {@code RecipeType}, never appears in the recipe book, and never acquires an API that would let
 * something ask the hammer what it can make. The hammer genuinely does not know; a compatibility
 * layer that implied otherwise would be the mod lying about its own central claim.
 *
 * <h2>Why the recipes arrive late</h2>
 * JEI starts its runtime from the vanilla recipe sync, which lands during the configuration phase,
 * while our table arrives on datapack sync in the play phase. So {@code registerRecipes} would
 * almost always see an empty table on a first join, and adding there as well as here would produce
 * every card twice. Pushing once, from the runtime, is the version with one code path.
 * <p>
 * {@code hideRecipes} before each push is what makes a second push idempotent: a {@code /reload}
 * can deliver a fresh table without JEI restarting, and without this the old cards would linger
 * alongside the new ones.
 *
 * <h2>One tab for both, not two</h2>
 * A first pass gave the pooled vanilla/modded cooking recipes their own category and their own
 * tab. Wrong call: they are the same question {@link ThermalProcessCategory} already answers --
 * what a fire does to a material -- and a second tab just for "we didn't author this one" is a
 * distinction the player has no reason to care about. {@link ThermalProcessTable} entries and
 * pooled ones are both wrapped as a {@link ThermalProcess} and published under one
 * {@link ThermalProcessCategory#TYPE}. See {@link #pooled} for how a pooled recipe is turned into
 * one without inventing figures nobody wrote down: it only ever carries the two universal floors
 * every vessel already enforces, {@link FTuning#METAL_MIN_TU} and {@link FTuning#FOOD_MAX_TU}.
 *
 * <h2>Vanilla's own categories are hidden here too</h2>
 * The Furnace, Smoker and Blast Furnace never touch a vanilla block class any more (§4f), so
 * JEI's built-in smelting/blasting/smoking categories describe blocks that no longer exist in a
 * normal playthrough.
 */
@JeiPlugin
public class FeedbackJeiPlugin implements IModPlugin {

    @Nullable
    private static IJeiRuntime runtime;

    /** The item stack JEI's ingredient list or bookmark overlay has under the mouse, or empty: lets the stand screen preview what hovering it would add. */
    public static net.minecraft.world.item.ItemStack hoveredStack() {
        if (runtime == null)
            return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack stack = runtime.getIngredientListOverlay().getIngredientUnderMouse(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        if (stack == null)
            stack = runtime.getBookmarkOverlay().getIngredientUnderMouse(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        return stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack;
    }
    @Nullable
    private static List<Deformation> published;
    @Nullable
    private static List<Removal> publishedRemoval;
    @Nullable
    private static List<ThermalProcess> publishedThermal;

    @Override
    public ResourceLocation getPluginUid() {
        return Feedback.id("jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe.class, new CableCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeRecipe.class, new ElectrodeCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeTipRecipe.class, new ElectrodeTipCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.skillet.SkilletRecipe.class, new SkilletCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotRecipe.class, new StockpotCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.modular.BootstrapToolRecipe.class, new BootstrapGloveExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.item.GrappleUpgradeRecipe.class, new GrappleUpgradeCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.item.HotToolRecipe.class, new HotToolCraftingExtension());
        ElectronicsCrafting.register(registration);
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.ender.EnderPairRecipe.class, new EnderPairCraftingExtension());
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.tube.CarrierDyeRecipe.class, new CarrierDyeCraftingExtension());
        CodeRecipeExamples.register(registration);
    }

    /**
     * A cut, a board and a circuit are data components, so without this every cut of probe and
     * crystal plate, and every board, would collapse into one JEI entry.
     */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack> cut = new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return io.github.soundgoodizerfan.feedback.process.Cut.of(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return io.github.soundgoodizerfan.feedback.process.Cut.of(stack).getSerializedName();
            }
        };
        registration.registerSubtypeInterpreter(FItems.MASS_PROBE.get(), cut);
        io.github.soundgoodizerfan.feedback.registry.FormItems.all().stream()
                .filter(g -> g.form().equals("crystal_plate") && g.block() == null)
                .forEach(g -> registration.registerSubtypeInterpreter(g.item().get(), cut));
        registration.registerSubtypeInterpreter(FItems.TRACE_BOARD.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get()));
        registration.registerSubtypeInterpreter(FItems.CIRCUIT.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get()));
    }

    private static mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack> component(
            net.minecraft.core.component.DataComponentType<?> type) {
        return new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return stack.get(type);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return String.valueOf(stack.get(type));
            }
        };
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new DeformationCategory(registration.getJeiHelpers().getGuiHelper()),
                new RemovalCategory(registration.getJeiHelpers().getGuiHelper()),
                new ThermalProcessCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreVeinCategory(registration.getJeiHelpers().getGuiHelper()),
                new BasinCategory(registration.getJeiHelpers().getGuiHelper()),
                new MaterialStatsCategory(registration.getJeiHelpers().getGuiHelper()),
                new BrewingIngredientCategory(registration.getJeiHelpers().getGuiHelper()),
                new SeparationCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreProcessingCategory(registration.getJeiHelpers().getGuiHelper()),
                new FuelCategory(registration.getJeiHelpers().getGuiHelper()),
                new PotionShapeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MixtureCategory(registration.getJeiHelpers().getGuiHelper()),
                new GoodsCategory(registration.getJeiHelpers().getGuiHelper()),
                new BuysCategory(registration.getJeiHelpers().getGuiHelper()),
                new GunOptionCategory(registration.getJeiHelpers().getGuiHelper()),
                new GunBlueprintPoolCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolderingCategory(registration.getJeiHelpers().getGuiHelper()),
                new CircuitChainCategory(registration.getJeiHelpers().getGuiHelper()),
                new ToolTableToolCategory(registration.getJeiHelpers().getGuiHelper()),
                new ToolTableToolCategory(registration.getJeiHelpers().getGuiHelper(), ToolTableToolCategory.GUN_TYPE, "feedback.jei.gun_table_gun",
                        new net.minecraft.world.item.ItemStack(io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get())),
                new ToolTablePartCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * Info pages for what happens in the world with no table behind it. Verbs only, never values
     * or solutions (philosophy 8, "documentation is a free sense"): what to do with the thing, not
     * what figure to aim for.
     */
    /** The display-only crafting cards; the bootstrap tools need the modular types as well as the materials, so both tables' arrival publishes them. */
    private static void publishExamples() {
        var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
        var examples = new ArrayList<>(ElectronicsCrafting.examples(materials));
        examples.addAll(CodeRecipeExamples.examples(materials));
        show(RecipeTypes.CRAFTING, examples);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (var form : io.github.soundgoodizerfan.feedback.registry.FormItems.all()) {
            var made = io.github.soundgoodizerfan.feedback.registry.FormItems.materials().get(form.material());
            if (made != null && made.food().isPresent())
                registration.addIngredientInfo(form.item().get(), Component.translatable("feedback.jei.info.food_form"));
        }
        registration.addIngredientInfo(FItems.PUDDLING_FURNACE.get(), Component.translatable("feedback.jei.info.puddling_furnace"));
        registration.addIngredientInfo(FItems.IRON_BLOOM.get(), Component.translatable("feedback.jei.info.iron_bloom"));
        registration.addIngredientInfo(FItems.BLOOMERY_SLAG.get(), Component.translatable("feedback.jei.info.bloomery_slag"));
        registration.addIngredientInfo(FItems.GRAPPLE.get(), Component.translatable("feedback.jei.info.grapple"));
        registration.addIngredientInfo(FItems.GRAPPLE_WINCH.get(), Component.translatable("feedback.jei.info.grapple_winch"));
        registration.addIngredientInfo(FItems.ARC_HEARTH.get(), Component.translatable("feedback.jei.info.arc_hearth"));
        registration.addIngredientInfo(net.minecraft.world.item.Items.HEAVY_CORE, Component.translatable("feedback.jei.info.heavy_core"));
        registration.addIngredientInfo(FItems.INDUCTION_COIL.get(), Component.translatable("feedback.jei.info.induction_coil"));
        registration.addIngredientInfo(FItems.INDUCTION_STOVE.get(), Component.translatable("feedback.jei.info.induction_stove"));
        registration.addIngredientInfo(FItems.PREHEAT_COIL.get(), Component.translatable("feedback.jei.info.preheat_coil"));
        registration.addIngredientInfo(FItems.ARC_HEAD.get(), Component.translatable("feedback.jei.info.arc_head"));
        registration.addIngredientInfo(FItems.REACTOR_CORE.get(), Component.translatable("feedback.jei.info.reactor_core"));
        registration.addIngredientInfo(FItems.FUEL_CHANNEL.get(), Component.translatable("feedback.jei.info.fuel_channel"));
        registration.addIngredientInfo(FItems.CONTROL_ASSEMBLY.get(), Component.translatable("feedback.jei.info.control_assembly"));
        registration.addIngredientInfo(FItems.SCRAM_TRIP.get(), Component.translatable("feedback.jei.info.scram_trip"));
        registration.addIngredientInfo(FItems.GRAPHITE_MODERATOR.get(), Component.translatable("feedback.jei.info.graphite_moderator"));
        registration.addIngredientInfo(FItems.FUEL_ROD.get(), Component.translatable("feedback.jei.info.fuel_rod"));
        registration.addIngredientInfo(FItems.THORIUM_FUEL_ROD.get(), Component.translatable("feedback.jei.info.thorium_fuel_rod"));
        registration.addIngredientInfo(FItems.SPENT_FUEL_ROD.get(), Component.translatable("feedback.jei.info.spent_fuel_rod"));
        registration.addIngredientInfo(FItems.CORE_SLAG.get(), Component.translatable("feedback.jei.info.core_slag"));
        registration.addIngredientInfo(FItems.POWER_CUTTER.get(), Component.translatable("feedback.jei.info.power_cutter"));
        registration.addIngredientInfo(FItems.SAW_HEAD.get(), Component.translatable("feedback.jei.info.saw_head"));
        registration.addIngredientInfo(FItems.PLASMA_HEAD.get(), Component.translatable("feedback.jei.info.plasma_head"));
        registration.addIngredientInfo(FItems.DATA_STICK.get(), Component.translatable("feedback.jei.info.data_stick"));
        registration.addIngredientInfo(FItems.LARGE_BOILER.get(), Component.translatable("feedback.jei.info.large_boiler"));
        registration.addIngredientInfo(FItems.ARC_ELECTRODE.get(), Component.translatable("feedback.jei.info.arc_electrode"));
        registration.addIngredientInfo(FItems.ELECTROLYTIC_CELL.get(), Component.translatable("feedback.jei.info.electrolytic_cell"));
        registration.addIngredientInfo(FModular.HAND_HAMMER.get(), Component.translatable("feedback.jei.info.hand_hammer"));
        registration.addIngredientInfo(FItems.SKILLET.get(), Component.translatable("feedback.jei.info.skillet"));
        registration.addIngredientInfo(FItems.REFRIGERATION_UNIT.get(), Component.translatable("feedback.jei.info.refrigeration_unit"));
        registration.addIngredientInfo(FItems.CRYOCOOLER.get(), Component.translatable("feedback.jei.info.cryocooler"));
        registration.addIngredientInfo(net.minecraft.world.item.Items.EMERALD, Component.translatable("feedback.jei.info.commissions"));
        registration.addIngredientInfo(FModular.SPATULA.get(), Component.translatable("feedback.jei.info.spatula"));
        registration.addIngredientInfo(FItems.TASTING_SPOON.get(), Component.translatable("feedback.jei.info.tasting_spoon"));
        registration.addIngredientInfo(FModular.SPOON.get(), Component.translatable("feedback.jei.info.spoon"));
        registration.addIngredientInfo(FItems.TONGS.get(), Component.translatable("feedback.jei.info.tongs"));
        registration.addIngredientInfo(FItems.WRENCH.get(), Component.translatable("feedback.jei.info.hot_tool"));
        registration.addIngredientInfo(FItems.CROWBAR.get(), Component.translatable("feedback.jei.info.hot_tool"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BACKPACK.get(), Component.translatable("feedback.jei.info.backpack"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BEDROLL.get(), Component.translatable("feedback.jei.info.bedroll"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.GLOVE.get(), Component.translatable("feedback.jei.info.gloves"));
        // Basic electronics: machines with no recipe, and the items an operation turns out.
        info(registration, FItems.COHERER.get(), "coherer");
        info(registration, FItems.ROLLING_MILL.get(), "rolling_mill");
        info(registration, FItems.MORTAR.get(), "mortar");
        info(registration, FModular.BATTERY.get(), "battery");
        info(registration, FItems.HYDROMETER.get(), "hydrometer");
        info(registration, FItems.RECTIFIER.get(), "rectifier");
        info(registration, FItems.CONVERTER.get(), "converter");
        info(registration, FItems.TUNED_EYE.get(), "tuned_eye");
        info(registration, FItems.SYNTHETIC_PEARL.get(), "synthetic_pearl");
        info(registration, FItems.POROUS_AGGREGATE.get(), "porous_aggregate");
        info(registration, FItems.CONVERTER_TAP.get(), "converter_tap");
        info(registration, FItems.HEATSINK.get(), "heatsink");
        info(registration, FItems.THERMAL_STORE.get(), "thermal_store");
        info(registration, FItems.WHISKER.get(), "whisker");
        info(registration, FItems.RELAY.get(), "relay");
        info(registration, FItems.COMPONENT_TESTER.get(), "component_tester");
        info(registration, FItems.OSCILLOSCOPE.get(), "oscilloscope");
        info(registration, FItems.PHOSPHOR_SCREEN.get(), "phosphor_screen");
        info(registration, FItems.REFLOW_OVEN.get(), "reflow_oven");
        info(registration, FItems.SOLDERING_IRON.get(), "soldering_iron");
        info(registration, FItems.ARC_WELDER.get(), "arc_welder");
        info(registration, FItems.DIAMOND_GRINDING_WHEEL.get(), "diamond_grinding_wheel");
        info(registration, FItems.MASS_PROBE.get(), "mass_probe");
        info(registration, FItems.TRACE_BOARD.get(), "trace_board");
        info(registration, FItems.CIRCUIT.get(), "circuit");
        info(registration, FItems.GALENA.get(), "galena");
        info(registration, FItems.PYRITE.get(), "pyrite");
        // Food: the crafting-grid recipes that read a food's state have no fixed output for JEI to show.
        info(registration, FItems.JAR.get(), "jar");
        info(registration, FItems.JARRED_FOOD.get(), "jarred_food");
        info(registration, FItems.SALT.get(), "salt");
        info(registration, FItems.DOUGH.get(), "dough");
        // Potions: one generic bottle and salts entry each. Per-effect entries would list every node, spoiling the ones not yet made.
        info(registration, FItems.BREWING_STAND.get(), "brewing_stand");
        info(registration, FItems.VACUUM_DRYER.get(), "vacuum_dryer");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), "mixer");
        // Stations, blueprints and the magazine (loaded by a code recipe with no fixed grid).
        info(registration, FModular.PREP_TABLE_ITEM.get(), "prep_table");
        info(registration, FModular.SMITHING_TABLE_ITEM.get(), "smithing_table");
        info(registration, FModular.ANVIL_ITEM.get(), "anvil");
        info(registration, FModular.WOODWORKING_TABLE_ITEM.get(), "woodworking_table");
        info(registration, FModular.STONECUTTER_ITEM.get(), "stonecutter");
        info(registration, FModular.BLUEPRINT_TABLE_ITEM.get(), "blueprint_table");
        info(registration, FModular.ENCHANTING_TABLE_ITEM.get(), "enchanting_table");
        info(registration, FModular.BLUEPRINT.get(), "modular_blueprint");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_ITEM.get(), "engine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.TRANSMISSION_ITEM.get(), "transmission");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get(), "gun_table");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get(), "reloading_press");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE_ITEM.get(), "magazine");
        // Machines and instruments with no catalyst category; each page says what the thing is and how to use it.
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.FIREBOX.get().asItem(), "firebox");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STOCKPOT.get().asItem(), "stockpot");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DRYING_RACK.get().asItem(), "drying_rack");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.HEARTH.get().asItem(), "hearth");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.ICE_BOX.get().asItem(), "ice_box");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.WELDER.get(), "welder");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.CONTROLLER.get().asItem(), "controller");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.PROGRAMMER.get().asItem(), "programmer");
        info(registration, FItems.THERMOMETER.get(), "thermometer");
        info(registration, FItems.PYROMETER.get(), "pyrometer");
        info(registration, FItems.PRESSURE_GAUGE.get(), "pressure_gauge");
        info(registration, FModular.GEIGER_COUNTER.get(), "geiger_counter");
        info(registration, FModular.DOSIMETER.get(), "dosimeter");
        info(registration, FItems.TEMPERATURE_SENSOR.get(), "temperature_sensor");
        info(registration, FItems.ELECTROSCOPE.get(), "electroscope");
        info(registration, FItems.GALVANOMETER.get(), "galvanometer");
        info(registration, FItems.CURRENT_SENSOR.get(), "current_sensor");
        info(registration, FModular.BALANCE_SCALE.get(), "balance_scale");
        info(registration, FModular.SURVEY_METER.get(), "survey_meter");
        info(registration, FItems.XP_METER.get(), "xp_meter");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BURNER.get().asItem(), "burner");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BREAKER.get().asItem(), "breaker");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BELLOWS.get().asItem(), "bellows");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BIMETALLIC_STRIP.get().asItem(), "bimetallic_strip");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.COMPRESSOR.get().asItem(), "compressor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DAMPER.get().asItem(), "damper");
        info(registration, FModular.DYNAMOTOR.get(), "dynamotor");
        info(registration, FModular.CONVEYOR_BELT.get(), "conveyor_belt");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FLogistics.HEAD_ITEM.get(), "belt_head");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FItems.REDSTONE_EMITTER.get(), "redstone_emitter");
        io.github.soundgoodizerfan.feedback.registry.FAutomation.SENSOR_ITEMS.forEach((kind, item) -> info(registration, item.get(), kind.id()));
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.PIVOT_MAST_ITEM.get(), "pivot_mast");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.STOKER_ITEM.get(), "stoker");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.COUNTER_ITEM.get(), "counter");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.BOARD_ITEM.get(), "ledger_board");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.CROSS_ITEM.get(), "market_cross");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.DEPOT_ITEM.get(), "depot");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.DEPOT_LINK_CARD.get(), "depot_link_card");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.KILN_ZONE_ITEM.get(), "kiln_zone");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.CREEL_ITEM.get(), "creel");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FAutomation.FISH_WHEEL_ITEM.get(), "fish_wheel");
        for (var e : java.util.Map.of("dark_cell", io.github.soundgoodizerfan.feedback.registry.FAutomation.DARK_CELL_ITEM,
                "fall_plate", io.github.soundgoodizerfan.feedback.registry.FAutomation.FALL_PLATE_ITEM,
                "fire_plate", io.github.soundgoodizerfan.feedback.registry.FAutomation.FIRE_PLATE_ITEM,
                "crush_head", io.github.soundgoodizerfan.feedback.registry.FAutomation.CRUSH_HEAD_ITEM,
                "fume_port", io.github.soundgoodizerfan.feedback.registry.FAutomation.FUME_PORT_ITEM,
                "shock_plate", io.github.soundgoodizerfan.feedback.registry.FAutomation.SHOCK_PLATE_ITEM,
                "radiation_source", io.github.soundgoodizerfan.feedback.registry.FAutomation.RADIATION_SOURCE_ITEM).entrySet())
            info(registration, e.getValue().get(), e.getKey());
        io.github.soundgoodizerfan.feedback.registry.FAutomation.ARMS.forEach((len, item) -> info(registration, item.get(), "pivot_arm"));
        io.github.soundgoodizerfan.feedback.registry.FAutomation.TIPS.forEach((tip, item) -> info(registration, item.get(), "pivot_tip_" + tip.id()));
        info(registration, io.github.soundgoodizerfan.feedback.registry.FItems.ANALOG_ADAPTER.get(), "analog_adapter");
        io.github.soundgoodizerfan.feedback.registry.FLogistics.MODULE_ITEMS.forEach((m, item) -> info(registration, item.get(), "belt_module_" + m.id()));
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.ELECTROMAGNET.get().asItem(), "electromagnet");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.LAMP.get().asItem(), "lamp");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.GAS_TANK.get().asItem(), "gas_tank");
        info(registration, FItems.TUBE.get(), "tube");
        info(registration, FItems.LINED_TUBE.get(), "lined_tube");
        info(registration, FItems.WIDE_TUBE.get(), "wide_tube");
        info(registration, FItems.EVACUATED_TUBE.get(), "evacuated_tube");
        info(registration, FItems.TUBE_JUNCTION.get(), "tube_junction");
        info(registration, FItems.PROGRAMMED_JUNCTION.get(), "programmed_junction");
        info(registration, FItems.TUBE_STATION.get(), "tube_station");
        info(registration, FItems.SORTING_STATION.get(), "sorting_station");
        info(registration, FItems.SORT_GAUGE.get(), "sort_gauge");
        info(registration, FItems.VACUUM_PUMP.get(), "vacuum_pump");
        info(registration, FItems.LINEAR_MOTOR.get(), "linear_motor");
        info(registration, FItems.CARRIER.get(), "carrier");
        info(registration, FItems.MAGNETIC_CARRIER.get(), "magnetic_carrier");
        info(registration, FItems.POD.get(), "pod");
        info(registration, FItems.CREATIVE_AIR.get(), "creative_air");
        info(registration, FItems.ENDER_PORT.get(), "ender_port");
        info(registration, FItems.ENDER_ANCHOR.get(), "ender_anchor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.GAS_TURBINE.get().asItem(), "gas_turbine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.HEAT_EXCHANGER.get().asItem(), "heat_exchanger");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.HAND_CRANK.get().asItem(), "hand_crank");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.LEYDEN_JAR.get().asItem(), "leyden_jar");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.OVERSPEED_TRIP.get().asItem(), "overspeed_trip");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.PRESSURE_VESSEL.get().asItem(), "pressure_vessel");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.PUMP.get().asItem(), "pump");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STEAM_ENGINE.get().asItem(), "steam_engine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STEAM_TURBINE.get().asItem(), "steam_turbine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.TIMER.get().asItem(), "timer");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.WATER_WHEEL.get().asItem(), "water_wheel");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.GALVANIC_CELL.get().asItem(), "galvanic_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.SCULK_RELAY.get().asItem(), "sculk_relay");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DATA_SENSOR.get().asItem(), "data_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.REBOILER.get().asItem(), "reboiler");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.FLARE.get().asItem(), "flare");
        registration.addIngredientInfo(FItems.POTION.get(), Component.translatable("feedback.jei.info.potion"), Component.translatable("feedback.jei.info.potion_use"));
        registration.addIngredientInfo(FItems.POTION_SALTS.get(), Component.translatable("feedback.jei.info.potion_salts"), Component.translatable("feedback.jei.info.potion_salts_use"));
    }

    private static void info(IRecipeRegistration registration, Item item, String key) {
        registration.addIngredientInfo(item, Component.translatable("feedback.jei.info." + key));
    }

    /**
     * Every machine that answers for a category is listed, which is true and is not an identity
     * check: it says "this machine deforms" or "this machine removes", never "this machine makes
     * copper plate". Deformation used to list only the Mechanical Hammer, which was wrong the
     * moment the Wire Drawer, Rolling Mill and Mechanical Press started publishing into the same
     * category too -- see {@link DeformationCategory}'s own doc for the fuller correction.
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(GunOptionCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
        registration.addRecipeCatalysts(DeformationCategory.TYPE,
                FItems.MECHANICAL_HAMMER.get(), FItems.WIRE_DRAWER.get(),
                FItems.ROLLING_MILL.get(), FItems.MECHANICAL_PRESS.get(), FModular.HAND_HAMMER.get());
        registration.addRecipeCatalysts(RemovalCategory.TYPE,
                FItems.DRILL_PRESS.get(), FItems.LATHE.get(), FItems.GRINDING_WHEEL.get(), FItems.DIAMOND_GRINDING_WHEEL.get());
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, FItems.INGOT_MOLD.get(), FItems.BULLET_MOLD.get(), FItems.BALL_MOLD.get(), FItems.TUBE_MOLD.get(),
                FItems.GLASS_BLOCK_MOLD.get(), FItems.GLASS_PANE_MOLD.get(), FItems.GLASS_BOTTLE_MOLD.get(),
                FItems.PICKAXE_HEAD_MOLD.get(), FItems.AXE_HEAD_MOLD.get(), FItems.SHOVEL_HEAD_MOLD.get(), FItems.HOE_HEAD_MOLD.get(), FItems.SICKLE_HEAD_MOLD.get(), FItems.SAW_BLADE_MOLD.get(), FItems.ATOMISING_MOLD.get());
        registration.addRecipeCatalysts(BrewingIngredientCategory.TYPE, FItems.BREWING_STAND.get());
        registration.addRecipeCatalysts(SeparationCategory.TYPE, FItems.ELECTROMAGNET.get(), io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get());
        registration.addRecipeCatalysts(OreProcessingCategory.TYPE, FItems.MECHANICAL_HAMMER.get(), FItems.ROLLING_MILL.get(),
                io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), FItems.ELECTROMAGNET.get(), FItems.REACTOR.get(), FItems.ELECTROLYSER.get());
        registration.addRecipeCatalysts(FuelCategory.TYPE, FItems.FIREBOX.get(), FItems.BURNER.get());
        registration.addRecipeCatalysts(PotionShapeCategory.TYPE, FItems.BREWING_STAND.get());
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, net.minecraft.world.item.Items.WATER_BUCKET);
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, FItems.REACTOR.get());
        registration.addRecipeCatalysts(PressCategory.TYPE,
                io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get());
        // All eight: every vessel checks ThermalProcessTable before falling back to
        // VanillaFallback (§4f), so a hand-authored process like steel is genuinely reachable in
        // the Blast Furnace too, not only the crucible -- this was missing before the merge and
        // is a real fix, not just a consequence of it. Which vessel can actually reach a given
        // card's printed floor is a fact the card states in words or figures, not a fact this
        // list gets to decide (§15, no whitelists).
        Item[] vessels = {FItems.SMALL_CRUCIBLE.get(), FItems.LARGE_CRUCIBLE.get(), FItems.ARC_HEARTH.get(),
                FItems.FURNACE.get(), FItems.SMOKER.get(), FItems.BLAST_FURNACE.get(),
                FItems.IMPROVED_FURNACE.get(), FItems.KILN.get(), FItems.ANNEALING_FURNACE.get(), FItems.PUDDLING_FURNACE.get()};
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, vessels);
        // A dip is drawn from any vessel's tank (MoltenVessel); a mix is kept only by a crucible.
        registration.addRecipeCatalysts(MixtureCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get(),
                FItems.SMALL_CRUCIBLE.get(), FItems.LARGE_CRUCIBLE.get(), FItems.ARC_HEARTH.get());
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, FItems.BOILER.get(), FItems.LARGE_BOILER.get(), FItems.CONDENSER.get(), FItems.REBOILER.get());
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, FItems.ELECTROLYSER.get());
        registration.addRecipeCatalysts(CircuitChainCategory.TYPE, FItems.CIRCUIT.get());
        registration.addRecipeCatalysts(SolderingCategory.TYPE, FItems.SOLDERING_IRON.get(), FItems.REFLOW_OVEN.get());
        registration.addRecipeCatalysts(ToolTableToolCategory.TYPE, net.minecraft.world.item.Items.CRAFTING_TABLE);
        registration.addRecipeCatalysts(ToolTableToolCategory.GUN_TYPE, io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
        registration.addRecipeCatalysts(ToolTablePartCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.SMITHING_TABLE_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.ANVIL_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.WOODWORKING_TABLE_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.STONECUTTER_ITEM.get(),
                io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        published = null;
        publishedRemoval = null;
        publishedThermal = null;
        partsListed = false;
        // Our vessels are the only things left that can run these; vanilla's own built-in
        // categories now describe blocks that are permanently inert (§4f).
        IRecipeManager recipes = jeiRuntime.getRecipeManager();
        recipes.hideRecipeCategory(RecipeTypes.SMELTING);
        recipes.hideRecipeCategory(RecipeTypes.BLASTING);
        recipes.hideRecipeCategory(RecipeTypes.SMOKING);
        // Vanilla items the mod removed (campfire, tools, ...) list nowhere; the tag is the list, so a pack can extend it.
        var hidden = new ArrayList<net.minecraft.world.item.ItemStack>();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, Feedback.id("hidden_from_jei")))
                .forEach(item -> hidden.add(new net.minecraft.world.item.ItemStack(item.value())));
        // Vanilla ores are replaced by the mod's own deposits; the shared c:ores tag, minecraft namespace only, so other mods' ores stay.
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores")))
                .forEach(item -> {
                    if (item.value().builtInRegistryHolder().key().location().getNamespace().equals("minecraft"))
                        hidden.add(new net.minecraft.world.item.ItemStack(item.value()));
                });
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hidden);
        ClientDeformations.onChanged(FeedbackJeiPlugin::publish);
        ClientRemovals.onChanged(FeedbackJeiPlugin::publishRemoval);
        ClientThermalProcesses.onChanged(FeedbackJeiPlugin::publishThermal);
        io.github.soundgoodizerfan.feedback.process.ClientMaterials.onChanged(() -> {
            var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
            show(SolderingCategory.TYPE, SolderingCategory.cards(materials));
            show(CircuitChainCategory.TYPE, CircuitChainCategory.cards(materials));
            show(MaterialStatsCategory.TYPE, MaterialStatsCategory.cards(materials));
            show(BrewingIngredientCategory.TYPE, BrewingIngredientCategory.cards(materials));
            show(SeparationCategory.TYPE, SeparationCategory.cards(materials));
            show(OreProcessingCategory.TYPE, OreProcessingCategory.cards(materials));
            show(FuelCategory.TYPE, FuelCategory.cards(materials));
            show(PotionShapeCategory.TYPE, PotionShapeCategory.cards(materials));
            showPartIngredients(materials);
            publishExamples();
            publishPress();
            publishModular();
        });
        io.github.soundgoodizerfan.feedback.modular.ModularTypes.onChanged(() -> {
            publishExamples();
            publishModular();
        });
        ClientTable.MIXTURES.onChanged(() -> show(MixtureCategory.TYPE, ClientTable.MIXTURES.get()));
        ClientTable.GOODS.onChanged(() -> show(GoodsCategory.TYPE, ClientTable.GOODS.get()));
        ClientTable.GUN_OPTIONS.onChanged(() -> show(GunOptionCategory.TYPE, ClientTable.GUN_OPTIONS.get()));
        ClientTable.GUN_BLUEPRINTS.onChanged(() -> show(GunBlueprintPoolCategory.TYPE, ClientTable.GUN_BLUEPRINTS.get()));
        ClientTable.BUYS.onChanged(() -> show(BuysCategory.TYPE, ClientTable.BUYS.get()));
        ClientTable.ORE_VEINS.onChanged(() -> show(OreVeinCategory.TYPE, ClientTable.ORE_VEINS.get()));
        ClientTable.BASINS.onChanged(() -> show(BasinCategory.TYPE, ClientTable.BASINS.get()));
    }

    private static boolean partsListed;

    /**
     * The shaped parts (receiver, barrel, heads...) are in no creative tab -- they mean nothing without
     * a material -- so JEI never lists them. One stack per part, made of iron (else copper, else any material with an ingot),
     * is added once; a lookup on it lands on the Tool Table parts card, which already outputs every part.
     */
    private static void showPartIngredients(Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
        if (runtime == null || partsListed)
            return;
        var withIngot = materials.entrySet().stream().filter(e -> e.getValue().item(e.getKey(), "ingot").isPresent()).toList();
        // A plain, familiar metal reads as "a part"; map order alone picked leaded solder.
        var stock = java.util.stream.Stream.of("iron", "copper")
                .map(n -> withIngot.stream().filter(e -> e.getKey().getPath().equals(n)).findFirst())
                .flatMap(java.util.Optional::stream).findFirst()
                .or(() -> withIngot.stream().findFirst());
        if (stock.isEmpty())
            return;
        partsListed = true;
        List<ItemStack> parts = FModular.PARTS.stream().map(p -> {
            ItemStack s = new ItemStack(p.get());
            s.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(),
                    new io.github.soundgoodizerfan.feedback.process.MadeOf(stock.get().getKey(), "ingot", 1));
            return s;
        }).toList();
        runtime.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, parts);
    }

    /** What each category last showed, so a fresh table replaces it rather than doubling it. */
    private static final Map<RecipeType<?>, List<?>> shown = new HashMap<>();

    /**
     * Show {@code entries} for {@code type}, replacing what was shown. JEI keeps a recipe hidden by
     * equality, so hiding the old list and adding an equal new one leaves every card hidden (and
     * the tab empty): only recipes that went away are hidden, and the ones that stay are unhidden
     * and added again (a duplicate add is ignored).
     */
    @SuppressWarnings("unchecked")
    private static <T> void show(RecipeType<T> type, List<T> entries) {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        List<T> old = (List<T>) shown.put(type, entries);
        if (old != null) {
            List<T> gone = old.stream().filter(o -> !entries.contains(o)).toList();
            if (!gone.isEmpty())
                recipes.hideRecipes(type, gone);
            recipes.unhideRecipes(type, entries);
        }
        recipes.addRecipes(type, entries);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        published = null;
        publishedRemoval = null;
        publishedThermal = null;
        publishedPress = null;
        shown.clear();
        partsListed = false;
    }

    private static void publish() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (published != null)
            recipes.hideRecipes(DeformationCategory.TYPE, published);
        published = ClientDeformations.get();
        recipes.addRecipes(DeformationCategory.TYPE, published);
    }

    private static void publishRemoval() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedRemoval != null)
            recipes.hideRecipes(RemovalCategory.TYPE, publishedRemoval);
        publishedRemoval = withCuts(ClientRemovals.get());
        recipes.addRecipes(RemovalCategory.TYPE, publishedRemoval);
    }

    /**
     * A {@code finer} step takes a plate one cut finer, so one entry is two cards: coarse to standard
     * and standard to fine, each with its cut on the slots. The finest cut has no card.
     */
    private static List<Removal> withCuts(List<Removal> removals) {
        List<Removal> out = new ArrayList<>();
        for (Removal r : removals) {
            ItemStack[] accepted = r.input().getItems();
            if (!r.finer() || accepted.length == 0) {
                out.add(r);
                continue;
            }
            for (var cut : io.github.soundgoodizerfan.feedback.process.Cut.values()) {
                if (cut == io.github.soundgoodizerfan.feedback.process.Cut.FINE)
                    continue;
                ItemStack result = r.result().copy();
                result.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CUT.get(), cut.finer());
                out.add(new Removal(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false,
                        io.github.soundgoodizerfan.feedback.registry.FDataComponents.CUT.get(), cut, accepted[0].getItem()),
                        r.work(), r.hardness(), result, r.operation(), r.swarf(), false));
            }
        }
        return out;
    }

    @Nullable
    private static List<PressCategory.Card> publishedPress;



    /**
     * One card per propellant. The case and bullet slots are every {@code case} and {@code bullet}
     * form the material table has -- the press accepts any, so the card says any.
     */
    private static void publishPress() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedPress != null)
            recipes.hideRecipes(PressCategory.TYPE, publishedPress);
        var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
        List<Item> cases = new ArrayList<>(), bullets = new ArrayList<>();
        materials.forEach((id, m) -> {
            m.item(id, "case").ifPresent(cases::add);
            m.item(id, "paper_case").ifPresent(cases::add);
            for (var shape : io.github.soundgoodizerfan.feedback.weapon.BulletShape.values())
                m.item(id, shape.form()).ifPresent(bullets::add);
        });
        List<PressCategory.Card> cards = new ArrayList<>();
        materials.forEach((id, m) -> m.propellant().ifPresent(p -> {
            List<Item> charge = new ArrayList<>(m.items(id).values());
            if (!charge.isEmpty() && !cases.isEmpty() && !bullets.isEmpty())
                cards.add(new PressCategory.Card(id, charge, cases, bullets, p.peakPressure().value(), p.burnRate()));
        }));
        publishedPress = List.copyOf(cards);
        recipes.addRecipes(PressCategory.TYPE, publishedPress);
    }

    /**
     * One tool card per pattern of each modular type and one part card per shape the Tool Table can
     * make, from the client's copies of both tables; they arrive separately, so either changing republishes.
     */
    private static void publishModular() {
        if (runtime == null)
            return;
        var types = io.github.soundgoodizerfan.feedback.modular.ModularTypes.known();
        List<ToolTableToolCategory.Card> tools = new ArrayList<>(), guns = new ArrayList<>();
        var gunId = io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN.getId();
        types.forEach((id, type) -> {
            for (int i = 0; i < type.baseForms().size(); i++)
                (id.equals(gunId) ? guns : tools).add(new ToolTableToolCategory.Card(id, i));
        });
        show(ToolTableToolCategory.TYPE, tools);
        show(ToolTableToolCategory.GUN_TYPE, guns);
        show(ToolTablePartCategory.TYPE, io.github.soundgoodizerfan.feedback.modular.ToolTableShaping.options(types));
    }

    private static void publishThermal() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedThermal != null)
            recipes.hideRecipes(ThermalProcessCategory.TYPE, publishedThermal);
        List<ThermalProcess> handAuthored = ClientThermalProcesses.get();
        // Reaction rows (fluids, catalyst, pressure) have their own card; the item-only card would print "Air" for them.
        List<ThermalProcess> merged = new ArrayList<>(handAuthored);
        merged.addAll(pooled(handAuthored));
        publishedThermal = List.copyOf(merged);
        recipes.addRecipes(ThermalProcessCategory.TYPE, publishedThermal);
    }

    /**
     * Every vanilla or modded cooking recipe not already claimed by an authored row, as the same one-input rows a vessel runs
     * ({@link VanillaFallback#allRows}), one card per input item with the shortest hold when several recipes take it.
     */
    private static List<ThermalProcess> pooled(List<ThermalProcess> handAuthored) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null)
            return List.of();
        Map<Item, ThermalProcess> best = new LinkedHashMap<>();
        for (ThermalProcess row : VanillaFallback.allRows(client.level)) {
            ItemStack[] accepted = row.inputs().get(0).getItems();
            if (accepted.length == 0 || claimed(row, handAuthored))
                continue;
            best.merge(accepted[0].getItem(), row, (a, b) -> b.requiredTpu() < a.requiredTpu() ? b : a);
        }
        return List.copyOf(best.values());
    }

    /** Whether some authored {@link ThermalProcess} already answers for this row's input. */
    private static boolean claimed(ThermalProcess row, List<ThermalProcess> handAuthored) {
        for (ThermalProcess process : handAuthored)
            for (Ingredient input : process.inputs())
                for (ItemStack stack : row.inputs().get(0).getItems())
                    if (input.test(stack))
                        return true;
        return false;
    }
}
