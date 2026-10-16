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

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularRecipe;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The load-bearing parts of the modular core ({@code feedback_modular_items_spec.md}): a pattern
 * of parts makes the item, the bake is physics (a heavier head hits harder and swings slower),
 * the hardness gate is arithmetic on the edge, and a worn item is mended by its own stuff.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModularGameTests {

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid, int width, int height) {
        ModularRecipe recipe = new ModularRecipe(CraftingBookCategory.EQUIPMENT);
        CraftingInput input = CraftingInput.of(width, height, grid);
        if (!recipe.matches(input, helper.getLevel()))
            helper.fail("the grid should have made a modular item");
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    /** Three heads over two handles, as vanilla lays a pickaxe out, made of whatever the head item is. */
    static ItemStack pickaxe(GameTestHelper helper, Item head) {
        ItemStack h = new ItemStack(head);
        ItemStack s = new ItemStack(Items.STICK);
        ItemStack e = ItemStack.EMPTY;
        return craft(helper, List.of(h, h, h, e, s, e, e, s, e), 3, 3);
    }

    @GameTest(template = "empty")
    public static void ironPickaxeIsNearVanilla(GameTestHelper helper) {
        ItemStack pick = pickaxe(helper, Items.IRON_INGOT);
        helper.assertTrue(pick.is(FModular.PICKAXE.get()), "three ingots over two sticks should be a pickaxe, got " + pick);
        ModularFigures f = ModularStacks.figures(pick);
        helper.assertTrue(f != null, "the assembled item should carry baked figures");
        helper.assertTrue(Math.abs(f.get(Fig.DIG_SPEED) - 6) < 1.2f, "an iron pickaxe digs near 6, got " + f.get(Fig.DIG_SPEED));
        helper.assertTrue(Math.abs(pick.getMaxDamage() - 250) < 50, "an iron pickaxe lasts near 250, got " + pick.getMaxDamage());
        helper.succeed();
    }

    /** Gold is soft and nearly three times iron's density: a heavy head that swings slow and bites poorly. */
    @GameTest(template = "empty")
    public static void heavierHeadHitsHarderAndSwingsSlower(GameTestHelper helper) {
        ModularFigures iron = ModularStacks.figures(pickaxe(helper, Items.IRON_INGOT));
        ModularFigures gold = ModularStacks.figures(pickaxe(helper, Items.GOLD_INGOT));
        helper.assertTrue(gold.get(Fig.IMPACT) > iron.get(Fig.IMPACT), "a gold head carries more per blow than iron");
        helper.assertTrue(gold.get(Fig.SWING) < iron.get(Fig.SWING), "a gold head swings slower than iron");
        helper.assertTrue(gold.get(Fig.DIG_SPEED) < iron.get(Fig.DIG_SPEED), "soft gold digs slower despite the weight");
        helper.succeed();
    }

    /** Hardness is the gate: copper cannot take iron ore, iron cannot take obsidian, diamond can. */
    @GameTest(template = "empty")
    public static void hardnessGatesWhatDrops(GameTestHelper helper) {
        Tool copper = pickaxe(helper, Items.COPPER_INGOT).get(DataComponents.TOOL);
        Tool iron = pickaxe(helper, Items.IRON_INGOT).get(DataComponents.TOOL);
        Tool diamond = pickaxe(helper, Items.DIAMOND).get(DataComponents.TOOL);
        helper.assertTrue(!copper.isCorrectForDrops(Blocks.IRON_ORE.defaultBlockState()), "copper is too soft for iron ore");
        helper.assertTrue(iron.isCorrectForDrops(Blocks.IRON_ORE.defaultBlockState()), "iron takes iron ore");
        helper.assertTrue(iron.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "iron takes diamond ore");
        helper.assertTrue(!iron.isCorrectForDrops(Blocks.OBSIDIAN.defaultBlockState()), "iron cannot take obsidian");
        helper.assertTrue(diamond.isCorrectForDrops(Blocks.OBSIDIAN.defaultBlockState()), "diamond takes obsidian");
        helper.succeed();
    }

    /** The stuff the head is made of puts durability back; a spent item stops at one point, not zero. */
    @GameTest(template = "empty")
    public static void mendingAndBreaking(GameTestHelper helper) {
        ItemStack pick = pickaxe(helper, Items.IRON_INGOT);
        pick.setDamageValue(pick.getMaxDamage() - 1);
        helper.assertTrue(ModularStacks.broken(pick), "one point from the limit is broken");
        helper.assertTrue(ModularStacks.cap(pick, 5) == 0, "a broken item takes no further damage");
        ItemStack ingot = new ItemStack(Items.IRON_INGOT);
        ItemStack mended = craft(helper, List.of(pick, ingot), 2, 1);
        helper.assertTrue(mended.getDamageValue() < pick.getDamageValue(), "an iron ingot mends an iron head");
        helper.succeed();
    }

    /** Two blades over a grip is a sword; an iron block over two rods is a mace that works its haft loose. */
    @GameTest(template = "empty")
    public static void swordAndMaceAssemble(GameTestHelper helper) {
        ItemStack i = new ItemStack(Items.IRON_INGOT);
        ItemStack s = new ItemStack(Items.STICK);
        ItemStack sword = craft(helper, List.of(i, i, s), 1, 3);
        helper.assertTrue(sword.is(FModular.SWORD.get()), "two blades over a grip are a sword, got " + sword);
        helper.assertTrue(sword.has(DataComponents.TOOL), "a sword cuts cobweb");
        ItemStack block = new ItemStack(Items.IRON_BLOCK);
        ItemStack mace = craft(helper, List.of(block, s, s), 1, 3);
        helper.assertTrue(mace.is(FModular.MACE.get()), "a block over two sticks is a mace, got " + mace);
        helper.assertTrue(ModularStacks.figures(mace).get(Fig.JOINT) > 1, "an iron block on a stick works loose");
        helper.succeed();
    }
}
