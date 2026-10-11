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
 * The welded chassis model ({@code feedback_vehicles_spec.md} section 4): plates form only on a
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
     * A welded 2x2 floor lifts only its frame block into a body, and its two triangles become two
     * boxes welded to it. Nothing is placed in the world.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void weldedFloorBecomesBoxes(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 4, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        plateQuad(f, f.add(new Vertex(0, h, 0)), f.add(new Vertex(2 * B - 1, h, 0)),
                f.add(new Vertex(2 * B - 1, h, 2 * B - 1)), f.add(new Vertex(0, h, 2 * B - 1)));

        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null && result[0] == FrameAssembly.Result.ASSEMBLED, "should assemble, got " + result[0]);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(rel).offset(1, 0, 1)).isAir(),
                "no block is placed for a plate");
        helper.runAfterDelay(5, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id()); // assembly moved the block entity
            helper.assertTrue(live.boxCount() == 2, "two triangles are two boxes, got " + live.boxCount());
            helper.assertTrue(live.boxesSettled(), "both boxes should be in Sable and welded");
            helper.assertTrue(live.boxMass() > 0, "the boxes carry the plates' mass");
            helper.succeed();
        });
    }

    /**
     * A box stays where its plate is as the body falls. Loose for the same reason as the joint
     * tests: the test server is far from the origin, where Sable rounds poses.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void boxesFallWithTheirBody(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 6, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        plateQuad(f, f.add(new Vertex(0, h, 0)), f.add(new Vertex(2 * B - 1, h, 0)),
                f.add(new Vertex(2 * B - 1, h, 2 * B - 1)), f.add(new Vertex(0, h, 2 * B - 1)));
        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null, "should assemble, got " + result[0]);
        helper.runAfterDelay(20, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            Vec3 at = body.logicalPose().transformPosition(Vec3.atLowerCornerOf(live.getBlockPos()).add(1, h / (double) B, 1));
            double nearest = live.boxPositions().stream().mapToDouble(p -> p.distanceTo(at)).min().orElse(Double.MAX_VALUE);
            helper.assertTrue(nearest < 1.5, "a box should still be at its plate, nearest was " + nearest + " away");
            helper.succeed();
        });
    }

    /** Editing an assembled frame changes its boxes: cut one triangle out and one box goes. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void editResyncsBoxes(GameTestHelper helper) {
        BlockPos rel = new BlockPos(0, 6, 0);
        helper.setBlock(rel, FVehicles.FRAME.get());
        FrameBlockEntity be = (FrameBlockEntity) helper.getBlockEntity(rel);
        Frame f = be.frame();
        int h = B / 2;
        int a = f.add(new Vertex(0, h, 0)), c = f.add(new Vertex(2 * B - 1, h, 2 * B - 1));
        plateQuad(f, a, f.add(new Vertex(2 * B - 1, h, 0)), c, f.add(new Vertex(0, h, 2 * B - 1)));
        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        helper.assertTrue(FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result) != null, "should assemble");
        helper.runAfterDelay(5, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id()); // assembly moved the block entity
            helper.assertTrue(live.boxCount() == 2 && live.boxesSettled(), "two boxes to start, got " + live.boxCount());
            var first = live.frame().plates().iterator().next();
            live.frame().removePlate(first.a(), first.b(), first.c());
            live.changed();
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(live.boxCount() == 1 && live.boxesSettled(), "one box after cutting a plate, got " + live.boxCount());
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
        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null, "a tube frame should assemble, got " + result[0]);
        double y0 = body.logicalPose().position().y();
        helper.runAfterDelay(10, () -> {
            FrameBlockEntity live = FrameBlockEntity.loaded(be.id());
            helper.assertTrue(live.boxCount() == 1 && live.boxesSettled(), "the tube is one welded box, got " + live.boxCount());
            helper.assertTrue(body.logicalPose().position().y() < y0 - 0.1,
                    "it should fall, y went " + y0 + " to " + body.logicalPose().position().y());
            helper.succeed();
        });
    }

    /**
     * A point set to be a bearing, inside one frame, cuts the frame into two bodies on assembly and
     * joins them there: the pivot is only a setting until the chassis is a physics object.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void pivotInsideAFrameSplitsItIntoTwoBodies(GameTestHelper helper) {
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
        java.util.UUID id = be.id();

        FrameAssembly.Result[] result = new FrameAssembly.Result[1];
        var body = FrameAssembly.assemble(helper.getLevel(), helper.absolutePos(rel), be, result);
        helper.assertTrue(body != null, "should assemble, got " + result[0]);
        helper.assertTrue(be.joints().size() == 1 && be.joints().get(0).kind() == FrameJoint.Kind.BALL,
                "the pivot should become a ball joint, got " + be.joints());
        helper.assertTrue(be.frame().pivots().isEmpty() && be.frame().tubes().size() == 1,
                "the first part keeps one tube and no pivot");
        helper.runAfterDelay(10, () -> {
            FrameBlockEntity first = FrameBlockEntity.loaded(id);
            FrameBlockEntity second = FrameBlockEntity.loaded(first.joints().get(0).other());
            helper.assertTrue(second != null && second.assembled() && second != first, "the other half should be its own body");
            helper.assertTrue(second.frame().tubes().size() == 1, "the other half keeps the other tube");
            helper.assertTrue(first.liveJoints() == 1, "the pivot should be a live Sable joint");
            helper.succeed();
        });
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
