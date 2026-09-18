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
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.item.HandToolItem;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A hand tool plus one workpiece, anywhere in the grid. One craft is one application.
 *
 * <h2>Why this is a vanilla RecipeType when nothing else in the mod is</h2>
 * The mod's four process tables (deformation, thermal_process, quench, fuel) are deliberately not
 * {@code RecipeType}s, because philosophy 4 says a machine defines a physical operation and never a
 * recipe -- a Mechanical Hammer that owned a recipe list would know when it was finished, and
 * knowing when to stop is the one thing this mod's machines must never do.
 *
 * <p>None of that applies here. A crafting table <em>is</em> vanilla's recipe machine, and the
 * thing doing the work is the player, who is allowed to know what they are making. So this is an
 * adapter and not a fifth table: every figure still comes from {@link DeformationTable}, the blow
 * itself is {@link Deforming#strike}, and deleting this class would remove a route to a plate
 * without removing a single fact about copper.
 *
 * <h2>What the player sees, and why that is not a leak</h2>
 * The result slot previews the next blow, so a player can watch an ingot go 3, 6, 9, 12 Fu and
 * then become a plate. That is the player gaining information, which philosophy 7 is entirely in
 * favour of. The machine still cannot tell -- put the same ingot on an anvil and the hammer will
 * beat it into foil without noticing.
 *
 * <p>Shift-click is the sharp edge and it is left sharp on purpose. It crafts until the input runs
 * out, which runs an ingot through plate, through foil, into scrap. That is beat 1's lesson --
 * <em>a machine will do this forever, and it does not know when to stop</em> -- delivered by the
 * player's own hand before they have built a machine at all, on copper, which philosophy 6 chose
 * for being something they can afford to waste while learning that waste is possible.
 *
 * <h2>No shape, and no identity</h2>
 * {@code isSpecial()} keeps it out of the recipe book, which is correct twice over: the output
 * depends on components rather than on the input item, so there is nothing fixed to show, and a
 * recipe-book entry saying "copper ingot makes copper plate" would be a lie about a five-blow
 * process. Nothing here names an item either -- the workpiece is whatever the deformation table
 * has an entry for, which is a property lookup and not an identity check (philosophy 1).
 */
public class HandDeformationRecipe extends CustomRecipe {

    /** See {@link #assemble}. Set by {@code matches}, which vanilla always calls first. */
    private static final ThreadLocal<Level> CRAFTING_LEVEL = new ThreadLocal<>();

    public HandDeformationRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * Exactly one hand tool and one workpiece, and the blow has to be one that could land.
     *
     * <p>A swing that accomplishes nothing -- steel too hard, an ingot gone cold, a piece of scrap
     * with nowhere left to go -- simply fails to match, so the grid offers no craft and the player
     * spends nothing. That is the hand equivalent of the Mechanical Hammer wearing: the force had
     * nowhere to go, and the difference is only that a player can feel it before committing.
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        CRAFTING_LEVEL.set(level);
        return find(input, level) != null;
    }

    /**
     * <h3>Why the level arrives by ThreadLocal</h3>
     * A workpiece computes its own temperature on demand from a stamp and a tick, so landing a blow
     * needs a clock -- and {@code assemble} is the one place vanilla does not hand one over.
     * {@code CraftingMenu.slotChangedCraftingGrid} holds the level and simply does not pass it
     * down.
     *
     * <p>NeoForge has the identical problem with the crafting player and solves it the identical
     * way, in {@code CommonHooks.craftingPlayer}. Vanilla always calls {@code matches} immediately
     * before {@code assemble} on the same thread, and the two logical sides are different threads,
     * so a ThreadLocal is exact rather than merely convenient. The fallback is still the honest
     * one: no clock means no blow.
     */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = find(input, CRAFTING_LEVEL.get());
        return result == null ? ItemStack.EMPTY : result;
    }

    // The tool comes back one point more damaged, via HandToolItem#getCraftingRemainingItem.
    // Vanilla's default getRemainingItems walks every slot and asks each stack for its remainder,
    // so there is nothing to override here -- which is the whole reason the durability lives on the
    // item rather than in this class.

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.HAND_DEFORMATION.get();
    }

    /**
     * @param level the clock a workpiece needs to work out how far it has cooled. Null only if
     *              something called {@code assemble} without a {@code matches} before it, in which
     *              case no blow lands -- a hand hammer with no sense of time cannot tell whether
     *              the metal in front of it is still workable.
     * @return the stack the blow would produce, or null if there is no blow to land
     */
    private static ItemStack find(CraftingInput input, Level level) {
        ItemStack tool = ItemStack.EMPTY;
        ItemStack workpiece = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;

            if (stack.getItem() instanceof HandToolItem) {
                if (!tool.isEmpty())
                    return null;    // two tools is not two blows
                tool = stack;
            } else {
                if (!workpiece.isEmpty())
                    return null;    // one workpiece at a time; a tool works one thing
                workpiece = stack;
            }
        }

        if (tool.isEmpty() || workpiece.isEmpty())
            return null;

        // A stack of ingots under one hammer would be one blow spread across sixty-four of them,
        // which is not a thing a hammer can do. Working one at a time is the cost of hand work.
        if (workpiece.getCount() != 1)
            return null;

        if (level == null)
            return null;

        // Any hand tool, not only the hammer. A tool that does not strike declares 0 St and is
        // refused below the material's hardness floor a line later, so there is no list anywhere of
        // which tools deform -- the strength and the declared operation are the whole answer.
        HandToolItem handTool = (HandToolItem) tool.getItem();
        Deforming.Blow blow = Deforming.apply(workpiece, handTool.getStrength(), level, handTool.getOperation());
        return blow.landed() ? blow.result() : null;
    }
}
