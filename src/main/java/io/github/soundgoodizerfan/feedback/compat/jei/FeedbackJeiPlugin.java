// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.github.soundgoodizerfan.feedback.Feedback;
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
 * <h2>Every card is added during registration</h2>
 * The server posts {@code OnDatapackSyncEvent}, which sends our tables, before it sends the vanilla
 * recipes (on login and on {@code /reload} alike), and JEI (re)starts on those recipes. So
 * {@code registerRecipes} sees every table. Nothing is added from the runtime: EMI reads JEI's
 * recipes as soon as the runtime exists, so a card added later is missing there.
 *
 * <h2>One tab for both, not two</h2>
 * A first pass gave the pooled vanilla/modded cooking recipes their own category and their own
 * tab. Wrong call: they are the same question {@link ThermalProcessCategory} already answers --
 * what a fire does to a material -- and a second tab just for "we didn't author this one" is a
 * distinction the player has no reason to care about. {@link ThermalProcessTable} entries and
 * pooled ones are both wrapped as a {@link ThermalProcess} and published under one
 * {@link ThermalProcessCategory#TYPE}. See {@link #pooled} for how a pooled recipe is turned into
 * one without inventing figures nobody wrote down: it only ever carries the two universal floors
 * every vessel already enforces, {@link ProcessTuning#METAL_MIN_TU} and {@link ProcessTuning#FOOD_MAX_TU}.
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

    /** The live JEI runtime, or null before JEI has started (for the GUI shot tool, {@code client/GuiShot}). */
    @Nullable
    static IJeiRuntime runtime() {
        return runtime;
    }

    /** The item stack JEI's ingredient list or bookmark overlay has under the mouse, or empty: lets the stand screen preview what hovering it would add. */
    public static net.minecraft.world.item.ItemStack hoveredStack() {
        if (runtime == null)
            return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack stack = runtime.getIngredientListOverlay().getIngredientUnderMouse(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        if (stack == null)
            stack = runtime.getBookmarkOverlay().getIngredientUnderMouse(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        return stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Feedback.id("jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe.class, new CableCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeTipRecipe.class, new ElectrodeTipCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.skillet.SkilletRecipe.class, new SkilletCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotRecipe.class, new StockpotCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.modular.item.BootstrapToolRecipe.class, new BootstrapGloveExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.item.GrappleUpgradeRecipe.class, new GrappleUpgradeCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.machine.bearing.SliderSpringRecipe.class, new SliderSpringCraftingExtension());
        registration.getCraftingCategory().addExtension(
                io.github.soundgoodizerfan.feedback.item.HotToolRecipe.class, new HotToolCraftingExtension());
        ElectronicsCrafting.register(registration);
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.ender.EnderPairRecipe.class, new EnderPairCraftingExtension());
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.tube.CarrierDyeRecipe.class, new CarrierDyeCraftingExtension());
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.ender.DataPairRecipe.class, new EndernetCraftingExtensions.Pair());
        registration.getCraftingCategory().addExtension(io.github.soundgoodizerfan.feedback.machine.ender.EndernetUpgradeRecipe.class, new EndernetCraftingExtensions.Upgrade());
        CodeRecipeExamples.register(registration);
    }

    /**
     * A cut, a board and a circuit are data components, so without this every cut of probe and
     * crystal plate, and every board, would collapse into one JEI entry.
     */
    /** The Engineering Table's tools and fixings popout sits outside the panel; JEI's lists keep off it. */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(io.github.soundgoodizerfan.feedback.client.bench.EngineeringTableScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<io.github.soundgoodizerfan.feedback.client.bench.EngineeringTableScreen>() {
                    @Override
                    public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(io.github.soundgoodizerfan.feedback.client.bench.EngineeringTableScreen screen) {
                        return java.util.List.of(new net.minecraft.client.renderer.Rect2i(screen.getGuiLeft() + io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu.W - 4,
                                screen.getGuiTop() + 14, io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu.POP_W + 4, 170));
                    }
                });
        // a Computer window made game-fullscreen covers the whole game window: JEI's lists keep off all of it, which leaves them no room, so they hide
        registration.addGuiContainerHandler(io.github.soundgoodizerfan.feedback.client.desktop.ComputerScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<io.github.soundgoodizerfan.feedback.client.desktop.ComputerScreen>() {
                    @Override
                    public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(io.github.soundgoodizerfan.feedback.client.desktop.ComputerScreen screen) {
                        if (!screen.windowFullscreen())
                            return java.util.List.of();
                        return java.util.List.of(new net.minecraft.client.renderer.Rect2i(0, 0, screen.width, screen.height));
                    }
                });
    }

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
        registration.registerSubtypeInterpreter(FModular.HAFTED_TOOL.get(), component(FModular.DESIGN.get()));
        registration.registerSubtypeInterpreter(FItems.TRACE_BOARD.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get()));
        registration.registerSubtypeInterpreter(FItems.CIRCUIT.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get()));
        registration.registerSubtypeInterpreter(FModular.VACUUM_TUBE.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TUBE.get()));
        registration.registerSubtypeInterpreter(FModular.RESONATOR.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.ELEMENT.get()));
        registration.registerSubtypeInterpreter(FModular.LATTICE.get(), component(io.github.soundgoodizerfan.feedback.registry.FDataComponents.ELEMENT.get()));
        // A bottle is told apart by its effect alone, never its stamp or potency.
        registration.registerSubtypeInterpreter(FItems.POTION.get(), new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                var state = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
                return state == null ? "" : state.effect().map(Object::toString).orElse("dud");
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return String.valueOf(getSubtypeData(stack, context));
            }
        });
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

    /**
     * JEI lists categories in registration order, so this order is the sort rule. Three groups, in the order a
     * player reaches for them: processes (what a machine or a hand does to matter: heat first, then shaping hand
     * before machine, separation, chemistry, electronics, brewing), assembly (bench work that makes parts and
     * designs), reference (pages that are read, not run: material figures, veins, blueprint pools, trade). A new
     * category goes in the group it belongs to, at the point the same simple-before-advanced rule puts it.
     * Creative tabs have their own order, documented on {@code FCreativeTabs}.
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                // Processes: heat, shaping, separation, chemistry, then electronics and brewing.
                new ThermalProcessCategory(registration.getJeiHelpers().getGuiHelper()),
                new FuelCategory(registration.getJeiHelpers().getGuiHelper()),
                new BasinCategory(registration.getJeiHelpers().getGuiHelper()),
                new DeformationCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressCategory(registration.getJeiHelpers().getGuiHelper()),
                new RemovalCategory(registration.getJeiHelpers().getGuiHelper()),
                new MinedCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreProcessingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SeparationCategory(registration.getJeiHelpers().getGuiHelper()),
                new MixtureCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolderingCategory(registration.getJeiHelpers().getGuiHelper()),
                new CircuitChainCategory(registration.getJeiHelpers().getGuiHelper()),
                new PotionShapeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BrewingIngredientCategory(registration.getJeiHelpers().getGuiHelper()),
                // Assembly: where parts and designs are made.
                new PartBenchPartCategory(registration.getJeiHelpers().getGuiHelper()),
                new PartBenchToolCategory(registration.getJeiHelpers().getGuiHelper()),
                new EngineeringCategory(registration.getJeiHelpers().getGuiHelper()),
                new GunOptionCategory(registration.getJeiHelpers().getGuiHelper()),
                // Reference: what to read, never what to run.
                new MaterialStatsCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreVeinCategory(registration.getJeiHelpers().getGuiHelper()),
                new GunBlueprintPoolCategory(registration.getJeiHelpers().getGuiHelper()),
                new GoodsCategory(registration.getJeiHelpers().getGuiHelper()),
                new BuysCategory(registration.getJeiHelpers().getGuiHelper()),
                new ResearchCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * Info pages for what happens in the world with no table behind it. Verbs only, never values
     * or solutions (philosophy 10, "documentation is a free sense"): what to do with the thing, not
     * what figure to aim for.
     */
    /** Every card made from a synced table. */
    private static void registerTables(IRecipeRegistration registration) {
        var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
        registration.addRecipes(DeformationCategory.TYPE, ClientDeformations.get());
        registration.addRecipes(RemovalCategory.TYPE, withCuts(ClientRemovals.get()));
        registration.addRecipes(ThermalProcessCategory.TYPE, thermalCards());
        registration.addRecipes(SolderingCategory.TYPE, SolderingCategory.cards(materials));
        registration.addRecipes(CircuitChainCategory.TYPE, CircuitChainCategory.cards(materials));
        registration.addRecipes(MaterialStatsCategory.TYPE, MaterialStatsCategory.cards(materials));
        registration.addRecipes(BrewingIngredientCategory.TYPE, BrewingIngredientCategory.cards(materials));
        registration.addRecipes(SeparationCategory.TYPE, SeparationCategory.cards(materials));
        registration.addRecipes(MinedCategory.TYPE, MinedCategory.cards(materials));
        registration.addRecipes(OreProcessingCategory.TYPE, OreProcessingCategory.cards(materials));
        registration.addRecipes(FuelCategory.TYPE, FuelCategory.cards(materials));
        registration.addRecipes(PotionShapeCategory.TYPE, PotionShapeCategory.cards(materials));
        registration.addRecipes(PressCategory.TYPE, pressCards(materials));
        var examples = new ArrayList<>(ElectronicsCrafting.examples(materials));
        examples.addAll(CodeRecipeExamples.examples(materials));
        registration.addRecipes(RecipeTypes.CRAFTING, examples);
        registerModular(registration);
        registration.addRecipes(MixtureCategory.TYPE, ClientTable.MIXTURES.get());
        registration.addRecipes(GoodsCategory.TYPE, ClientTable.GOODS.get());
        registration.addRecipes(GunOptionCategory.TYPE, ClientTable.GUN_OPTIONS.get());
        registration.addRecipes(GunBlueprintPoolCategory.TYPE, ClientTable.GUN_BLUEPRINTS.get());
        registration.addRecipes(BuysCategory.TYPE, ClientTable.BUYS.get());
        registration.addRecipes(ResearchCategory.TYPE, ResearchCategory.cards(ClientTable.RESEARCH.get(),
                io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.get().effects()));
        registration.addRecipes(OreVeinCategory.TYPE, ClientTable.ORE_VEINS.get());
        registration.addRecipes(BasinCategory.TYPE, ClientTable.BASINS.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerTables(registration);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            List<EngineeringCategory.Card> cards = new ArrayList<>();
            for (var holder : level.getRecipeManager().getAllRecipesFor(io.github.soundgoodizerfan.feedback.registry.FEngineering.TYPE.get()))
                cards.add(EngineeringCategory.of(holder.id(), holder.value()));
            registration.addRecipes(EngineeringCategory.TYPE, cards);
        }
        info(registration, io.github.soundgoodizerfan.feedback.registry.FModular.ENGINEERING_TABLE_ITEM.get(), "engineering_table");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FModular.RESEARCH_TABLE_ITEM.get(), "research_table");
        for (var form : io.github.soundgoodizerfan.feedback.registry.FormItems.all()) {
            var made = io.github.soundgoodizerfan.feedback.registry.FormItems.materials().get(form.material());
            if (made != null && made.food().isPresent())
                registration.addIngredientInfo(form.item().get(), Component.translatable("feedback.jei.info.food_form"));
            if (io.github.soundgoodizerfan.feedback.modular.engineering.Fasteners.isFastenerForm(form.form()))
                registration.addIngredientInfo(form.item().get(), Component.translatable("feedback.jei.info.fastener"));
        }
        registration.addIngredientInfo(FItems.HOPPER.get(), Component.translatable("feedback.jei.info.hopper"));
        registration.addIngredientInfo(FModular.PISTON.get(), Component.translatable("feedback.jei.info.piston_block"));
        registration.addIngredientInfo(FModular.NOTE_BLOCK.get(), Component.translatable("feedback.jei.info.note_block"));
        registration.addIngredientInfo(FItems.DROPPER.get(), Component.translatable("feedback.jei.info.dropper"));
        registration.addIngredientInfo(FItems.DISPENSER.get(), Component.translatable("feedback.jei.info.dispenser"));
        registration.addIngredientInfo(FItems.DISPENSER_ACTUATOR.get(), Component.translatable("feedback.jei.info.dispenser_actuator"));
        registration.addIngredientInfo(FItems.MICRO_WEDGE.get(), Component.translatable("feedback.jei.info.micro_wedge"));
        registration.addIngredientInfo(FItems.LARGE_RING_GEAR.get(), Component.translatable("feedback.jei.info.large_ring_gear"));
        registration.addIngredientInfo(FItems.JOINT_LINK.get(), Component.translatable("feedback.jei.info.joint_link"));
        registration.addIngredientInfo(FItems.FREEWHEEL.get(), Component.translatable("feedback.jei.info.freewheel"));
        registration.addIngredientInfo(FItems.DIFFERENTIAL.get(), Component.translatable("feedback.jei.info.differential"));
        registration.addIngredientInfo(FItems.TIRE_RUBBER.get(), Component.translatable("feedback.jei.info.tire_rubber"));
        registration.addIngredientInfo(FItems.BALL_JOINT.get(), Component.translatable("feedback.jei.info.ball_joint"));
        registration.addIngredientInfo(FItems.LOCKED_JOINT.get(), Component.translatable("feedback.jei.info.locked_joint"));
        registration.addIngredientInfo(FItems.SLIDER_JOINT.get(), Component.translatable("feedback.jei.info.slider_joint"));
        registration.addIngredientInfo(FItems.PISTON_JOINT.get(), Component.translatable("feedback.jei.info.piston_joint"));
        registration.addIngredientInfo(FItems.PUDDLING_FURNACE.get(), Component.translatable("feedback.jei.info.puddling_furnace"));
        registration.addIngredientInfo(FItems.IRON_BLOOM.get(), Component.translatable("feedback.jei.info.iron_bloom"));
        registration.addIngredientInfo(FItems.BLOOMERY_SLAG.get(), Component.translatable("feedback.jei.info.bloomery_slag"));
        registration.addIngredientInfo(FItems.CHARGING_GANTRY.get(), Component.translatable("feedback.jei.info.charging_gantry"));
        registration.addIngredientInfo(FItems.CHARGER.get(), Component.translatable("feedback.jei.info.charger"));
        registration.addIngredientInfo(FItems.ENERGY_FITTING.get(), Component.translatable("feedback.jei.info.energy_fitting"));
        registration.addIngredientInfo(FItems.VIS_TAP.get(), Component.translatable("feedback.jei.info.vis_tap"));
        registration.addIngredientInfo(FItems.ROBOT_DOCK.get(), Component.translatable("feedback.jei.info.robot_dock"));
        registration.addIngredientInfo(FItems.ROBOT_BEACON.get(), Component.translatable("feedback.jei.info.robot_beacon"));
        registration.addIngredientInfo(FItems.ROBOT_ARM.get(), Component.translatable("feedback.jei.info.robot_arm"));
        registration.addIngredientInfo(FItems.ROBOT_MAST.get(), Component.translatable("feedback.jei.info.robot_mast"));
        registration.addIngredientInfo(FItems.ROBOT_BOARD.get(), Component.translatable("feedback.jei.info.robot_board"));
        registration.addIngredientInfo(FItems.ROBOT_REEL.get(), Component.translatable("feedback.jei.info.robot_reel"));
        registration.addIngredientInfo(FItems.ROBOT_TETHER.get(), Component.translatable("feedback.jei.info.robot_tether"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.CHESTPLATE.get(), Component.translatable("feedback.jei.info.exosuit"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.TREATISE_BINDER.get(), Component.translatable("feedback.jei.info.treatise_binder"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.TREATISE_SHELF_ITEM.get(), Component.translatable("feedback.jei.info.treatise_shelf"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BLANK_TREATISE.get(), Component.translatable("feedback.jei.info.blank_treatise"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BLANK_RITE.get(), Component.translatable("feedback.jei.info.blank_rite"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.RITE.get(), Component.translatable("feedback.jei.info.rite"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT_BOOK.get(), Component.translatable("feedback.jei.info.blueprint_book"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.SUIT_MODULE.get(), Component.translatable("feedback.jei.info.suit_module"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.ROBOT.get(), Component.translatable("feedback.jei.info.robot"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.LAUNCHER.get(), Component.translatable("feedback.jei.info.launcher"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.ELECTRIC_GUN.get(), Component.translatable("feedback.jei.info.electric_gun"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.CASTER.get(), Component.translatable("feedback.jei.info.caster"));
        registration.addIngredientInfo(new FluidStack(io.github.soundgoodizerfan.feedback.registry.FFluids.POTION.get(), 1000),
                mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                Component.translatable("feedback.jei.info.potion_fluid"));
        registration.addIngredientInfo(FItems.GRAPPLE.get(), Component.translatable("feedback.jei.info.grapple"));
        registration.addIngredientInfo(FItems.GRAPPLE_WINCH.get(), Component.translatable("feedback.jei.info.grapple_winch"));
        registration.addIngredientInfo(FItems.ARC_HEARTH.get(), Component.translatable("feedback.jei.info.arc_hearth"));
        registration.addIngredientInfo(net.minecraft.world.item.Items.HEAVY_CORE, Component.translatable("feedback.jei.info.heavy_core"));
        registration.addIngredientInfo(FItems.INDUCTION_COIL.get(), Component.translatable("feedback.jei.info.induction_coil"));
        registration.addIngredientInfo(FItems.INDUCTION_STOVE.get(), Component.translatable("feedback.jei.info.induction_stove"));
        registration.addIngredientInfo(FItems.PREHEAT_COIL.get(), Component.translatable("feedback.jei.info.preheat_coil"));
        registration.addIngredientInfo(FItems.ARC_HEAD.get(), Component.translatable("feedback.jei.info.arc_head"));
        registration.addIngredientInfo(FItems.STAR_BURNER.get(), Component.translatable("feedback.jei.info.star_burner"));
        registration.addIngredientInfo(FItems.FUSION_CHAMBER.get(), Component.translatable("feedback.jei.info.fusion_chamber"));
        registration.addIngredientInfo(FItems.REACTOR_CORE.get(), Component.translatable("feedback.jei.info.reactor_core"));
        registration.addIngredientInfo(FItems.FUEL_CHANNEL.get(), Component.translatable("feedback.jei.info.fuel_channel"));
        registration.addIngredientInfo(FItems.CONTROL_ASSEMBLY.get(), Component.translatable("feedback.jei.info.control_assembly"));
        registration.addIngredientInfo(FItems.SCRAM_TRIP.get(), Component.translatable("feedback.jei.info.scram_trip"));
        registration.addIngredientInfo(FItems.GRAPHITE_MODERATOR.get(), Component.translatable("feedback.jei.info.graphite_moderator"));
        registration.addIngredientInfo(FModular.RESPIRATOR.get(), Component.translatable("feedback.jei.info.respirator"));
        info(registration, FModular.TELESCOPE.get(), "telescope");
        registration.addIngredientInfo(FItems.IMPACT_PENDULUM.get(), Component.translatable("feedback.jei.info.impact_pendulum"));
        registration.addIngredientInfo(FItems.STRIKE_POST.get(), Component.translatable("feedback.jei.info.strike_post"));
        registration.addIngredientInfo(FItems.SHEATHED_BILLET.get(), Component.translatable("feedback.jei.info.sheathed_billet"));
        registration.addIngredientInfo(FItems.SHEAVE.get(), Component.translatable("feedback.jei.info.sheave"));
        registration.addIngredientInfo(FItems.PITON.get(), Component.translatable("feedback.jei.info.piton"));
        registration.addIngredientInfo(FItems.WINCH.get(), Component.translatable("feedback.jei.info.winch"));
        registration.addIngredientInfo(FItems.SHOCK_CAPSULE.get(), Component.translatable("feedback.jei.info.shock_capsule"));
        registration.addIngredientInfo(FItems.DIAMOND_SEED.get(), Component.translatable("feedback.jei.info.diamond_seed"));
        registration.addIngredientInfo(FItems.FUEL_ROD.get(), Component.translatable("feedback.jei.info.fuel_rod"));
        registration.addIngredientInfo(FItems.THORIUM_FUEL_ROD.get(), Component.translatable("feedback.jei.info.thorium_fuel_rod"));
        registration.addIngredientInfo(FItems.SPENT_FUEL_ROD.get(), Component.translatable("feedback.jei.info.spent_fuel_rod"));
        registration.addIngredientInfo(FItems.SPENT_BLAZE_ROD.get(), Component.translatable("feedback.jei.info.spent_blaze_rod"));
        registration.addIngredientInfo(FItems.INERT_BLAZE_DUST.get(), Component.translatable("feedback.jei.info.inert_blaze_dust"));
        registration.addIngredientInfo(FItems.AIR_VALVE.get(), Component.translatable("feedback.jei.info.air_valve"));
        registration.addIngredientInfo(FItems.RADIATOR_PANEL.get(), Component.translatable("feedback.jei.info.radiator_panel"));
        registration.addIngredientInfo(FItems.CORE_SLAG.get(), Component.translatable("feedback.jei.info.core_slag"));
        registration.addIngredientInfo(FItems.POWER_CUTTER.get(), Component.translatable("feedback.jei.info.power_cutter"));
        registration.addIngredientInfo(FItems.AUTO_ENGINEER.get(), Component.translatable("feedback.jei.info.auto_engineer"));
        registration.addIngredientInfo(FItems.SAW_HEAD.get(), Component.translatable("feedback.jei.info.saw_head"));
        registration.addIngredientInfo(FItems.PLASMA_HEAD.get(), Component.translatable("feedback.jei.info.plasma_head"));
        registration.addIngredientInfo(FItems.WATERJET_HEAD.get(), Component.translatable("feedback.jei.info.waterjet_head"));
        registration.addIngredientInfo(FItems.DATA_STICK.get(), Component.translatable("feedback.jei.info.data_stick"));
        registration.addIngredientInfo(FItems.ARC_ELECTRODE.get(), Component.translatable("feedback.jei.info.arc_electrode"));
        registration.addIngredientInfo(FItems.ELECTROLYTIC_CELL.get(), Component.translatable("feedback.jei.info.electrolytic_cell"));
        registration.addIngredientInfo(FModular.HAFTED_TOOL.get(), Component.translatable("feedback.jei.info.hafted_tool"));
        registration.addIngredientInfo(FItems.SKILLET.get(), Component.translatable("feedback.jei.info.skillet"));
        registration.addIngredientInfo(FItems.REFRIGERATION_UNIT.get(), Component.translatable("feedback.jei.info.refrigeration_unit"));
        registration.addIngredientInfo(FItems.CRYOCOOLER.get(), Component.translatable("feedback.jei.info.cryocooler"));
        registration.addIngredientInfo(net.minecraft.world.item.Items.EMERALD, Component.translatable("feedback.jei.info.commissions"));
        registration.addIngredientInfo(FModular.SPATULA.get(), Component.translatable("feedback.jei.info.spatula"));
        registration.addIngredientInfo(FModular.SPOON.get(), Component.translatable("feedback.jei.info.spoon"));
        registration.addIngredientInfo(FModular.SHEARS.get(), Component.translatable("feedback.jei.info.shears"));
        registration.addIngredientInfo(FItems.TONGS.get(), Component.translatable("feedback.jei.info.tongs"));
        registration.addIngredientInfo(FItems.WRENCH.get(), Component.translatable("feedback.jei.info.hot_tool"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BACKPACK.get(), Component.translatable("feedback.jei.info.backpack"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.BED.get(), Component.translatable("feedback.jei.info.bed"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.GLOVE.get(), Component.translatable("feedback.jei.info.gloves"));
        // Basic electronics: machines with no recipe, and the items an operation turns out.
        info(registration, FItems.COHERER.get(), "coherer");
        info(registration, FItems.DATA_CONNECTOR.get(), "data_connector");
        info(registration, FItems.ROLLING_MILL.get(), "rolling_mill");
        info(registration, FItems.WIRE_DRAWER.get(), "wire_drawer");
        info(registration, FItems.LATHE.get(), "lathe");
        info(registration, FItems.VICE.get(), "vice");
        info(registration, FItems.DRILL_PRESS.get(), "drill_press");
        info(registration, FItems.GEAR_CUTTER.get(), "gear_cutter");
        info(registration, FItems.GRINDING_WHEEL.get(), "grinding_wheel");
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
        info(registration, FModular.THERMAL_STORE.get(), "thermal_store");
        info(registration, FModular.CRUCIBLE.get(), "crucible");
        info(registration, FModular.HEATING_COIL.get(), "heating_coil");
        info(registration, FItems.WHISKER.get(), "whisker");
        info(registration, FItems.RELAY.get(), "relay");
        info(registration, FItems.COMPONENT_TESTER.get(), "component_tester");
        info(registration, FItems.OSCILLOSCOPE.get(), "oscilloscope");
        info(registration, FItems.PHOSPHOR_SCREEN.get(), "phosphor_screen");
        info(registration, FItems.REFLOW_OVEN.get(), "reflow_oven");
        info(registration, FItems.SOLDERING_IRON.get(), "soldering_iron");
        info(registration, FItems.WICK.get(), "wick");
        info(registration, FItems.ARC_WELDER.get(), "arc_welder");
        info(registration, FItems.MASS_PROBE.get(), "mass_probe");
        info(registration, FItems.TRACE_BOARD.get(), "trace_board");
        info(registration, FItems.CIRCUIT.get(), "circuit");
        info(registration, FModular.VACUUM_TUBE.get(), "vacuum_tube");
        info(registration, FModular.RESONATOR.get(), "resonator");
        info(registration, FModular.LATTICE.get(), "lattice");
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
        info(registration, FItems.THRESHER.get(), "thresher");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), "mixer");
        // Stations, blueprints and the magazine (loaded by a code recipe with no fixed grid).
        info(registration, FModular.PREP_TABLE_ITEM.get(), "prep_table");
        info(registration, FModular.SMITHING_TABLE_ITEM.get(), "smithing_table");
        info(registration, FModular.ANVIL_ITEM.get(), "anvil");
        info(registration, FModular.WOODWORKING_TABLE_ITEM.get(), "woodworking_table");
        info(registration, FModular.STONECUTTER_ITEM.get(), "stonecutter");
        info(registration, FModular.CREATIVE_TOOL_TABLE_ITEM.get(), "creative_tool_table");
        info(registration, FModular.BLUEPRINT_TABLE_ITEM.get(), "blueprint_table");
        info(registration, FModular.ENCHANTING_TABLE_ITEM.get(), "enchanting_table");
        info(registration, FModular.BLUEPRINT.get(), "modular_blueprint");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_ITEM.get(), "engine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.PROPELLER_ITEM.get(), "propeller");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.GAS_CELL_CLOTH_ITEM.get(), "gas_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.GAS_CELL_RUBBER_ITEM.get(), "gas_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.GAS_CELL_PLATE_ITEM.get(), "gas_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.GAS_CELL_BLADDER_ITEM.get(), "gas_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.GAS_CELL_MEMBRANE_ITEM.get(), "gas_cell");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.CULTURE_DISH.get().asItem(), "culture_dish");
        info(registration, FItems.SCULK_GROWER.get(), "sculk_grower");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.VENT_VALVE_ITEM.get(), "vent_valve");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.BALLAST_TANK_ITEM.get(), "ballast_tank");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.TRANSMISSION_ITEM.get(), "transmission");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get(), "reloading_press");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE_ITEM.get(), "magazine");
        // Machines and instruments with no catalyst category; each page says what the thing is and how to use it.
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.FIREBOX.get().asItem(), "firebox");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STOCKPOT.get().asItem(), "stockpot");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DRYING_RACK.get().asItem(), "drying_rack");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.HEARTH.get().asItem(), "hearth");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.ICE_BOX.get().asItem(), "ice_box");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.CONTROLLER.get().asItem(), "controller");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.PROGRAMMER.get().asItem(), "programmer");
        info(registration, FItems.THERMOMETER.get(), "thermometer");
        info(registration, FItems.PYROMETER.get(), "pyrometer");
        info(registration, FItems.PRESSURE_GAUGE.get(), "pressure_gauge");
        info(registration, FItems.LIGHT_METER.get(), "light_meter");
        info(registration, FItems.REACH_PROBE.get(), "reach_probe");
        info(registration, FModular.GEIGER_COUNTER.get(), "geiger_counter");
        info(registration, FModular.DOSIMETER.get(), "dosimeter");
        info(registration, FItems.TEMPERATURE_SENSOR.get(), "temperature_sensor");
        info(registration, FItems.STARTER.get(), "starter");
        info(registration, FItems.RADIATOR.get(), "radiator");
        info(registration, FItems.ELECTROSCOPE.get(), "electroscope");
        info(registration, FItems.GALVANOMETER.get(), "galvanometer");
        info(registration, FItems.CURRENT_SENSOR.get(), "current_sensor");
        info(registration, FModular.BALANCE_SCALE.get(), "balance_scale");
        info(registration, FModular.HAMMER_CAM.get(), "hammer_cam");
        info(registration, FItems.COHERER_CLAPPER.get(), "coherer_clapper");
        info(registration, FModular.CAMERA.get(), "camera");
        info(registration, FModular.DRAW_GAUGE.get(), "draw_gauge");
        info(registration, FModular.SURVEY_METER.get(), "survey_meter");
        info(registration, FItems.XP_METER.get(), "xp_meter");
        info(registration, FItems.ELECTRIC_CONTROLLER.get(), "electric_controller");
        info(registration, FItems.ELECTRIC_PROGRAMMER.get(), "electric_programmer");
        info(registration, FItems.COMPUTER.get(), "computer");
        info(registration, FItems.MOTE.get(), "mote");
        io.github.soundgoodizerfan.feedback.registry.FVehicles.TIRE_HUB_ITEMS.forEach(hub -> info(registration, hub.get(), "tire_hub"));
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.MICRO_CORE.get(), "micro_core");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.VOXEL_CASE.get(), "voxel_case");
        info(registration, FItems.MEMORY_BOARD.get(), "memory_board");
        info(registration, FItems.USB_PORT.get(), "usb_port");
        info(registration, FItems.FLOPPY_DISK.get(), "floppy_disk");
        info(registration, FItems.USB_DRIVE.get(), "usb_drive");
        info(registration, FItems.FUNCTION_CARD.get(), "function_card");
        info(registration, FModular.GAS_CENTRIFUGE.get(), "gas_centrifuge");
        info(registration, FItems.ENRICHED_FUEL_ROD.get(), "enriched_fuel_rod");
        info(registration, FItems.PLUTONIUM_FUEL_ROD.get(), "plutonium_fuel_rod");
        info(registration, FItems.BREEDER_ROD.get(), "breeder_rod");
        info(registration, FItems.SPENT_FUEL_POOL.get(), "spent_fuel_pool");
        info(registration, FItems.COOLANT_PORT.get(), "coolant_port");
        info(registration, FItems.COOLANT_CHANNEL.get(), "coolant_channel");
        info(registration, FItems.NEUTRON_REFLECTOR.get(), "neutron_reflector");
        info(registration, FItems.STAR_SEAT.get(), "star_seat");
        info(registration, FItems.FLUX_METER.get(), "flux_meter");
        info(registration, FItems.ORB_ABSORBER.get(), "orb_absorber");
        info(registration, FItems.ORE_TAP.get(), "ore_tap");
        info(registration, FItems.TRADE_TAP.get(), "trade_tap");
        info(registration, FItems.SMELTER_TAP.get(), "smelter_tap");
        info(registration, FItems.DISENCHANT_STONE.get(), "disenchant_stone");
        info(registration, FItems.CRYSTAL_HOLDER.get(), "crystal_holder");
        info(registration, FItems.CRYSTAL_BANK.get(), "crystal_bank");
        info(registration, FModular.BURNER.get(), "burner");
        info(registration, FModular.THRUSTER.get(), "thruster");
        info(registration, FModular.JET.get(), "jet");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BREAKER.get().asItem(), "breaker");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BELLOWS.get().asItem(), "bellows");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BIMETALLIC_STRIP.get().asItem(), "bimetallic_strip");
        info(registration, FModular.COMPRESSOR.get(), "compressor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DAMPER.get().asItem(), "damper");
        info(registration, FModular.DYNAMOTOR.get(), "dynamotor");
        info(registration, FModular.CONVEYOR_BELT.get(), "conveyor_belt");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FLogistics.HEAD_ITEM.get(), "belt_head");
        info(registration, FModular.PALLET_RACK.get(), "pallet_rack");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FStore.RAIL_ITEM.get(), "warehouse_rail");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FStore.HEAD_ITEM.get(), "shuttle_head");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FStore.DOCK_ITEM.get(), "warehouse_dock");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FStore.DESK_ITEM.get(), "catalogue_desk");
        io.github.soundgoodizerfan.feedback.registry.FStore.FITTINGS.forEach((fitting, item) -> info(registration, item.get(), "rack_fitting_" + fitting.id()));
        info(registration, io.github.soundgoodizerfan.feedback.registry.FItems.REDSTONE_EMITTER.get(), "redstone_emitter");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FItems.REDSTONE_RECEIVER.get(), "redstone_receiver");
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
        info(registration, FModular.ELECTROMAGNET.get(), "electromagnet");
        info(registration, FModular.LAMP.get(), "lamp");
        info(registration, FItems.LEVITATION_PLATE.get(), "levitation_plate");
        info(registration, FItems.PYRO_PLATE.get(), "pyro_plate");
        info(registration, FItems.HYDRIDE_BED.get(), "hydride_bed");
        info(registration, FItems.SCRUBBING_BED.get(), "scrubbing_bed");
        info(registration, FItems.RUB_GENERATOR.get(), "rub_generator");
        info(registration, FItems.ESSENTIA_VENT.get(), "essentia_vent");
        info(registration, FItems.ESSENTIA_ADAPTER.get(), "essentia_adapter");
        info(registration, FItems.THERMAL_VALVE.get(), "thermal_valve");
        info(registration, FItems.TAINT_GLAZE.get(), "taint_glaze");
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(io.github.soundgoodizerfan.feedback.Feedback.id("uranium_glass_block"))
                .ifPresent(item -> info(registration, item, "uranium_glass_block"));
        info(registration, FItems.LEVITATION_SLEEVE.get(), "levitation_sleeve");
        info(registration, FItems.COUPLING_SLEEVE.get(), "coupling_sleeve");
        info(registration, FModular.GAS_TANK.get(), "gas_tank");
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
        info(registration, FModular.LINEAR_MOTOR.get(), "linear_motor");
        info(registration, FItems.CARRIER.get(), "carrier");
        info(registration, FItems.MAGNETIC_CARRIER.get(), "magnetic_carrier");
        info(registration, FItems.POD.get(), "pod");
        info(registration, FItems.CREATIVE_AIR.get(), "creative_air");
        info(registration, FItems.ENDER_PORT.get(), "ender_port");
        info(registration, FItems.DATA_PORT.get(), "data_port");
        info(registration, FItems.ENDERNET_UPGRADE.get(), "endernet_upgrade");
        info(registration, FItems.ENDER_ANCHOR.get(), "ender_anchor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.GAS_TURBINE.get().asItem(), "gas_turbine");
        info(registration, FModular.HEAT_EXCHANGER.get(), "heat_exchanger");
        for (net.neoforged.neoforge.registries.DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularMachineHead> head : FModular.MACHINE_HEADS)
            info(registration, head.get(), head.getId().getPath());
        info(registration, FModular.HAND_CRANK.get(), "hand_crank");
        info(registration, FModular.LEYDEN_JAR.get(), "leyden_jar");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.OVERSPEED_TRIP.get().asItem(), "overspeed_trip");
        info(registration, FModular.PRESSURE_VESSEL.get(), "pressure_vessel");
        info(registration, FModular.PUMP.get(), "pump");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STEAM_ENGINE.get().asItem(), "steam_engine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.STEAM_TURBINE.get().asItem(), "steam_turbine");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.TIMER.get().asItem(), "timer");
        info(registration, FModular.WATER_WHEEL.get(), "water_wheel");
        info(registration, FModular.SAWBLADE.get(), "sawblade");
        for (var part : FModular.PARTS) {
            String id = part.getId().getPath();
            if (id.equals("pry_head"))
                info(registration, part.get(), id);
        }
        info(registration, FModular.GALVANIC_CELL.get(), "galvanic_cell");
        info(registration, FModular.THERMOELECTRIC_GENERATOR.get(), "thermoelectric_generator");
        info(registration, FModular.THERMOCOUPLE_PROBE.get(), "thermocouple_probe");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.SCULK_RELAY.get().asItem(), "sculk_relay");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.DATA_SENSOR.get().asItem(), "data_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.RADIATION_SENSOR.get().asItem(), "radiation_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.AURA_SENSOR.get().asItem(), "aura_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.OPTICAL_TERMINAL.get().asItem(), "optical_terminal");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.LASER.get().asItem(), "laser");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.MEMBRANE_SENSOR.get().asItem(), "membrane_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.REBOILER.get().asItem(), "reboiler");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.FLARE.get().asItem(), "flare");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.BLOCK_CAMERA.get().asItem(), "block_camera");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FBlocks.CAMERA_POLE.get().asItem(), "camera_pole");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.FLIGHT_SENSOR_ITEM.get(), "flight_sensor");
        info(registration, io.github.soundgoodizerfan.feedback.registry.FVehicles.BEARING_SENSOR_ITEM.get(), "bearing_sensor");
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
        registration.addRecipeCatalysts(GunOptionCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.ENGINEERING_TABLE_ITEM.get());
        registration.addRecipeCatalysts(ResearchCategory.TYPE, FModular.RESEARCH_TABLE_ITEM.get());
        registration.addRecipeCatalysts(DeformationCategory.TYPE,
                FItems.MECHANICAL_HAMMER.get(), FItems.WIRE_DRAWER.get(),
                FItems.ROLLING_MILL.get(), FItems.MECHANICAL_PRESS.get(), FModular.HAFTED_TOOL.get());
        registration.addRecipeCatalysts(RemovalCategory.TYPE,
                FItems.DRILL_PRESS.get(), FItems.LATHE.get(), FItems.GRINDING_WHEEL.get());
        registration.addRecipeCatalysts(BrewingIngredientCategory.TYPE, FItems.BREWING_STAND.get());
        registration.addRecipeCatalysts(SeparationCategory.TYPE, FModular.ELECTROMAGNET.get(), io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get());
        registration.addRecipeCatalysts(OreProcessingCategory.TYPE, FItems.MECHANICAL_HAMMER.get(), FItems.ROLLING_MILL.get(),
                io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), FModular.ELECTROMAGNET.get(), FItems.REACTOR.get(), FItems.ELECTROLYSER.get());
        registration.addRecipeCatalysts(FuelCategory.TYPE, FItems.FIREBOX.get(), FModular.BURNER.get());
        registration.addRecipeCatalysts(PotionShapeCategory.TYPE, FItems.BREWING_STAND.get());
        registration.addRecipeCatalysts(PressCategory.TYPE,
                io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get());
        // Thermal Process has no category-wide catalysts: each card carries its own (ThermalContainers).
        // A mix is kept only by a crucible.
        registration.addRecipeCatalysts(MixtureCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXER_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get(),
                FModular.CRUCIBLE.get(), FItems.ARC_HEARTH.get());
        registration.addRecipeCatalysts(CircuitChainCategory.TYPE, FItems.CIRCUIT.get());
        registration.addRecipeCatalysts(SolderingCategory.TYPE, FItems.SOLDERING_IRON.get(), FItems.REFLOW_OVEN.get());
        registration.addRecipeCatalysts(EngineeringCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.ENGINEERING_TABLE_ITEM.get());
        registration.addRecipeCatalysts(PartBenchToolCategory.TYPE, net.minecraft.world.item.Items.CRAFTING_TABLE, io.github.soundgoodizerfan.feedback.registry.FModular.ENGINEERING_TABLE_ITEM.get());
        registration.addRecipeCatalysts(PartBenchPartCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.SMITHING_TABLE_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.ANVIL_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.WOODWORKING_TABLE_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.STONECUTTER_ITEM.get(), io.github.soundgoodizerfan.feedback.registry.FModular.CREATIVE_TOOL_TABLE_ITEM.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        // Our vessels are the only things left that can run these; vanilla's own built-in
        // categories now describe blocks that are permanently inert (§4f).
        IRecipeManager recipes = jeiRuntime.getRecipeManager();
        recipes.hideRecipeCategory(RecipeTypes.SMELTING);
        recipes.hideRecipeCategory(RecipeTypes.BLASTING);
        recipes.hideRecipeCategory(RecipeTypes.SMOKING);
        // Vanilla brewing is replaced by the brewing stand's map.
        recipes.hideRecipeCategory(RecipeTypes.BREWING);
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
        // Vanilla enchanted books, every enchantment and level JEI adds: rites replace them.
        jeiRuntime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).stream()
                .filter(s -> s.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)).forEach(hidden::add);
        // Vanilla potions in every form and tipped arrows: our bottles replace them.
        jeiRuntime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).stream()
                .filter(s -> s.is(net.minecraft.world.item.Items.POTION) || s.is(net.minecraft.world.item.Items.SPLASH_POTION)
                        || s.is(net.minecraft.world.item.Items.LINGERING_POTION) || s.is(net.minecraft.world.item.Items.TIPPED_ARROW))
                .forEach(hidden::add);
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hidden);
        var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
        showPartIngredients(materials);
        showTemplateTools(materials);
    }

    /**
     * One wrought iron tool per single-headed face (pickaxe, axe, shovel...), at the face's default haft (its own rod count), added to the
     * ingredient list so the browser shows what a plain hafted tool is; a lookup lands on the Tools card. Parts a face cannot take in wrought
     * iron fall back to the type's default material.
     */
    private static void showTemplateTools(Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
        if (runtime == null)
            return;
        var types = io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known();
        if (types.isEmpty())
            return;
        ResourceLocation iron = ResourceLocation.fromNamespaceAndPath("feedback", "wrought_iron");
        List<ItemStack> tools = new ArrayList<>();
        types.forEach((id, type) -> {
            if (!type.face())
                return;
            io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.specimen(id, iron, types, materials)
                    .map(d -> io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.toPlan(d, type))
                    .flatMap(d -> io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.build(d, types, materials))
                    .ifPresent(tools::add);
        });
        if (tools.isEmpty())
            return;
        runtime.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, tools);
    }

    /**
     * The shaped parts (receiver, barrel, heads...) are in no creative tab -- they mean nothing without
     * a material -- so JEI never lists them. One stack per part, made of iron (else copper, else any material with an ingot),
     * is added once; a lookup on it lands on the Tool Table parts card, which already outputs every part.
     */
    private static void showPartIngredients(Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
        if (runtime == null)
            return;
        var withIngot = materials.entrySet().stream().filter(e -> e.getValue().item(e.getKey(), "ingot").isPresent()).toList();
        // A plain, familiar metal reads as "a part"; map order alone picked leaded solder.
        var stock = java.util.stream.Stream.of("iron", "copper")
                .map(n -> withIngot.stream().filter(e -> e.getKey().getPath().equals(n)).findFirst())
                .flatMap(java.util.Optional::stream).findFirst()
                .or(() -> withIngot.stream().findFirst());
        if (stock.isEmpty())
            return;
        List<ItemStack> parts = FModular.PARTS.stream().map(p -> {
            ItemStack s = new ItemStack(p.get());
            s.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(),
                    new io.github.soundgoodizerfan.feedback.process.MadeOf(stock.get().getKey(), "ingot", 1));
            return s;
        }).toList();
        runtime.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, parts);
        runtime.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, slotParts(stock.get().getKey()));
    }

    /** A composed part once per material of each slot after its first, so every slot material is findable in the ingredient list. */
    private static List<ItemStack> slotParts(net.minecraft.resources.ResourceLocation main) {
        List<ItemStack> out = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (io.github.soundgoodizerfan.feedback.modular.type.ModularType t : io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known().values())
            for (io.github.soundgoodizerfan.feedback.modular.type.ModularType.Role r : t.roles().values()) {
                if (r.part().isEmpty() || r.part().get().choices().isEmpty() || !seen.add(r.part().get().item().toString()))
                    continue;
                io.github.soundgoodizerfan.feedback.modular.type.ModularType.Shaping sh = r.part().get();
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(sh.item());
                for (io.github.soundgoodizerfan.feedback.modular.type.ModularType.PartSlot slot : sh.choices())
                    for (io.github.soundgoodizerfan.feedback.process.Material.Located l : io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns
                            .slotMaterials(slot, sh, io.github.soundgoodizerfan.feedback.process.ClientMaterials.get())) {
                        ItemStack s = new ItemStack(item);
                        s.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(),
                                new io.github.soundgoodizerfan.feedback.process.MadeOf(main, "ingot", (int) Math.ceil(sh.cells()))
                                        .withMeasures(io.github.soundgoodizerfan.feedback.process.Measures.NONE.withSlots(java.util.Map.of(slot.name(),
                                                new io.github.soundgoodizerfan.feedback.process.Measures.Slot(l.id(), l.form())))));
                        out.add(s);
                    }
            }
        return out;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
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

    /**
     * One card per propellant. The case and bullet slots are every {@code case} and {@code bullet}
     * form the material table has -- the press accepts any, so the card says any.
     */
    private static List<PressCategory.Card> pressCards(Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
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
        return cards;
    }

    /** One tool card per pattern of each modular type and one part card per shape the Tool Table can make. */
    private static void registerModular(IRecipeRegistration registration) {
        var types = io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known();
        List<PartBenchToolCategory.Card> tools = new ArrayList<>();
        types.forEach((id, type) -> {
            if (type.face())
                return;
            for (int i = 0; i < type.baseForms().size(); i++)
                tools.add(new PartBenchToolCategory.Card(id, i));
        });
        registration.addRecipes(PartBenchToolCategory.TYPE, tools);
        registration.addRecipes(EngineeringCategory.TYPE, EngineeringCategory.generated(types));
        registration.addRecipes(PartBenchPartCategory.TYPE, io.github.soundgoodizerfan.feedback.modular.bench.PartBenchShaping.options(types));
    }

    /** The authored rows and the pooled cooking recipes, as one list. */
    private static List<ThermalProcess> thermalCards() {
        List<ThermalProcess> handAuthored = ClientThermalProcesses.get();
        List<ThermalProcess> merged = new ArrayList<>(handAuthored);
        merged.addAll(pooled(handAuthored));
        return merged;
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
