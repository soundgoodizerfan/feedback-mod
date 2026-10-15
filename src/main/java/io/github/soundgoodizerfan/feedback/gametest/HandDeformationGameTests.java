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
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TODO.md §3c (as of 50b93c6): "nothing is verified by eye" for the Hand Hammer, and its own note that the
 * recipe is pure server logic so a test that feeds a {@code CraftingInput} five times and
 * asserts a plate is cheap. This is that test, run against {@link HandDeformationRecipe}
 * directly rather than through a real crafting menu, since the recipe's own {@code matches}/
 * {@code assemble} pair is the entire rule -- see that class's javadoc.
 *
 * <p>Uses {@code data/feedback/structure/empty.nbt}, a bare 1x1x1 structure with nothing in it.
 * Nothing here needs a placed structure; the framework does.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class HandDeformationGameTests {

    /**
     * A copper ingot's deformation entry costs 14 Fu; the Hand Hammer delivers
     * {@code St / hardness = 3 / 1 = 3} Fu per blow (see {@code Deformation.workFrom}). Five
     * blows land 15 Fu, which is the first blow to clear 14 -- so the fifth craft, and no
     * earlier one, should turn the ingot into a plate.
     */
    @GameTest(template = "empty")
    public static void fiveBlowsMakeAPlate(GameTestHelper helper) {
        Level level = helper.getLevel();
        HandDeformationRecipe recipe = new HandDeformationRecipe(CraftingBookCategory.MISC);

        ItemStack hammer = new ItemStack(FItems.HAND_HAMMER.get());
        ItemStack workpiece = new ItemStack(Items.COPPER_INGOT);

        for (int blow = 1; blow <= 4; blow++) {
            CraftingInput input = CraftingInput.of(2, 1, java.util.List.of(hammer, workpiece));
            if (!recipe.matches(input, level))
                helper.fail("blow " + blow + " should have landed on an ordinary copper ingot");
            workpiece = recipe.assemble(input, level.registryAccess());
            helper.assertTrue(workpiece.is(Items.COPPER_INGOT),
                    "blow " + blow + " should not yet have made a plate, got " + workpiece);
        }

        CraftingInput fifthBlow = CraftingInput.of(2, 1, java.util.List.of(hammer, workpiece));
        if (!recipe.matches(fifthBlow, level))
            helper.fail("the fifth blow should have landed");
        workpiece = recipe.assemble(fifthBlow, level.registryAccess());

        helper.assertTrue(workpiece.is(FItems.COPPER_PLATE.get()),
                "five blows at 3 St (15 Fu) should have produced a copper plate (14 Fu), got " + workpiece);
        helper.succeed();
    }

    /**
     * TODO.md's own §3c guessed "a sixth [blow] makes foil," flagged unverified. The arithmetic
     * says otherwise: the plate costs 9 more Fu on top of the 1 Fu carried over from blow five
     * (see {@link #fiveBlowsMakeAPlate}), and 3 Fu a blow does not clear 9 until the third blow
     * past the plate -- the eighth blow overall. This test is the correction, run rather than
     * guessed a second time.
     */
    @GameTest(template = "empty")
    public static void eighthBlowMakesFoil(GameTestHelper helper) {
        Level level = helper.getLevel();
        HandDeformationRecipe recipe = new HandDeformationRecipe(CraftingBookCategory.MISC);

        ItemStack hammer = new ItemStack(FItems.HAND_HAMMER.get());
        ItemStack workpiece = new ItemStack(Items.COPPER_INGOT);

        for (int blow = 1; blow <= 7; blow++) {
            CraftingInput input = CraftingInput.of(2, 1, java.util.List.of(hammer, workpiece));
            if (!recipe.matches(input, level))
                helper.fail("blow " + blow + " should have landed");
            workpiece = recipe.assemble(input, level.registryAccess());
            helper.assertTrue(!workpiece.is(FItems.COPPER_FOIL.get()),
                    "blow " + blow + " should not yet have made foil, got " + workpiece);
        }

        CraftingInput eighthBlow = CraftingInput.of(2, 1, java.util.List.of(hammer, workpiece));
        if (!recipe.matches(eighthBlow, level))
            helper.fail("the eighth blow should have landed");
        workpiece = recipe.assemble(eighthBlow, level.registryAccess());

        helper.assertTrue(workpiece.is(FItems.COPPER_FOIL.get()),
                "eight blows total should have overshot the plate into foil, got " + workpiece);
        helper.succeed();
    }

    /** Two tools, or two workpieces, is not one blow -- the grid should offer nothing at all. */
    @GameTest(template = "empty")
    public static void twoWorkpiecesDoNotMatch(GameTestHelper helper) {
        Level level = helper.getLevel();
        HandDeformationRecipe recipe = new HandDeformationRecipe(CraftingBookCategory.MISC);

        ItemStack hammer = new ItemStack(FItems.HAND_HAMMER.get());
        ItemStack ingotA = new ItemStack(Items.COPPER_INGOT);
        ItemStack ingotB = new ItemStack(Items.COPPER_INGOT);

        CraftingInput input = CraftingInput.of(3, 1, java.util.List.of(hammer, ingotA, ingotB));
        helper.assertTrue(!recipe.matches(input, level),
                "a hammer with two workpieces should not offer a craft");
        helper.succeed();
    }
}
