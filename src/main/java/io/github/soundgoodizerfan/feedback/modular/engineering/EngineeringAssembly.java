// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import io.github.soundgoodizerfan.feedback.modular.ModularFit;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.Treatises;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FEngineering;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * What the Engineering Table's Crafting tab makes of its slots, in the order it looks: a vanilla crafting recipe, then a
 * {@code feedback:engineering} recipe, then a modular assembly read off the grid. Pure over an {@link EngineeringInput} and a level, so the
 * menu, a test and JEI agree; nothing is worn or spent until the {@link Plan#take} of a chosen plan runs, server side.
 *
 * <p>A modular assembly merges grid slots of one material and form into a role's count, positions ignored, and asks the type's data for
 * its fasteners ({@code fasteners}), its tools ({@code tools}) and its school. A fastener shortfall never gates beyond the count; the
 * quality over the floor is the {@link ModularFit.Start}.
 */
public final class EngineeringAssembly {

    /** Nothing in the slots makes anything. */
    public static final int NOTHING = 0;
    /** An output is offered. */
    public static final int OK = 1;
    /** The slots make something the maker lacks the school's volume for. */
    public static final int SCHOOL = 2;
    /** A tool with a needed ability is missing. */
    public static final int NO_TOOL = 3;
    /** A tool with a needed ability is broken. */
    public static final int BROKEN_TOOL = 4;
    /** The hammer is under half the strength the recipe asks. */
    public static final int HAMMER_TOO_WEAK = 5;
    /** Fewer fasteners than the work spends. */
    public static final int NO_FASTENERS = 6;

    /**
     * What a craft does. {@code status} is one of the constants above, {@code result} is empty unless {@link #OK}, and {@code take}
     * (server side, once, when the output is taken) wears the tools, spends the fasteners and consumes the grid. {@code data} is what the
     * hand-work game needs to draw itself ({@link FittingOrder#encode} for fitting, empty for torque).
     */
    public record Plan(int status, ItemStack result, BiConsumer<EngineeringInput, Player> take, String minigame, int rounds, int quality,
                       String data) {

        public static final Plan NONE = new Plan(NOTHING, ItemStack.EMPTY, (i, p) -> {
        });

        /** A plan with no hand-work game. */
        public Plan(int status, ItemStack result, BiConsumer<EngineeringInput, Player> take) {
            this(status, result, take, "", 0, 0, "");
        }

        /** Whether taking the output plays a game first (the game's id is set and there are rounds to play). */
        public boolean playsGame() {
            return ok() && !minigame.isEmpty() && rounds > 0;
        }

        public boolean ok() {
            return status == OK && !result.isEmpty();
        }
    }

    private EngineeringAssembly() {
    }

    /** The types the Crafting tab assembles: all but dishes, which are made at the Prep Table. */
    public static Map<ResourceLocation, ModularType> types(Level level) {
        Map<ResourceLocation, ModularType> out = new TreeMap<>();
        ModularTypes.on(level).forEach((id, t) -> {
            if (t.family() != ModularType.Family.DISH)
                out.put(id, t);
        });
        return out;
    }

    /**
     * The plan for {@code input}.
     *
     * @param blueprint the blueprint slot's stack (empty for none): the yardstick of a modular assembly's fit.
     */
    public static Plan plan(Level level, EngineeringInput input, ItemStack blueprint) {
        return plan(level, input, blueprint, -1f);
    }

    /**
     * As {@link #plan(Level, EngineeringInput, ItemStack)} with the 0..1 score a hand-work game reported ({@code score} negative for none);
     * the score only moves a modular assembly's start condition. A blueprint in the slot skips the game and counts as a start level.
     */
    public static Plan plan(Level level, EngineeringInput input, ItemStack blueprint, float score) {
        return plan(level, input, blueprint, score, false);
    }

    /**
     * As above; {@code floor} is the Auto Engineer's assembly: no game is offered, no score counts and the start condition never rises above
     * the floor quality (fastener levels and the hammer's shortfall may only lower it). {@code Plan#take} may then be run with no player.
     */
    public static Plan plan(Level level, EngineeringInput input, ItemStack blueprint, float score, boolean floor) {
        if (input.gridStacks().isEmpty())
            return Plan.NONE;
        CraftingInput crafting = input.crafting();
        Optional<RecipeHolder<CraftingRecipe>> vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, crafting, level);
        if (vanilla.isPresent() && !(vanilla.get().value() instanceof ModularCraftingRecipe)) {
            ItemStack out = vanilla.get().value().assemble(crafting, level.registryAccess());
            if (!out.isEmpty()) {
                CraftingRecipe recipe = vanilla.get().value();
                return new Plan(OK, out, (in, player) -> consume(in, recipe.getRemainingItems(crafting), player));
            }
        }
        int status = NOTHING;
        for (RecipeHolder<EngineeringRecipe> holder : level.getRecipeManager().getAllRecipesFor(FEngineering.TYPE.get())) {
            EngineeringRecipe recipe = holder.value();
            EngineeringRecipe.Attempt attempt = recipe.attempt(input);
            if (attempt.ok()) {
                String game = recipe.minigame().filter(EngineeringMinigames::known).orElse("");
                int pieces = input.gridStacks().size();
                int rounds = blueprint.isEmpty() && !floor ? EngineeringMinigames.rounds(game, recipe.fasteners().count(), pieces) : 0;
                int quality = Math.max(1, Math.min(5, attempt.fasteners().levels() + recipe.fasteners().floor()));
                String data = "";
                if (rounds > 0 && EngineeringMinigames.FITTING.toString().equals(game))
                    data = recipeOrder(input, recipe.fitOrder());
                // A modular result carries the start condition (fastener levels, hammer shortfall, blueprint, game score); a plain item has no fit to move.
                ItemStack made = started(attempt.result(), new ModularFit.Start(floor ? Math.min(0, attempt.start().levels()) : attempt.start().levels(), rounds > 0 ? score : -1f), !blueprint.isEmpty());
                return new Plan(OK, made, (in, player) -> {
                    EngineeringRecipe.Attempt fresh = recipe.attempt(in);
                    if (fresh.ok())
                        fresh.apply(in);
                    consume(in, recipe.getRemainingItems(in), player);
                }, rounds > 0 ? game : "", rounds, quality, rounds > 0 ? data : "");
            }
            if (status == NOTHING && attempt.refusal() != EngineeringRecipe.Refusal.GRID)
                status = of(attempt.refusal());
        }
        Plan assembled = modular(level, input, blueprint, score, floor);
        if (assembled.status() != NOTHING)
            return assembled;
        return status == NOTHING ? Plan.NONE : new Plan(status, ItemStack.EMPTY, Plan.NONE.take());
    }

    private static int of(EngineeringRecipe.Refusal why) {
        return switch (why) {
            case GRID -> NOTHING;
            case SCHOOL -> SCHOOL;
            case NO_TOOL -> NO_TOOL;
            case BROKEN_TOOL -> BROKEN_TOOL;
            case HAMMER_TOO_WEAK -> HAMMER_TOO_WEAK;
            case NO_FASTENERS -> NO_FASTENERS;
        };
    }

    /** Shrinks every grid stack by one (a recipe's ingredients are one item each) and hands the leftovers (a bucket) to the player (lost when a machine crafts, there being no player). */
    private static void consume(EngineeringInput input, NonNullList<ItemStack> remaining, Player player) {
        for (ItemStack s : input.grid())
            if (!s.isEmpty())
                s.shrink(1);
        for (ItemStack r : remaining)
            if (!r.isEmpty() && player != null)
                player.getInventory().placeItemBackInInventory(r.copy());
    }

    /** The modular assembly the grid makes, with its school, tools and fasteners; see the class comment. */
    private static Plan modular(Level level, EngineeringInput input, ItemStack blueprint, float score, boolean floor) {
        List<ItemStack> grid = input.grid();
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = types(level);
        int mask = input.known() | Treatises.mask(input.binder());
        Optional<ToolTableAssembly.Match> found = ToolTableAssembly.infer(grid, blueprint, materials, types, Treatises.assembles(mask), null);
        if (found.isEmpty())
            return ToolTableAssembly.infer(grid, blueprint, materials, types).isPresent()
                    ? new Plan(SCHOOL, ItemStack.EMPTY, Plan.NONE.take()) : Plan.NONE;
        ResourceLocation id = found.get().type();
        ModularType type = types.get(id);
        Fasteners.Supply supply = Fasteners.supply(input.fasteners(), type.fasteners());
        if (!supply.complete())
            return new Plan(NO_FASTENERS, ItemStack.EMPTY, Plan.NONE.take());
        // The type's own game, else torque for a type that spends fasteners; a blueprint in the slot skips it.
        String game = EngineeringMinigames.forType(type, supply.wanted());
        int pieces = 0;
        for (String r : found.get().role())
            if (r != null)
                pieces++;
        int rounds = blueprint.isEmpty() && !floor ? EngineeringMinigames.rounds(game, supply.wanted(), pieces) : 0;
        int quality = Math.max(1, Math.min(5, supply.levels() + type.fasteners().floor()));
        List<ItemStack> tools = input.tools();
        int[] slot = new int[type.tools().size()];
        boolean[] used = new boolean[tools.size()];
        for (int t = 0; t < slot.length; t++) {
            ItemAbility ability = ToolAbilities.byName(type.tools().get(t));
            int found1 = -1, broken = -1;
            for (int i = 0; i < tools.size() && ability != null; i++) {
                if (used[i] || !ToolAbilities.can(tools.get(i), ability))
                    continue;
                if (!ModularStacks.broken(tools.get(i))) {
                    found1 = i;
                    break;
                }
                if (broken < 0)
                    broken = i;
            }
            if (found1 < 0)
                return new Plan(broken >= 0 ? BROKEN_TOOL : NO_TOOL, ItemStack.EMPTY, Plan.NONE.take());
            used[found1] = true;
            slot[t] = found1;
        }
        // The same parts again, now with what the maker brought, restricted to the type found: the fit and head start are written on the item.
        Optional<ToolTableAssembly.Match> made = ToolTableAssembly.infer(grid, blueprint, materials, Map.of(id, type), t -> true,
                new ModularFit.Start(floor ? Math.min(0, supply.levels()) : supply.levels(), rounds > 0 ? score : -1f));
        if (made.isEmpty())
            return Plan.NONE;
        List<Integer> toWear = new ArrayList<>();
        for (int s : slot)
            toWear.add(s);
        String data = "";
        if (rounds > 0 && EngineeringMinigames.FITTING.toString().equals(game)) {
            String[] letters = made.get().role();
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < letters.length; i++)
                if (letters[i] != null && !grid.get(i).isEmpty() && slots.size() < ModularTuning.FIT_MAX_PIECES)
                    slots.add(i);
            String[] roles = new String[slots.size()];
            int[] at = new int[slots.size()];
            for (int i = 0; i < roles.length; i++) {
                roles[i] = letters[slots.get(i)];
                at[i] = slots.get(i);
            }
            data = FittingOrder.encode(at, FittingOrder.ranks(type, roles));
        }
        return new Plan(OK, made.get().tool(), (in, player) -> {
            for (int s : toWear)
                if (s < in.tools().size())
                    ToolAbilities.wear(in.tools().get(s), 1);
            supply.spend(in.fasteners());
            for (ItemStack s : in.grid())
                s.setCount(0);
        }, rounds > 0 ? game : "", rounds, quality, data);
    }

    /** The fitting order of a recipe's grid: its pieces ranked by the recipe's {@code fit_order} item ids, the rest in reading order. */
    private static String recipeOrder(EngineeringInput input, List<String> order) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < input.grid().size() && slots.size() < ModularTuning.FIT_MAX_PIECES; i++)
            if (!input.grid().get(i).isEmpty())
                slots.add(i);
        String[] ids = new String[slots.size()];
        int[] at = new int[slots.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = BuiltInRegistries.ITEM.getKey(input.grid().get(slots.get(i)).getItem()).toString();
            at[i] = slots.get(i);
        }
        return FittingOrder.encode(at, FittingOrder.ranksByItem(ids, order));
    }

    /**
     * A recipe's result with the start condition applied when it is a modular item (it has a design): the fit and the head start of
     * {@link ModularFit#fit} with no flaw, then rebaked. A plain item is returned untouched; a poor score has no effect on it.
     */
    private static ItemStack started(ItemStack result, ModularFit.Start start, boolean blueprint) {
        if (ModularStacks.design(result) == null)
            return result;
        ModularFit.Fit fit = ModularFit.fit(new ModularFit.Result(true, false, 0f, 0, 0, blueprint, Optional.empty()), start);
        result.set(FModular.FIT.get(), fit);
        ModularFit.start(result, fit);
        ModularStacks.rebake(result);
        return result;
    }
}
