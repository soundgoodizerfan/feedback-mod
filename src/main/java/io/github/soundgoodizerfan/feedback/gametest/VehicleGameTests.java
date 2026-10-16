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
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlock;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameJoint;
import net.minecraft.world.phys.Vec3;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.BlockPos;
import java.util.List;
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

    /**
     * Drivetrain coupling (spec section 5, steps 2-3): a frame tube welded to a shaft on a lifted
     * vehicle joins that shaft's run. A crank on the vehicle spins the run up to its own speed
     * first -- a rotation run inside a Sable plot works -- then the tube's end is welded to the
     * shaft, and the chain walks out along it to a driving bearing and reports its friction.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void weldedTubeJoinsItsShaft(GameTestHelper helper) {
        BlockPos lift = new BlockPos(1, 4, 0);
        helper.setBlock(lift, FVehicles.LIFT.get());
        helper.setBlock(lift.above().west(), FBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.EAST));
        helper.setBlock(lift.above(), RotationGameTests.alongX(FBlocks.SHAFT.get()));
        helper.setBlock(lift.above().east(), FVehicles.FRAME.get());
        FrameBlockEntity frame = (FrameBlockEntity) helper.getBlockEntity(lift.above().east());
        Frame f = frame.frame();
        int half = Frame.GRID / 2;
        int welded = f.add(new Frame.Vertex(0, half, half)); // the frame block's west face: the shaft's east end
        int free = f.add(new Frame.Vertex(2 * Frame.GRID, half, half));
        f.toggleEdge(welded, free);
        f.setTube(welded, free, ResourceLocation.withDefaultNamespace("iron_block"));
        f.setAnchor(free, BlockPos.ZERO); // held to the body, but turning nothing yet
        f.setPivot(free, new Frame.Pivot(FrameJoint.Kind.HINGE, new Vec3(1, 0, 0), true));

        LiftBlockEntity be = (LiftBlockEntity) helper.getBlockEntity(lift);
        helper.assertTrue(be.lift(helper.getLevel()).equals("lifted"), "the rig should lift");
        var level = helper.getLevel();
        var box = ServerSubLevelContainer.getContainer(level).getSubLevel(be.body()).getPlot().getBoundingBox();
        HandCrankBlockEntity crank = null;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (level.getBlockEntity(p) instanceof HandCrankBlockEntity c)
                crank = c;
        helper.assertTrue(crank != null, "the crank went with the body");
        HandCrankBlockEntity turning = crank;
        BlockPos crankAt = crank.getBlockPos();
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (!done[0])
                turning.turn();
        });
        float crankRpm = FTuning.HAND_CRANK_RPM.value();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(Math.abs(RotationGameTests.rpm(turning) - crankRpm) < 1e-3,
                    "a run in a plot spins up like one in the world, got " + RotationGameTests.rpm(turning));
            FrameBlockEntity live = FrameBlockEntity.loaded(frame.id());
            helper.assertTrue(live.drivetrain().coupledDrag() == 0, "an unwelded tube turns nothing");
            live.frame().setAnchor(welded, new BlockPos(-1, 0, 0));
            live.changed();
            helper.runAfterDelay(20, () -> {
                float drag = live.drivetrain().coupledDrag();
                helper.assertTrue(drag == FTuning.SHAFT_DRAG_SU_PER_RPM.suPerRpm(),
                        "the chain reaches its one driving bearing and charges its friction, got " + drag);
                helper.assertTrue(RotationGameTests.rpm(turning) <= crankRpm + 1e-3, "and the run is no faster for it");
                done[0] = true;
                for (int i = 0; i < 3; i++) // clear the rig: a run left standing can hang shutdown
                    level.removeBlock(crankAt.east(i), false);
                helper.succeed();
            });
        });
    }

    /**
     * Driving (spec section 5, steps 4-5): a tricycle of an engine, a shaft and a frame -- a
     * driven hub on a bent shaft out of the engine's, and two free ones -- is dropped onto a floor
     * and stands on its tires. Switched on, the engine turns the driven tire and its grip pushes the
     * car along its length, not along its axles. Only "moved more than a block" is asserted.
     */
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void engineDrivesACarOnItsTires(GameTestHelper helper) {
        var level = helper.getLevel();
        // Built near the world origin, not in the test's own plot: tests run about 10^7 blocks out,
        // where Sable's poses are rounded to around a block and a car's speed reads as noise.
        BlockPos origin = new BlockPos(8, 100, 8);
        // The test server keeps its world between runs, bodies included: clear any car an
        // earlier run left here, or the new one lands on it.
        var container = ServerSubLevelContainer.getContainer(level);
        for (var old : List.copyOf(container.getAllSubLevels()))
            if (old.logicalPose().position().distanceSquared(origin.getX(), origin.getY(), origin.getZ()) < 64 * 64)
                container.removeSubLevel(old, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
        for (int x = -30; x <= 10; x++)
            for (int z = -6; z <= 10; z++)
                level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState());
        BlockPos lift = origin.offset(2, 4, 2);
        level.setBlockAndUpdate(lift, FVehicles.LIFT.get().defaultBlockState());
        level.setBlockAndUpdate(lift.above(), FVehicles.ENGINE.get().defaultBlockState()
                .setValue(io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlock.FACING, Direction.EAST));
        var design = new io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign(8, 100, 100, Feedback.id("steel"));
        var item = new net.minecraft.world.item.ItemStack(FVehicles.ENGINE_ITEM.get());
        item.set(FVehicles.ENGINE_DESIGN.get(), design);
        level.getBlockEntity(lift.above()).applyComponentsFromItemStack(item);
        level.setBlockAndUpdate(lift.above().east(), RotationGameTests.alongX(FBlocks.SHAFT.get()));
        level.setBlockAndUpdate(lift.above().east(2), FVehicles.FRAME.get().defaultBlockState());

        Frame f = ((FrameBlockEntity) level.getBlockEntity(lift.above().east(2))).frame();
        int g = Frame.GRID, half = g / 2;
        ResourceLocation iron = ResourceLocation.withDefaultNamespace("iron_block");
        ResourceLocation wool = ResourceLocation.withDefaultNamespace("white_wool");
        Vec3 z = new Vec3(0, 0, 1);
        // The driven wheel: out of the shaft along x, then through two locked joints that still
        // carry the turn -- across, then down at a slant -- to a hub on the car's centreline half a
        // block below the base. Its axle runs along z, so the car drives along its own length and
        // the push goes through its middle: a wheel off to one side would yaw it round in circles.
        int w = f.add(new Frame.Vertex(0, half, half));
        int m1 = f.add(new Frame.Vertex(g + half, half, half));
        int m2 = f.add(new Frame.Vertex(g + half, half, -half));
        int d = f.add(new Frame.Vertex(g + half, -half, half));
        f.toggleEdge(w, m1);
        f.setTube(w, m1, iron);
        f.toggleEdge(m1, m2);
        f.setTube(m1, m2, iron);
        f.toggleEdge(m2, d);
        f.setTube(m2, d, iron);
        f.setAnchor(w, new BlockPos(-1, 0, 0));
        f.setPivot(m1, new Frame.Pivot(FrameJoint.Kind.LOCKED, z, true));
        f.setPivot(m2, new Frame.Pivot(FrameJoint.Kind.LOCKED, z, true));
        f.setPivot(d, new Frame.Pivot(FrameJoint.Kind.HINGE, z, true));
        f.setAnchor(d, BlockPos.ZERO);
        helper.assertTrue(f.setTire(d, new Frame.Tire(wool, 0)), "a hinge takes a tire");
        // Two free wheels at the back, either side, welded straight to the frame block.
        for (Frame.Vertex at : new Frame.Vertex[]{
                new Frame.Vertex(-2 * g - half, -half, -g), new Frame.Vertex(-2 * g - half, -half, 2 * g)}) {
            int hub = f.add(at);
            f.setPivot(hub, new Frame.Pivot(FrameJoint.Kind.HINGE, z, false));
            f.setAnchor(hub, BlockPos.ZERO);
            f.setTire(hub, new Frame.Tire(wool, 0));
        }

        LiftBlockEntity be = (LiftBlockEntity) level.getBlockEntity(lift);
        helper.assertTrue(be.lift(level).equals("lifted"), "the car should lift");
        var body = ServerSubLevelContainer.getContainer(level).getSubLevel(be.body());
        level.setBlockAndUpdate(lift, Blocks.AIR.defaultBlockState()); // drops it onto the floor
        double[] start = new double[3];
        helper.runAfterDelay(80, () -> {
            var at = body.logicalPose().position();
            start[0] = at.x();
            start[1] = at.y();
            start[2] = at.z();
            var plot = body.getPlot().getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(plot.minX(), plot.minY(), plot.minZ(), plot.maxX(), plot.maxY(), plot.maxZ()))
                if (level.getBlockEntity(p) instanceof io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity engine)
                    engine.setEngaged(true);
        });
        helper.runAfterDelay(200, () -> {
            var at = body.logicalPose().position();
            double along = Math.abs(at.x() - start[0]), across = Math.abs(at.z() - start[2]);
            helper.assertTrue(along > 1, "the car drives off along its wheels, moved " + along + " [removed " + body.isRemoved()
                    + ", y " + at.y() + "]");
            helper.assertTrue(across < along, "and not along its axles, moved " + across + " that way");
            helper.assertTrue(at.y() > origin.getY() + 2, "standing on its tires, not fallen through, at " + at.y());
            var plot = body.getPlot().getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(plot.minX(), plot.minY(), plot.minZ(), plot.maxX(), plot.maxY(), plot.maxZ()))
                if (level.getBlockEntity(p) instanceof io.github.soundgoodizerfan.feedback.core.rotation.RotationNode)
                    level.removeBlock(p.immutable(), false); // a run left standing can hang shutdown
            container.removeSubLevel(body, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
            helper.succeed();
        });
    }
}
