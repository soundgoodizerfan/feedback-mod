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
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameAssembly;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameJoint;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame.Vertex;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The welded chassis model ({@code docs/specs/feedback_vehicles_spec.md} section 4): plates form only on a
 * closed triangle, cutting an edge takes its plates, and {@code Vdrag} is the angle-weighted
 * silhouette -- a slab beats a wedge, and a closed box is not counted twice.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class FrameGameTests {

    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("iron_block");
    private static final int B = Frame.GRID; // one block, in grid units

    @GameTest(template = "empty")
    public static void plateNeedsAClosedTriangle(GameTestHelper helper) {
        Frame f = new Frame();
        int a = f.add(new Vertex(0, 0, 0)), b = f.add(new Vertex(B, 0, 0)), c = f.add(new Vertex(0, B, 0));
        f.toggleEdge(a, b);
        f.toggleEdge(b, c);
        helper.assertFalse(f.addPlate(a, b, c, IRON), "two edges are not a plate");
        f.toggleEdge(a, c);
        helper.assertTrue(f.addPlate(a, b, c, IRON), "three edges close a plate");
        helper.assertTrue(Math.abs(f.area(f.plates().iterator().next()) - 0.5) < 1e-9, "half-block triangle");
        f.toggleEdge(a, c);
        helper.assertTrue(f.plates().isEmpty(), "cutting an edge takes the plate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rakedFrontHasLessVdragThanASlab(GameTestHelper helper) {
        float slab = square(new Vertex(0, 0, 0), new Vertex(0, B, 0), new Vertex(0, B, B), new Vertex(0, 0, B))
                .vdrag(new Vec3(1, 0, 0)).raw();
        float raked = square(new Vertex(0, 0, 0), new Vertex(B, B, 0), new Vertex(B, B, B), new Vertex(0, 0, B))
                .vdrag(new Vec3(1, 0, 0)).raw();
        helper.assertTrue(Math.abs(slab - 1f) < 0.02f, "a 1x1 slab facing the air is 1 block², got " + slab);
        helper.assertTrue(Math.abs(raked - (float) Math.sqrt(0.5)) < 0.02f, "a 45° rake is cos45 of the slab, got " + raked);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBoxIsCountedOnce(GameTestHelper helper) {
        Frame f = new Frame();
        int[] v = new int[8];
        for (int i = 0; i < 8; i++)
            v[i] = f.add(new Vertex((i & 1) * B, ((i >> 1) & 1) * B, ((i >> 2) & 1) * B));
        int[][] faces = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] q : faces)
            plateQuad(f, v[q[0]], v[q[1]], v[q[2]], v[q[3]]);
        float drag = f.vdrag(new Vec3(1, 0, 0)).raw();
        helper.assertTrue(Math.abs(drag - 1f) < 0.02f, "a unit box meets the air with one face, got " + drag);
        helper.succeed();
    }

    /** Splitting an edge keeps the surface; moving the new vertex reshapes it; plates follow. */
    @GameTest(template = "empty")
    public static void splitThenMove(GameTestHelper helper) {
        Frame f = new Frame();
        int a = f.add(new Vertex(0, 0, 0)), b = f.add(new Vertex(2 * B, 0, 0)), c = f.add(new Vertex(0, 0, 2 * B));
        f.toggleEdge(a, b);
        f.toggleEdge(b, c);
        f.toggleEdge(a, c);
        f.addPlate(a, b, c, IRON);

        int m = f.split(a, b);
        helper.assertTrue(m >= 0 && f.vertices().get(m).equals(new Vertex(B, 0, 0)), "midpoint of a-b");
        helper.assertTrue(f.plates().size() == 2 && !f.hasEdge(a, b), "the edge and its plate split in two");
        helper.assertTrue(Math.abs(totalArea(f) - 2.0) < 1e-9, "splitting keeps the surface, got " + totalArea(f));

        helper.assertTrue(f.move(m, new Vertex(B, B, 0)), "the new vertex moves on its own");
        helper.assertTrue(totalArea(f) > 2.0, "lifting it out of plane adds area, got " + totalArea(f));
        helper.assertFalse(f.move(m, new Vertex(0, 0, 0)), "cannot move onto another vertex");
        helper.assertTrue(f.split(a, m) >= 0, "a split edge can be split again");
        helper.succeed();
    }

    private static double totalArea(Frame f) {
        double sum = 0;
        for (Frame.Plate p : f.plates())
            sum += f.area(p);
        return sum;
    }

    /**
     * A welded 2x2 floor becomes cells in the frame block's own body -- three of them, the fourth
     * block being the frame block itself -- and nothing is left in the world. The body weighs the
     * frame block plus the plates by iron's density, to within the cells' mass steps.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void weldedFloorBecomesCells(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 4, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        plateQuad(f, f.add(new Vertex(0, h, 0)), f.add(new Vertex(2 * B - 1, h, 0)),
                f.add(new Vertex(2 * B - 1, h, 2 * B - 1)), f.add(new Vertex(0, h, 2 * B - 1)));
        f.setAnchor(f.idAt(new Vertex(0, h, 0)), BlockPos.ZERO);

        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null && result[0] == FrameAssembly.Result.ASSEMBLED, "should assemble, got " + result[0]);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(rel).offset(1, 0, 1)).isAir(),
                "the cells went up with the body");
        helper.runAfterDelay(2, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id()); // assembly moved the block entity
            helper.assertTrue(live.cellCount() == 3 && live.groupBodies().isEmpty(),
                    "three cells, all in the frame's body, got " + live.cellCount() + " and " + live.groupBodies().size() + " group bodies");
            helper.assertTrue(helper.getLevel().getBlockState(live.getBlockPos().offset(1, 0, 1)).is(FVehicles.CHASSIS_CELL.get()),
                    "a cell stands where the plate crosses");
            double frameBlock = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(
                    net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, FVehicles.FRAME.get().defaultBlockState());
            double plates = f.plates().stream().mapToDouble(p -> f.volume(p) * 7.87).sum();
            double cells = body.getMassTracker().getMass() - frameBlock;
            helper.assertTrue(Math.abs(cells - plates) < 0.25 * plates,
                    "the cells weigh the plates by iron's density: " + plates + " kpg, got " + cells);
            helper.succeed();
        });
    }

    /**
     * A plate or tube weighs its material's density times its volume, read from the material
     * system: a full block of iron is 7.87 kpg (7870 Mu). A block no material claims falls back
     * to Sable's own per-block mass.
     */
    @GameTest(template = "empty")
    public static void platesWeighByMaterialDensity(GameTestHelper helper) {
        var iron = net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState();
        helper.assertTrue(Math.abs(FrameAssembly.mass(1.0, iron) - 7.87) < 1e-4,
                "a block of iron is 7.87 kpg, got " + FrameAssembly.mass(1.0, iron));
        helper.assertTrue(Math.abs(FrameAssembly.mass(0.25, iron) - 7.87 / 4) < 1e-4, "mass scales with volume");
        var sponge = net.minecraft.world.level.block.Blocks.SPONGE.defaultBlockState();
        double sable = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(
                net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, sponge);
        helper.assertTrue(Math.abs(FrameAssembly.mass(1.0, sponge) - Math.max(1e-3, sable)) < 1e-9,
                "a block no material claims weighs what Sable says, " + sable + ", got " + FrameAssembly.mass(1.0, sponge));
        helper.succeed();
    }

    /** Editing an assembled frame re-cuts its cells: cut one triangle out and the block only it crossed empties. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void editRecutsCells(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 6, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        int a = f.add(new Vertex(0, h, 0)), b = f.add(new Vertex(2 * B - 1, h, 0)), c = f.add(new Vertex(2 * B - 1, h, 2 * B - 1));
        plateQuad(f, a, b, c, f.add(new Vertex(0, h, 2 * B - 1)));
        f.setAnchor(a, BlockPos.ZERO);
        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        helper.assertTrue(FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result) != null, "should assemble");
        helper.runAfterDelay(2, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id()); // assembly moved the block entity
            helper.assertTrue(live.cellCount() == 3, "three cells to start, got " + live.cellCount());
            live.frame().removePlate(a, b, c);
            live.changed();
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(live.cellCount() == 2, "two after cutting a plate, got " + live.cellCount());
                helper.assertTrue(helper.getLevel().getBlockState(live.getBlockPos().offset(1, 0, 0)).isAir(),
                        "the block only that plate crossed is empty again");
                helper.succeed();
            });
        });
    }

    /**
     * Two floors side by side, joined where their edges meet, become two bodies held by a Sable
     * joint, and the shared vertex stays shared as they fall.
     *
     * <p>The tolerance is loose on purpose. The test server runs each test some ten million blocks
     * out, where Sable's body poses come back rounded to a good fraction of a block -- the gap read
     * here saws up and down by a quarter block with nothing wrong. Precise joint behaviour has to
     * be judged near the origin, in game, not here.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void weldedFramesHoldTogether(GameTestHelper helper) {
        framesJoin(helper, FrameJoint.Kind.WELD);
    }

    /** Same, on a hinged bearing: the bodies may swing, but the pivot stays shared. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hingedFramesHoldTogether(GameTestHelper helper) {
        framesJoin(helper, FrameJoint.Kind.HINGE);
    }

    /** Same, on a ball joint. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void ballJointedFramesHoldTogether(GameTestHelper helper) {
        framesJoin(helper, FrameJoint.Kind.BALL);
    }

    /** Same, on a bearing switched to locked. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void lockedBearingFramesHoldTogether(GameTestHelper helper) {
        framesJoin(helper, FrameJoint.Kind.LOCKED);
    }

    /** Switching a bearing swaps the live constraint: the old one goes, a new one replaces it. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bearingSwitchesPivot(GameTestHelper helper) {
        BlockPos ra = new BlockPos(0, 4, 0), rb = new BlockPos(2, 4, 0);
        helper.setBlock(ra, FVehicles.FRAME.get());
        helper.setBlock(rb, FVehicles.FRAME.get());
        FrameBlockEntity a = (FrameBlockEntity) helper.getBlockEntity(ra), b = (FrameBlockEntity) helper.getBlockEntity(rb);
        int va = a.frame().add(new Vertex(0, 0, 0)), vb = b.frame().add(new Vertex(0, 0, 0));
        a.join(va, b, vb, FrameJoint.Kind.HINGE, new Vec3(0, 1, 0));
        FrameJoint was = a.joint(va, b, vb);
        helper.assertTrue(was != null && b.joint(vb, a, va) == was, "either end should find the joint");
        FrameJoint now = was.withKind(was.kind().nextPivot(), was.axis()).toggleDrive();
        b.replace(a, was, now);
        helper.assertTrue(a.joints().size() == 1 && a.joint(va, b, vb).kind() == FrameJoint.Kind.BALL
                && a.joint(va, b, vb).drives(), "hinge should become a driving ball joint, got " + a.joints());
        helper.succeed();
    }

    private static void framesJoin(GameTestHelper helper, FrameJoint.Kind kind) {
        int h = B / 2;
        BlockPos ra = new BlockPos(0, 4, 0), rb = new BlockPos(2, 4, 0);
        helper.setBlock(ra, FVehicles.FRAME.get());
        helper.setBlock(rb, FVehicles.FRAME.get());
        FrameBlockEntity a = (FrameBlockEntity) helper.getBlockEntity(ra), b = (FrameBlockEntity) helper.getBlockEntity(rb);
        int va = a.frame().add(new Vertex(2 * B - 1, h, B));
        plateQuad(a.frame(), a.frame().add(new Vertex(0, h, 0)), a.frame().add(new Vertex(2 * B - 1, h, 0)),
                a.frame().add(new Vertex(2 * B - 1, h, 2 * B - 1)), a.frame().add(new Vertex(0, h, 2 * B - 1)));
        // B's frame block sits under A's far edge, so its floor starts there and runs on.
        int vb = b.frame().add(new Vertex(0, h, B));
        plateQuad(b.frame(), b.frame().add(new Vertex(0, h, 0)), b.frame().add(new Vertex(2 * B - 1, h, 0)),
                b.frame().add(new Vertex(2 * B - 1, h, 2 * B - 1)), b.frame().add(new Vertex(0, h, 2 * B - 1)));
        // Each floor welded to its own frame block, so it is cells of that frame's body: a loose
        // floor would be a body of its own, free to knock into the other frame.
        a.frame().setAnchor(a.frame().idAt(new Vertex(0, h, 0)), BlockPos.ZERO);
        b.frame().setAnchor(b.frame().idAt(new Vertex(0, h, 0)), BlockPos.ZERO);
        a.join(va, b, vb, kind, new Vec3(0, 1, 0));
        java.util.UUID ia = a.id(), ib = b.id();

        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var bodyA = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(ra), a, result);
        var bodyB = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rb), b, result);
        helper.assertTrue(bodyA != null && bodyB != null, "both frames should assemble");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(FrameBlockEntity.loaded(ia).liveJoints() == 1, kind + " should be a live Sable joint");
            double gap = world(bodyA, ia, va).distanceTo(world(bodyB, ib, vb));
            helper.assertTrue(gap < 0.5, kind + " should keep the vertices together, drifted " + gap + " apart");
            helper.succeed();
        });
    }

    /** Where vertex {@code v} of frame {@code id} is in the world, through its body's pose. */
    private static Vec3 world(dev.ryanhcode.sable.sublevel.ServerSubLevel body, java.util.UUID id, int v) {
        FrameBlockEntity be = FrameBlockEntity.loaded(id);
        return body.logicalPose().transformPosition(be.frame().pos(v).add(Vec3.atLowerCornerOf(be.getBlockPos())));
    }

    /** A tube follows its edge: split it and both halves are tube, cut it and the tube goes, save keeps it. */
    @GameTest(template = "empty")
    public static void tubeFollowsItsEdge(GameTestHelper helper) {
        Frame f = new Frame();
        int a = f.add(new Vertex(0, 0, 0)), b = f.add(new Vertex(2 * B - 1, 0, 0));
        helper.assertTrue(!f.setTube(a, b, IRON), "no edge, no tube");
        f.toggleEdge(a, b);
        helper.assertTrue(f.setTube(a, b, IRON), "an edge can be a tube");
        int m = f.split(a, b);
        helper.assertTrue(f.tube(a, m) != null && IRON.equals(f.tube(m, b).material()), "both halves of a split tube are tube");
        f.thicken(a, m);
        helper.assertTrue(f.tube(a, m).size() == 1 && f.tube(m, b).size() == 0, "thickening one tube leaves the other");
        Frame copy = new Frame();
        copy.load(f.save());
        helper.assertTrue(copy.tubes().size() == 2 && copy.tube(a, m).size() == 1, "tubes and sizes survive a save");
        f.toggleEdge(a, m);
        helper.assertTrue(f.tube(a, m) == null && f.tubes().size() == 1, "cutting the edge takes its tube");
        helper.succeed();
    }

    /**
     * A frame of nothing but a tube is a body: it assembles, and it falls. Where it lands is not
     * asserted: the test server's floor under a test depends on its neighbours' layout, and the
     * same body has both landed and fallen through between runs with no code changed.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tubeOnlyFrameAssemblesAndFalls(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 4, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        int a = be.frame().add(new Vertex(B / 2, B / 2, B / 2)), b = be.frame().add(new Vertex(3 * B, B / 2, B / 2));
        be.frame().toggleEdge(a, b);
        be.frame().setTube(a, b, IRON);
        be.frame().setAnchor(a, BlockPos.ZERO);
        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null, "a tube frame should assemble, got " + result[0]);
        double y0 = body.logicalPose().position().y();
        helper.runAfterDelay(10, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            helper.assertTrue(live.cellCount() > 0 && live.groupBodies().isEmpty(), "the tube is cells on the body, got " + live.cellCount());
            helper.assertTrue(body.logicalPose().position().y() < y0 - 0.1,
                    "it should fall, y went " + y0 + " to " + body.logicalPose().position().y());
            helper.succeed();
        });
    }

    /**
     * A bearing that is the lowest vertex on both sides it joins must still leave two sides. It
     * once gave both its id, which made them one group and asked Sable to join that group to
     * itself: a crash on every load of the world.
     */
    @GameTest(template = "empty")
    public static void bearingOnTheLowestVertexStillCutsTwoSides(GameTestHelper helper) {
        Frame f = new Frame();
        int p = f.add(new Vertex(2 * B, 0, 0)), a = f.add(new Vertex(0, 0, 0)), c = f.add(new Vertex(4 * B, 0, 0));
        f.toggleEdge(a, p);
        f.toggleEdge(p, c);
        f.setTube(a, p, IRON);
        f.setTube(p, c, IRON);
        f.setPivot(p, new Frame.Pivot(FrameJoint.Kind.BALL, new Vec3(0, 1, 0), false));
        var parts = FrameAssembly.parts(f);
        helper.assertTrue(parts.size() == 2 && parts.get(0).id() != parts.get(1).id(),
                "two sides with two ids, got " + parts.stream().map(FrameAssembly.Part::id).toList());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void pivotInsideAFrameHangsOneGroupOffTheOther(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 4, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int a = f.add(new Vertex(B / 2, B / 2, B / 2)), p = f.add(new Vertex(2 * B, B / 2, B / 2)),
                c = f.add(new Vertex(4 * B, B / 2, B / 2));
        f.toggleEdge(a, p);
        f.toggleEdge(p, c);
        f.setTube(a, p, IRON);
        f.setTube(p, c, IRON);
        f.setPivot(p, new Frame.Pivot(FrameJoint.Kind.BALL, new Vec3(0, 1, 0), false));
        f.setAnchor(a, BlockPos.ZERO);

        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null, "should assemble, got " + result[0]);
        helper.runAfterDelay(20, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            helper.assertTrue(live.groupBodies().size() == 1, "the far tube is a body of its own, got " + live.groupBodies().size());
            helper.assertTrue(live.liveJoints() == 1, "the bearing should be one live joint, got " + live.liveJoints());
            helper.assertTrue(live.joints().isEmpty() && live.frame().pivots().size() == 1,
                    "it stays one frame: no second block, no cross-frame joint");
            helper.succeed();
        });
    }

    /**
     * Only a welded point holds a frame to its vehicle (spec section 4): on a Lift, a floor welded
     * to the frame block is cells of the held body and stays put, and an unwelded one is a body
     * of its own and falls away.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void weldedFloorStaysOnALiftedVehicle(GameTestHelper helper) {
        floorOnALift(helper, true);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void unweldedFloorFallsOff(GameTestHelper helper) {
        floorOnALift(helper, false);
    }

    private static void floorOnALift(GameTestHelper helper, boolean welded) {
        BlockPos lift = new BlockPos(0, 4, 0), rel = lift.above();
        helper.setBlock(lift, FVehicles.LIFT.get());
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        // Out beside the lift, so a loose floor has air under it rather than the lift to land on.
        int a = f.add(new Vertex(B, h, 0));
        plateQuad(f, a, f.add(new Vertex(3 * B - 1, h, 0)), f.add(new Vertex(3 * B - 1, h, 2 * B - 1)),
                f.add(new Vertex(B, h, 2 * B - 1)));
        if (welded)
            f.setAnchor(a, BlockPos.ZERO);
        var liftBe = (io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity) helper.getBlockEntity(lift);
        Vec3 start = Vec3.atLowerCornerOf(helper.absolutePos(rel)).add(f.pos(a));
        helper.assertTrue(liftBe.lift(helper.getLevel()).equals("lifted"), "the frame block should lift");
        helper.runAfterDelay(30, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            helper.assertTrue(live.groupBodies().size() == (welded ? 0 : 1), "a loose floor is a body of its own");
            double drop = start.y - live.worldPos(helper.getLevel(), a).y;
            if (welded)
                helper.assertTrue(Math.abs(drop) < 0.5, "a welded floor stays with the held body, moved " + drop);
            else
                helper.assertTrue(drop > 0.75, "an unwelded floor falls off, fell only " + drop);
            helper.succeed();
        });
    }

    /**
     * Bearings swing (the 2026-10-15 playtest's top complaint): a tube welded to a held body
     * carries a second tube on a bearing, and the second hangs down under gravity -- on a ball
     * bearing, and on a hinge whose axis lies across the tubes. Built near the world origin, as
     * the driving test is, so the poses read are not noise.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void ballBearingSwings(GameTestHelper helper) {
        swings(helper, FrameJoint.Kind.BALL, -20);
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void hingeSwings(GameTestHelper helper) {
        swings(helper, FrameJoint.Kind.HINGE, 20);
    }

    private static void swings(GameTestHelper helper, FrameJoint.Kind kind, int dz) {
        var level = helper.getLevel();
        BlockPos lift = new BlockPos(8, 180, 8 + dz);
        clearNear(level, lift);
        level.setBlockAndUpdate(lift, FVehicles.LIFT.get().defaultBlockState());
        level.setBlockAndUpdate(lift.above(), FVehicles.FRAME.get().defaultBlockState());
        FrameBlockEntity be = (FrameBlockEntity) level.getBlockEntity(lift.above());
        Frame f = be.frame();
        int h = B / 2;
        int w = f.add(new Vertex(h, h, h)), p = f.add(new Vertex(2 * B + h, h, h)), e = f.add(new Vertex(4 * B + h, h, h));
        f.toggleEdge(w, p);
        f.toggleEdge(p, e);
        f.setTube(w, p, IRON);
        f.setTube(p, e, IRON);
        f.setAnchor(w, BlockPos.ZERO);
        f.setPivot(p, new Frame.Pivot(kind, new Vec3(0, 0, 1), false));
        var liftBe = (io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity) level.getBlockEntity(lift);
        double y0 = Vec3.atLowerCornerOf(lift.above()).add(f.pos(e)).y;
        helper.assertTrue(liftBe.lift(level).equals("lifted"), "the rig should lift");
        // A pendulum with nothing to damp it may be back up on the far side at any given tick, so
        // the deepest point it reached is what is read.
        double[] deepest = {0};
        helper.onEachTick(() -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            if (live != null && !live.groupBodies().isEmpty())
                deepest[0] = Math.max(deepest[0], y0 - live.worldPos(level, e).y);
        });
        helper.runAfterDelay(60, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            helper.assertTrue(live.groupBodies().size() == 1 && live.liveJoints() == 1,
                    kind + ": the hanging tube is its own body on one joint, got " + live.groupBodies().size() + " and " + live.liveJoints());
            Vec3 pivot = live.worldPos(level, p), end = live.worldPos(level, e);
            helper.assertTrue(deepest[0] > 0.75, kind + " bearing should let the tube swing down, its end fell only " + deepest[0]);
            helper.assertTrue(Math.abs(pivot.distanceTo(end) - 2) < 0.5,
                    kind + ": the tube stays its length from the bearing, got " + pivot.distanceTo(end));
            tearDown(level, liftBe);
            helper.succeed();
        });
    }

    /**
     * The Lift lowered to the bottom and used again puts the vehicle back as blocks: the frame
     * block stands in the world again, not assembled, and no cell is left anywhere.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void liftLowersAVehicleBackToBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos lift = new BlockPos(28, 180, 8);
        clearNear(level, lift);
        level.setBlockAndUpdate(lift, FVehicles.LIFT.get().defaultBlockState());
        level.setBlockAndUpdate(lift.above(), FVehicles.FRAME.get().defaultBlockState());
        FrameBlockEntity be = (FrameBlockEntity) level.getBlockEntity(lift.above());
        Frame f = be.frame();
        int h = B / 2;
        int a = f.add(new Vertex(0, h, 0)), p = f.add(new Vertex(2 * B + h, h, h)), e = f.add(new Vertex(4 * B + h, h, h));
        plateQuad(f, a, f.add(new Vertex(2 * B - 1, h, 0)), f.add(new Vertex(2 * B - 1, h, 2 * B - 1)), f.add(new Vertex(0, h, 2 * B - 1)));
        f.setAnchor(a, BlockPos.ZERO);
        f.toggleEdge(f.idAt(new Vertex(2 * B - 1, h, 0)), p);
        f.setTube(f.idAt(new Vertex(2 * B - 1, h, 0)), p, IRON);
        f.toggleEdge(p, e);
        f.setTube(p, e, IRON);
        f.setPivot(p, new Frame.Pivot(FrameJoint.Kind.HINGE, new Vec3(0, 0, 1), false));
        var liftBe = (io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity) level.getBlockEntity(lift);
        helper.assertTrue(liftBe.lift(level).equals("lifted"), "the rig should lift");
        helper.runAfterDelay(20, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            var held = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level).getSubLevel(liftBe.body());
            helper.assertTrue(live != null, "the frame should be loaded in its body [body " + liftBe.body() + " removed "
                    + (held == null ? "gone" : held.isRemoved() + " at " + held.logicalPose().position()) + "]");
            java.util.UUID group = live.groupBodies().values().iterator().next().body();
            helper.assertTrue(liftBe.lower(level).equals("lowered"), "a held vehicle at the bottom goes back to blocks");
            helper.assertTrue(level.getBlockEntity(lift.above()) instanceof FrameBlockEntity back && !back.assembled()
                    && back.cellCount() == 0 && back.frame().plates().size() == 2,
                    "the frame stands where it was built, a frame again");
            var container = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level);
            helper.assertTrue(container.getSubLevel(group) == null || container.getSubLevel(group).isRemoved(),
                    "the hanging group's body is gone");
            for (BlockPos q : BlockPos.betweenClosed(lift.offset(-2, 0, -2), lift.offset(6, 3, 3)))
                helper.assertFalse(level.getBlockState(q).is(FVehicles.CHASSIS_CELL.get()), "no cell is left in the world, found one at " + q);
            liftRig(level, lift);
            helper.succeed();
        });
    }

    /** The test world persists: take away bodies and blocks an earlier run left near {@code at}. */
    private static void clearNear(net.minecraft.server.level.ServerLevel level, BlockPos at) {
        var container = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level);
        for (var old : java.util.List.copyOf(container.getAllSubLevels()))
            if (old.logicalPose().position().distanceSquared(at.getX(), at.getY(), at.getZ()) < 12 * 12)
                container.removeSubLevel(old, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
        liftRig(level, at);
    }

    private static void liftRig(net.minecraft.server.level.ServerLevel level, BlockPos at) {
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-3, -1, -3), at.offset(7, 4, 4)))
            if (!level.getBlockState(q).isAir())
                level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    private static void tearDown(net.minecraft.server.level.ServerLevel level, io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity lift) {
        var container = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level);
        var body = lift.body() == null ? null : container.getSubLevel(lift.body());
        BlockPos at = lift.getBlockPos();
        clearNear(level, at);
        if (body != null && !body.isRemoved())
            container.removeSubLevel(body, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
    }

    /** A bearing on a drive shaft hinges along the shaft, whichever way it points, from either end. */
    @GameTest(template = "empty")
    public static void bearingOnAShaftTurnsAboutIt(GameTestHelper helper) {
        BlockPos ra = new BlockPos(0, 4, 0), rb = new BlockPos(2, 4, 0);
        helper.setBlock(ra, FVehicles.FRAME.get());
        helper.setBlock(rb, FVehicles.FRAME.get());
        FrameBlockEntity chassis = (FrameBlockEntity) helper.getBlockEntity(ra), shaft = (FrameBlockEntity) helper.getBlockEntity(rb);
        int mount = chassis.frame().add(new Vertex(0, 0, 0));
        int end = shaft.frame().add(new Vertex(0, 0, 0)), far = shaft.frame().add(new Vertex(2 * B, B, 0));
        shaft.frame().toggleEdge(end, far);
        shaft.frame().setTube(end, far, IRON);
        Vec3 along = new Vec3(2, 1, 0).normalize();
        Vec3 fromChassis = io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem.hingeAxis(chassis, mount, shaft, end, new Vec3(0, 0, 1));
        Vec3 fromShaft = io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem.hingeAxis(shaft, end, chassis, mount, new Vec3(0, 0, 1));
        helper.assertTrue(Math.abs(Math.abs(fromChassis.dot(along)) - 1) < 1e-6, "chassis side should hinge along the shaft, got " + fromChassis);
        helper.assertTrue(Math.abs(Math.abs(fromShaft.dot(along)) - 1) < 1e-6, "shaft side should hinge along the shaft, got " + fromShaft);
        helper.succeed();
    }

    private static Frame square(Vertex a, Vertex b, Vertex c, Vertex d) {
        Frame f = new Frame();
        plateQuad(f, f.add(a), f.add(b), f.add(c), f.add(d));
        return f;
    }

    /** Two triangles sharing the a-c diagonal. */
    private static void plateQuad(Frame f, int a, int b, int c, int d) {
        for (int[] e : new int[][]{{a, b}, {b, c}, {c, d}, {d, a}, {a, c}})
            if (!f.hasEdge(e[0], e[1]))
                f.toggleEdge(e[0], e[1]);
        f.addPlate(a, b, c, IRON);
        f.addPlate(a, c, d, IRON);
    }
}
