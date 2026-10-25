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
import io.github.soundgoodizerfan.feedback.process.Casting;
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
        ElectronicsCrafting.register(registration);
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
                new CastingCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressCategory(registration.getJeiHelpers().getGuiHelper()),
                new GunMetalCategory(registration.getJeiHelpers().getGuiHelper()),
                new QuenchCategory(registration.getJeiHelpers().getGuiHelper()),
                new AlloyCategory(registration.getJeiHelpers().getGuiHelper()),
                new DipCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolderingCategory(registration.getJeiHelpers().getGuiHelper()),
                new PhaseChangeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ToolTableToolCategory(registration.getJeiHelpers().getGuiHelper()),
                new ToolTablePartCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * Info pages for what happens in the world with no table behind it. Verbs only, never values
     * or solutions (philosophy 8, "documentation is a free sense"): what to do with the thing, not
     * what figure to aim for.
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(FItems.ELECTROLYTIC_CELL.get(), Component.translatable("feedback.jei.info.electrolytic_cell"));
        registration.addIngredientInfo(FModular.HAND_HAMMER.get(), Component.translatable("feedback.jei.info.hand_hammer"));
        registration.addIngredientInfo(FItems.TONGS.get(), Component.translatable("feedback.jei.info.tongs"));
        registration.addIngredientInfo(io.github.soundgoodizerfan.feedback.registry.FModular.GAUNTLET.get(), Component.translatable("feedback.jei.info.gloves"));
        // Basic electronics: machines with no recipe, and the items an operation turns out.
        info(registration, FItems.COHERER.get(), "coherer");
        info(registration, FItems.WHISKER.get(), "whisker");
        info(registration, FItems.RELAY.get(), "relay");
        info(registration, FItems.COMPONENT_TESTER.get(), "component_tester");
        info(registration, FItems.OSCILLOSCOPE.get(), "oscilloscope");
        info(registration, FItems.PHOSPHOR_SCREEN.get(), "phosphor_screen");
        info(registration, FItems.REFLOW_OVEN.get(), "reflow_oven");
        info(registration, FItems.SOLDERING_IRON.get(), "soldering_iron");
        info(registration, FItems.DIAMOND_GRINDING_WHEEL.get(), "diamond_grinding_wheel");
        info(registration, FItems.MASS_PROBE.get(), "mass_probe");
        info(registration, FItems.TRACE_BOARD.get(), "trace_board");
        info(registration, FItems.CIRCUIT.get(), "circuit");
        info(registration, FItems.GALENA.get(), "galena");
        info(registration, FItems.PYRITE.get(), "pyrite");
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
        registration.addRecipeCatalysts(DeformationCategory.TYPE,
                FItems.MECHANICAL_HAMMER.get(), FItems.WIRE_DRAWER.get(),
                FItems.ROLLING_MILL.get(), FItems.MECHANICAL_PRESS.get(), FModular.HAND_HAMMER.get());
        registration.addRecipeCatalysts(RemovalCategory.TYPE,
                FItems.DRILL_PRESS.get(), FItems.LATHE.get(), FItems.GRINDING_WHEEL.get(), FItems.DIAMOND_GRINDING_WHEEL.get());
        registration.addRecipeCatalysts(CastingCategory.TYPE, FItems.INGOT_MOLD.get(), FItems.BULLET_MOLD.get(), FItems.TUBE_MOLD.get());
        registration.addRecipeCatalysts(QuenchCategory.TYPE, net.minecraft.world.item.Items.WATER_BUCKET);
        registration.addRecipeCatalysts(GunMetalCategory.TYPE,
                io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
        registration.addRecipeCatalysts(PressCategory.TYPE,
                io.github.soundgoodizerfan.feedback.registry.FWeapons.RELOADING_PRESS_ITEM.get());
        // All eight: every vessel checks ThermalProcessTable before falling back to
        // VanillaFallback (§4f), so a hand-authored process like steel is genuinely reachable in
        // the Blast Furnace too, not only the crucible -- this was missing before the merge and
        // is a real fix, not just a consequence of it. Which vessel can actually reach a given
        // card's printed floor is a fact the card states in words or figures, not a fact this
        // list gets to decide (§15, no whitelists).
        Item[] vessels = {FItems.SMALL_CRUCIBLE.get(), FItems.LARGE_CRUCIBLE.get(),
                FItems.FURNACE.get(), FItems.SMOKER.get(), FItems.BLAST_FURNACE.get(),
                FItems.IMPROVED_FURNACE.get(), FItems.KILN.get(), FItems.ANNEALING_FURNACE.get()};
        registration.addRecipeCatalysts(ThermalProcessCategory.TYPE, vessels);
        // A dip is drawn from any vessel's tank (MoltenVessel); a mix is kept only by a crucible.
        registration.addRecipeCatalysts(DipCategory.TYPE, vessels);
        registration.addRecipeCatalysts(PhaseChangeCategory.TYPE, FItems.BOILER.get(), FItems.CONDENSER.get());
        registration.addRecipeCatalysts(SolderingCategory.TYPE, FItems.SOLDERING_IRON.get(), FItems.REFLOW_OVEN.get());
        registration.addRecipeCatalysts(AlloyCategory.TYPE, FItems.SMALL_CRUCIBLE.get(), FItems.LARGE_CRUCIBLE.get());
        registration.addRecipeCatalysts(ToolTableToolCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.TOOL_TABLE_ITEM.get(),
                io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
        registration.addRecipeCatalysts(ToolTablePartCategory.TYPE, io.github.soundgoodizerfan.feedback.registry.FModular.TOOL_TABLE_ITEM.get(),
                io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE_ITEM.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        published = null;
        publishedRemoval = null;
        publishedThermal = null;
        // Our vessels are the only things left that can run these; vanilla's own built-in
        // categories now describe blocks that are permanently inert (§4f).
        IRecipeManager recipes = jeiRuntime.getRecipeManager();
        recipes.hideRecipeCategory(RecipeTypes.SMELTING);
        recipes.hideRecipeCategory(RecipeTypes.BLASTING);
        recipes.hideRecipeCategory(RecipeTypes.SMOKING);
        ClientDeformations.onChanged(FeedbackJeiPlugin::publish);
        ClientRemovals.onChanged(FeedbackJeiPlugin::publishRemoval);
        ClientThermalProcesses.onChanged(FeedbackJeiPlugin::publishThermal);
        io.github.soundgoodizerfan.feedback.process.ClientCastings.onChanged(FeedbackJeiPlugin::publishCasting);
        io.github.soundgoodizerfan.feedback.process.ClientQuenches.onChanged(FeedbackJeiPlugin::publishQuench);
        io.github.soundgoodizerfan.feedback.process.ClientMaterials.onChanged(() -> {
            var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
            show(SolderingCategory.TYPE, SolderingCategory.cards(materials));
            show(RecipeTypes.CRAFTING, ElectronicsCrafting.examples(materials));
            publishPress();
            publishModular();
        });
        io.github.soundgoodizerfan.feedback.modular.ModularTypes.onChanged(FeedbackJeiPlugin::publishModular);
        ClientTable.ALLOYS.onChanged(() -> show(AlloyCategory.TYPE, ClientTable.ALLOYS.get()));
        ClientTable.DIPS.onChanged(() -> show(DipCategory.TYPE, ClientTable.DIPS.get()));
        ClientTable.PHASES.onChanged(() -> show(PhaseChangeCategory.TYPE, ClientTable.PHASES.get()));
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
        publishedCasting = null;
        publishedQuench = null;
        publishedPress = null;
        publishedGunMetals = null;
        shown.clear();
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
    private static List<Casting> publishedCasting;
    @Nullable
    private static List<io.github.soundgoodizerfan.feedback.process.Quench> publishedQuench;
    @Nullable
    private static List<PressCategory.Card> publishedPress;
    @Nullable
    private static List<GunMetalCategory.Card> publishedGunMetals;

    private static void publishCasting() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedCasting != null)
            recipes.hideRecipes(CastingCategory.TYPE, publishedCasting);
        publishedCasting = io.github.soundgoodizerfan.feedback.process.ClientCastings.get();
        recipes.addRecipes(CastingCategory.TYPE, publishedCasting);
    }

    private static void publishQuench() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedQuench != null)
            recipes.hideRecipes(QuenchCategory.TYPE, publishedQuench);
        publishedQuench = io.github.soundgoodizerfan.feedback.process.ClientQuenches.get();
        recipes.addRecipes(QuenchCategory.TYPE, publishedQuench);
    }

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
            m.item(id, "bullet").ifPresent(bullets::add);
        });
        List<PressCategory.Card> cards = new ArrayList<>();
        materials.forEach((id, m) -> m.propellant().ifPresent(p -> {
            List<Item> charge = new ArrayList<>(m.items(id).values());
            if (!charge.isEmpty() && !cases.isEmpty() && !bullets.isEmpty())
                cards.add(new PressCategory.Card(id, charge, cases, bullets, p.peakPressure().value(), p.burnRate()));
        }));
        publishedPress = List.copyOf(cards);
        recipes.addRecipes(PressCategory.TYPE, publishedPress);
        publishGunMetals(recipes, materials);
    }

    /** One card per metal a gun barrel can be made of: the reference design baked in it. */
    private static void publishGunMetals(IRecipeManager recipes,
                                         java.util.Map<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
        if (publishedGunMetals != null)
            recipes.hideRecipes(GunMetalCategory.TYPE, publishedGunMetals);
        List<GunMetalCategory.Card> cards = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (!io.github.soundgoodizerfan.feedback.weapon.GunSpec.buildable(m) || m.item(id, "ingot").isEmpty())
                return;
            // The gun type's starter design with every part in this metal: the reference gun the card bakes.
            var types = io.github.soundgoodizerfan.feedback.modular.ModularTypes.known();
            var gun = io.github.soundgoodizerfan.feedback.Feedback.id("gun");
            io.github.soundgoodizerfan.feedback.modular.ToolDesigns.starter(gun, types, materials).ifPresent(d -> {
                var metal = new io.github.soundgoodizerfan.feedback.modular.ModularDesign(d.type(), d.parts().stream()
                        .map(p -> new io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part(p.role(), id, p.form(), p.count(),
                                p.kind(), p.variant(), p.flaw(), p.dims())).toList(), d.edge());
                io.github.soundgoodizerfan.feedback.modular.ModularBake.bake(metal, types.get(gun), materials, 0)
                        .map(io.github.soundgoodizerfan.feedback.weapon.GunFigures::of).ifPresent(f ->
                                cards.add(new GunMetalCategory.Card(id, new ArrayList<>(m.items(id).values()), f)));
            });
        });
        publishedGunMetals = List.copyOf(cards);
        recipes.addRecipes(GunMetalCategory.TYPE, publishedGunMetals);
    }

    /**
     * One tool card per pattern of each modular type and one part card per shape the Tool Table can
     * make, from the client's copies of both tables; they arrive separately, so either changing republishes.
     */
    private static void publishModular() {
        if (runtime == null)
            return;
        var types = io.github.soundgoodizerfan.feedback.modular.ModularTypes.known();
        List<ToolTableToolCategory.Card> tools = new ArrayList<>();
        types.forEach((id, type) -> {
            for (int i = 0; i < type.baseForms().size(); i++)
                tools.add(new ToolTableToolCategory.Card(id, i));
        });
        show(ToolTableToolCategory.TYPE, tools);
        show(ToolTablePartCategory.TYPE, io.github.soundgoodizerfan.feedback.modular.ToolTableShaping.options(types));
    }

    private static void publishThermal() {
        if (runtime == null)
            return;
        IRecipeManager recipes = runtime.getRecipeManager();
        if (publishedThermal != null)
            recipes.hideRecipes(ThermalProcessCategory.TYPE, publishedThermal);
        List<ThermalProcess> handAuthored = ClientThermalProcesses.get();
        List<ThermalProcess> merged = new ArrayList<>(handAuthored);
        merged.addAll(pooled(handAuthored));
        publishedThermal = List.copyOf(merged);
        recipes.addRecipes(ThermalProcessCategory.TYPE, publishedThermal);
    }

    /**
     * Every vanilla/modded cooking recipe not already claimed by a hand-authored entry, wrapped
     * as a {@link ThermalProcess} so it renders through the one card {@link ThermalProcessCategory}
     * already draws.
     *
     * <h3>Sourced from JEI itself, not a second pooling pass</h3>
     * {@code createRecipeLookup} already returns the same deduplicated, cross-mod recipes
     * {@link VanillaFallback#find} searches at runtime, so this needs no pooling mechanism of its
     * own. Filtered against {@code handAuthored} so steel does not also show up as a generic
     * smelting card once something hand-authored already claims its input.
     *
     * <h3>One card per item -- a static grouping, not {@link VanillaFallback#find}'s tie-break</h3>
     * Vanilla ships more than one cooking-recipe entry for plenty of items -- potato has both a
     * {@code smelting} and a {@code smoking} recipe to baked potato, since either appliance can
     * cook food. Streaming all three vanilla types flat and wrapping each holder on its own used
     * to publish both: two cards for one item, and because {@code isFood} reads off the specific
     * recipe instance, the {@code smelting} copy of a food item drew as an ungated metal card next
     * to the correct food one -- {@code "800+ Tu"} beside {@code "0-400 Tu, Spoils > 400 Tu"} for
     * the same potato. Grouped by input item here instead, keeping the shortest cooking time on a
     * tie. This no longer matches {@link VanillaFallback#find} exactly -- that now picks whichever
     * type is most suitable at the vessel's <em>current</em> temperature, per {@code
     * docs/tpu_spec_doc.md}'s "Do Not Let the Shortest Vanilla Recipe Win" -- and that is fine: a JEI
     * card is static and cannot show a dynamic per-temperature answer anyway, so it only needs one
     * honest representative, and shortest-time remains as reasonable a pick as any for that.
     *
     * <h3>What is honest to wrap and what is not</h3>
     * Input, output, and whichever floor gates it ({@link FTuning#METAL_MIN_TU} or
     * {@link FTuning#FOOD_MAX_TU}) are real, universal, and exact -- the same kind of published
     * figure a machine's own spec sheet is (§17). {@code requiredTpu} is not: which of the three
     * thermal profiles actually applies depends on the vessel's live temperature, so there is no
     * single honest figure to put here, and it is left at {@link ThermalProcess#NO_HOLD} --
     * {@link ThermalProcessCategory} knows to leave the row off rather than print a number that
     * was never true.
     */
    private static List<ThermalProcess> pooled(List<ThermalProcess> handAuthored) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || runtime == null)
            return List.of();
        HolderLookup.Provider access = client.level.registryAccess();
        IRecipeManager recipes = runtime.getRecipeManager();
        Map<Item, RecipeHolder<AbstractCookingRecipe>> best = new LinkedHashMap<>();
        Stream.of(
                        lookup(recipes, RecipeTypes.SMELTING),
                        lookup(recipes, RecipeTypes.BLASTING),
                        lookup(recipes, RecipeTypes.SMOKING))
                .flatMap(stream -> stream)
                .forEach(recipe -> {
                    ItemStack[] accepted = recipe.value().getIngredients().get(0).getItems();
                    if (accepted.length == 0)
                        return;
                    Item item = accepted[0].getItem();
                    RecipeHolder<AbstractCookingRecipe> current = best.get(item);
                    if (current == null || recipe.value().getCookingTime() < current.value().getCookingTime())
                        best.put(item, recipe);
                });
        return best.values().stream()
                .filter(recipe -> !claimed(recipe, handAuthored))
                .map(recipe -> toThermalProcess(recipe, access))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <R extends AbstractCookingRecipe> Stream<RecipeHolder<AbstractCookingRecipe>> lookup(
            IRecipeManager recipes, RecipeType<RecipeHolder<R>> type) {
        return recipes.createRecipeLookup(type).get()
                .map(holder -> (RecipeHolder<AbstractCookingRecipe>) (RecipeHolder<?>) holder);
    }

    /** Whether some hand-authored {@link ThermalProcess} already answers for this input. */
    private static boolean claimed(RecipeHolder<AbstractCookingRecipe> recipe, List<ThermalProcess> handAuthored) {
        Ingredient recipeInput = recipe.value().getIngredients().get(0);
        for (ThermalProcess process : handAuthored)
            for (Ingredient input : process.inputs())
                for (ItemStack stack : recipeInput.getItems())
                    if (input.test(stack))
                        return true;
        return false;
    }

    private static ThermalProcess toThermalProcess(RecipeHolder<AbstractCookingRecipe> recipe, HolderLookup.Provider access) {
        ItemStack result = recipe.value().getResultItem(access);
        List<Ingredient> inputs = List.of(recipe.value().getIngredients().get(0));
        if (VanillaFallback.isFood(recipe))
            // No real floor -- food cooks at any heat and is destroyed outright above the
            // ceiling, with nothing named to replace it, so both the band and the destination
            // are printed plainly rather than invented. optimal_temperature is real here (drawn
            // as the card's Optimal row) even though requiredTpu is not.
            return new ThermalProcess(inputs, new Tu(0f), FTuning.SMOKING_OPTIMAL_TU, FTuning.FOOD_MAX_TU, ThermalProcess.NO_HOLD,
                    new TuRate(Float.MAX_VALUE), result, FTuning.FOOD_MAX_TU, ItemStack.EMPTY, FluidStack.EMPTY, false);
        Tu optimal = recipe.value() instanceof BlastingRecipe ? FTuning.BLASTING_OPTIMAL_TU : FTuning.SMELTING_OPTIMAL_TU;
        return new ThermalProcess(inputs, FTuning.METAL_MIN_TU, optimal, new Tu(Float.MAX_VALUE), ThermalProcess.NO_HOLD,
                new TuRate(Float.MAX_VALUE), result, new Tu(Float.MAX_VALUE), ItemStack.EMPTY, FluidStack.EMPTY, false);
    }
}
