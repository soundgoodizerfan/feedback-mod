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
import io.github.soundgoodizerfan.feedback.registry.FItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Punch Cards: blank ones stack (author, 2026-10-11), and the recipe that makes four loads. */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class ControlGameTests {

    @GameTest(template = "empty")
    public static void blankPunchCardsStack(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(FItems.PUNCH_CARD.get()).getMaxStackSize() > 1, "blank cards stack");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Feedback.id("punch_card")).isPresent(),
                "the four-card recipe loads now cards can stack");
        helper.succeed();
    }
}
