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

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The lift, {@code feedback_vehicles_spec.md} section 4: what rests on it becomes one Sable body,
 * held still; raising the lift carries it up; breaking the lift drops it. Heights are compared
 * with half a block of slack because the test server sits far enough out that Sable poses come
 * back rounded (spec section 5, testing caveat).
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class VehicleGameTests {

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void liftHoldsRaisesAndDrops(GameTestHelper helper) {
        BlockPos lift = new BlockPos(0, 4, 0); // clear of the floor
        helper.setBlock(lift, FVehicles.LIFT.get());
        helper.setBlock(lift.above(), Blocks.IRON_BLOCK);
        helper.setBlock(lift.above().east(), Blocks.IRON_BLOCK);
        LiftBlockEntity be = (LiftBlockEntity) helper.getBlockEntity(lift);
        helper.assertTrue(be.lift(helper.getLevel()).equals("lifted"), "two iron blocks on a lift should lift");
        helper.assertTrue(helper.getBlockState(lift).is(FVehicles.LIFT.get()), "the lift stays in the world");
        var body = ServerSubLevelContainer.getContainer(helper.getLevel()).getSubLevel(be.body());
        double y0 = body.logicalPose().position().y();
        helper.runAfterDelay(20, () -> {
            double held = body.logicalPose().position().y();
            helper.assertTrue(Math.abs(held - y0) < 0.5, "held still: y went " + y0 + " to " + held);
            be.step(helper.getLevel(), 2);
            helper.runAfterDelay(20, () -> {
                double raised = body.logicalPose().position().y();
                helper.assertTrue(raised > y0 + 1.5, "raised two: y went " + y0 + " to " + raised);
                helper.destroyBlock(lift);
                helper.runAfterDelay(10, () -> {
                    double dropped = body.logicalPose().position().y();
                    helper.assertTrue(dropped < raised - 0.5, "dropped: y went " + raised + " to " + dropped);
                    helper.succeed();
                });
            });
        });
    }

    /**
     * A Lift item picks a creation up whole and puts it down elsewhere: the body is gone, the lift
     * that held it is bare, and the chest comes back with what was in it.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void liftItemCarriesACreation(GameTestHelper helper) {
        BlockPos lift = new BlockPos(0, 4, 0);
        helper.setBlock(lift, FVehicles.LIFT.get());
        helper.setBlock(lift.above(), Blocks.CHEST);
        helper.setBlock(lift.above().east(), Blocks.IRON_BLOCK);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(lift.above()))
                .setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
        LiftBlockEntity be = (LiftBlockEntity) helper.getBlockEntity(lift);
        helper.assertTrue(be.lift(helper.getLevel()).equals("lifted"), "should lift");
        var container = ServerSubLevelContainer.getContainer(helper.getLevel());
        var body = (dev.ryanhcode.sable.sublevel.ServerSubLevel) container.getSubLevel(be.body());

        var creation = io.github.soundgoodizerfan.feedback.vehicle.LiftItem.capture(helper.getLevel(), body);
        helper.assertTrue(creation != null && creation.getInt("Count") == 2, "two blocks carried, got " + creation);
        helper.assertFalse(be.holding(), "the lift that held it is bare");

        BlockPos second = helper.absolutePos(new BlockPos(4, 4, 0));
        String placed = io.github.soundgoodizerfan.feedback.vehicle.LiftItem.place(helper.getLevel(), second, creation);
        helper.assertTrue(placed.equals("lifted"), "should put down and lift, got " + placed);
        LiftBlockEntity again = (LiftBlockEntity) helper.getLevel().getBlockEntity(second);
        var newBody = container.getSubLevel(again.body());
        var box = newBody.getPlot().getBoundingBox();
        int diamonds = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (helper.getLevel().getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)
                diamonds += chest.getItem(0).getCount();
        helper.assertTrue(diamonds == 7, "the chest keeps its seven diamonds, found " + diamonds);
        helper.succeed();
    }
}
