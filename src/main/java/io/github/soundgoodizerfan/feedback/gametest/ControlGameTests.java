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
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ComparatorNode;
import io.github.soundgoodizerfan.feedback.control.program.LogicNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Punch Cards: blank ones stack (author, 2026-10-11), and the recipe that makes four loads. And
 * the deadband philosophy 13 said could not be expressed until an actuator's state was readable.
 */
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

    /**
     * On below 100, off above 200, and between them whatever it already was: (T < 100) OR
     * (engaged AND T < 200). The engaged half is the clutch read back as a STATE sensor.
     */
    @GameTest(template = "empty")
    public static void deadbandHoldsBetweenItsMarks(GameTestHelper helper) {
        DataNodeRef heat = new DataNodeRef(BlockPos.ZERO, null);
        DataNodeRef clutch = new DataNodeRef(BlockPos.ZERO.above(), null);
        ProgramGraph graph = new ProgramGraph(List.of(
                new SensorNode(0, heat, Quantity.TEMPERATURE),
                new SensorNode(1, clutch, Quantity.STATE),
                new ComparatorNode(2, ComparatorNode.Compare.LESS, 0, -1, 100f),
                new ComparatorNode(3, ComparatorNode.Compare.LESS, 0, -1, 200f),
                new LogicNode(4, LogicNode.Gate.AND, 1, 3),
                new LogicNode(5, LogicNode.Gate.OR, 2, 4),
                new ActuatorNode(6, 5, clutch)));
        helper.assertTrue(graph.findError(io.github.soundgoodizerfan.feedback.control.program.ControllerLimits.BASE).isEmpty(), "a deadband is a legal graph");
        List<Integer> order = ProgramEvaluator.topologicalOrder(graph).orElseThrow();

        float[][] cases = {{50, 0, 1}, {150, 0, 0}, {150, 1, 1}, {250, 1, 0}};
        for (float[] c : cases) {
            AtomicBoolean out = new AtomicBoolean();
            ProgramEvaluator.evaluate(graph, order, n -> heat.equals(n.target()) ? c[0] : c[1], (ref, channel, on) -> out.set((Boolean) on));
            helper.assertTrue(out.get() == (c[2] == 1),
                    "at " + c[0] + " Tu, engaged " + (c[1] == 1) + ", the clutch should be " + (c[2] == 1));
        }
        helper.succeed();
    }
}
