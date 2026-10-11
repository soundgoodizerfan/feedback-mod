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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An assembled frame's collision and mass: one free Sable box per plate and per tube, welded to
 * the body the frame block anchors ({@code feedback_vehicles_spec.md} section 4, <em>Bridging to
 * Sable</em>). Replaces the 1/16 block cells -- no block exists for a plate; the frame is what
 * the vehicle is made of and the boxes are how Sable meets it.
 *
 * <p>Boxes are Sable's {@code ArbitraryPhysicsObject}s. Sable does not save them: it simply
 * drops them when their chunk unloads. So they are derived state, never stored -- {@link #sync}
 * rebuilds whichever are missing from the {@link Frame}, the same way a joint's constraint handle
 * is rebuilt.
 *
 * <p>A plate is its triangle's bounding rectangle in the triangle's own plane, one
 * {@link Frame#PLATE_THICKNESS} deep, oriented along its longest edge. That over-covers a
 * triangle by up to its own area again, at the corner away from the long edge. [OPEN]: split into
 * smaller boxes if the ghost corner is ever felt.
 */
final class FrameBodies {

    private record Spec(Vector3d centre, Quaterniond rot, Vector3d half, double mass) {
        boolean same(Spec o) {
            return centre.distanceSquared(o.centre) < 1e-12 && half.distanceSquared(o.half) < 1e-12
                    && Math.abs(mass - o.mass) < 1e-9 && Math.abs(Math.abs(rot.dot(o.rot)) - 1) < 1e-9;
        }
    }

    private static final class Part {
        final Spec spec;
        final BoxPhysicsObject box;
        PhysicsConstraintHandle weld;

        Part(Spec spec, BoxPhysicsObject box) {
            this.spec = spec;
            this.box = box;
        }

        boolean ready() {
            return weld != null && weld.isValid() && box.getRuntimeId() != PhysicsPipelineBody.NULL_RUNTIME_ID;
        }
    }

    /** Keyed by the sorted vertex ids: three for a plate, two (and a -1) for a tube. */
    private final Map<List<Integer>, Part> parts = new HashMap<>();
    /**
     * One {@code Free} constraint per pair of boxes that touch, with contacts off. A {@code Free}
     * joint holds nothing -- which is what is wanted: the boxes are already held, by their welds,
     * and what is missing is only that they must not push one another apart. Rapier has no
     * collision groups to say so, and a plate's box overlaps its neighbours at every fold.
     */
    private final List<PhysicsConstraintHandle> quiet = new ArrayList<>();

    private record Across(int mine, int theirs, List<PhysicsConstraintHandle> handles) {
    }

    /** Boxes of frames joined to this one, made to ignore ours; see {@link #quietenAcross}. */
    private final Map<FrameBodies, Across> across = new HashMap<>();
    /** Bumped whenever the set of boxes changes, so {@link #across} knows it has gone stale. */
    private int generation;

    int size() {
        return parts.size();
    }

    /** True once every box is in the physics system and welded -- nothing left for a tick to do. */
    boolean settled() {
        for (Part p : parts.values())
            if (!p.ready())
                return false;
        return true;
    }

    /** Where box {@code i} (in key order, stable between calls) sits in the world, for tests. */
    List<Vec3> positions() {
        List<Vec3> out = new ArrayList<>();
        for (Part p : parts.values()) {
            if (p.box.getRuntimeId() != PhysicsPipelineBody.NULL_RUNTIME_ID)
                p.box.updatePose(); // the box only knows where Sable last put it when asked
            var at = p.box.getPose().position();
            out.add(new Vec3(at.x(), at.y(), at.z()));
        }
        return out;
    }

    /** Total mass of every box, for tests and for the driving model later. */
    double mass() {
        return parts.values().stream().mapToDouble(p -> p.spec.mass).sum();
    }

    /**
     * Brings the boxes in line with the frame: drops those no longer wanted or no longer the right
     * shape, makes those missing, welds those not yet welded. Cheap when nothing changed.
     */
    void sync(ServerLevel level, FrameBlockEntity be, ServerSubLevel body) {
        Map<List<Integer>, Spec> want = want(be);
        boolean changed = false;
        for (Iterator<Map.Entry<List<Integer>, Part>> it = parts.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Spec spec = want.get(e.getKey());
            if (spec == null || !spec.same(e.getValue().spec) || e.getValue().box.isRemoved()) {
                retire(level, e.getValue());
                it.remove();
                changed = true;
            }
        }
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(level);
        for (Map.Entry<List<Integer>, Spec> e : want.entrySet()) {
            Part part = parts.get(e.getKey());
            if (part == null) {
                Spec s = e.getValue();
                Pose3d pose = new Pose3d(body.logicalPose().transformPosition(s.centre, new Vector3d()),
                        new Quaterniond(body.logicalPose().orientation()).mul(s.rot), new Vector3d(), new Vector3d(1, 1, 1));
                part = new Part(s, new BoxPhysicsObject(pose, s.half, s.mass));
                system.addObject(part.box);
                parts.put(e.getKey(), part);
                changed = true;
            }
            if ((part.weld == null || !part.weld.isValid()) && part.box.getRuntimeId() != PhysicsPipelineBody.NULL_RUNTIME_ID) {
                // Anchor on the body is in its plot, on the box its own centre; the box was placed
                // already turned by spec.rot, which is the relative turn a Fixed joint holds.
                part.weld = system.getPipeline().addConstraint(body, part.box,
                        new FixedConstraintConfiguration(part.spec.centre, new Vector3d(), part.spec.rot));
                part.weld.setContactsEnabled(false); // the frame block sits inside its own plates
                changed = true;
            }
        }
        if (changed && settled())
            quieten(level);
    }

    /**
     * Makes two joined frames ignore each other entirely: every box of one against every box of the
     * other that is near it, and each side's boxes against the other's frame block. The joint
     * itself only switches off contact between the two bodies, which are now just their frame
     * blocks -- the plates are boxes of their own and have to be told separately.
     */
    void quietenAcross(ServerLevel level, FrameBlockEntity myBe, ServerSubLevel mine,
                       FrameBodies other, FrameBlockEntity otherBe, ServerSubLevel theirs) {
        if (!settled() || !other.settled())
            return;
        Across prior = across.get(other);
        if (prior != null && prior.mine == generation && prior.theirs == other.generation)
            return;
        if (prior != null)
            prior.handles.forEach(h -> {
                if (h.isValid())
                    h.remove();
            });
        var pipeline = SubLevelPhysicsSystem.get(level).getPipeline();
        List<PhysicsConstraintHandle> made = new ArrayList<>();
        Vector3d myBlock = block(myBe), theirBlock = block(otherBe);
        Vector3d origin = new Vector3d();
        Quaterniond none = new Quaterniond();
        for (Part a : parts.values()) {
            made.add(free(pipeline, a.box, theirs, origin, theirBlock, none));
            a.box.updatePose();
        }
        for (Part b : other.parts.values()) {
            made.add(free(pipeline, mine, b.box, myBlock, origin, none));
            b.box.updatePose();
        }
        for (Part a : parts.values())
            for (Part b : other.parts.values()) {
                var pa = a.box.getPose().position();
                var pb = b.box.getPose().position();
                double reach = a.spec.half.length() + b.spec.half.length() + 0.5;
                if (pa.distanceSquared(pb) <= reach * reach)
                    made.add(free(pipeline, a.box, b.box, origin, origin, none));
            }
        across.put(other, new Across(generation, other.generation, made));
    }

    private static Vector3d block(FrameBlockEntity be) {
        BlockPos p = be.getBlockPos();
        return new Vector3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
    }

    private static PhysicsConstraintHandle free(dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline,
                                                PhysicsPipelineBody a, PhysicsPipelineBody b,
                                                Vector3d pa, Vector3d pb, Quaterniond rot) {
        PhysicsConstraintHandle h = pipeline.addConstraint(a, b, new FreeConstraintConfiguration(pa, pb, rot));
        h.setContactsEnabled(false);
        return h;
    }

    /** Makes every pair of touching boxes ignore each other; see {@link #quiet}. */
    private void quieten(ServerLevel level) {
        generation++;
        for (PhysicsConstraintHandle h : quiet)
            if (h.isValid())
                h.remove();
        quiet.clear();
        var pipeline = SubLevelPhysicsSystem.get(level).getPipeline();
        List<Part> all = new ArrayList<>(parts.values());
        for (int i = 0; i < all.size(); i++)
            for (int j = i + 1; j < all.size(); j++) {
                Spec a = all.get(i).spec, b = all.get(j).spec;
                double reach = a.half.length() + b.half.length() + 0.1;
                if (a.centre.distanceSquared(b.centre) > reach * reach)
                    continue;
                PhysicsConstraintHandle h = pipeline.addConstraint(all.get(i).box, all.get(j).box,
                        new FreeConstraintConfiguration(new Vector3d(), new Vector3d(), new Quaterniond()));
                h.setContactsEnabled(false);
                quiet.add(h);
            }
    }

    /** Takes every box out of the physics system. */
    void clear(ServerLevel level) {
        for (PhysicsConstraintHandle h : quiet)
            if (h.isValid())
                h.remove();
        quiet.clear();
        for (Part p : parts.values())
            retire(level, p);
        parts.clear();
    }

    private void retire(ServerLevel level, Part p) {
        if (p.weld != null && p.weld.isValid())
            p.weld.remove();
        SubLevelPhysicsSystem.get(level).removeObject(p.box);
    }

    /** What the frame says its boxes should be, in the frame's plot space (frame block + local). */
    private static Map<List<Integer>, Spec> want(FrameBlockEntity be) {
        Frame frame = be.frame();
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        Map<List<Integer>, Spec> out = new HashMap<>();

        for (Frame.Plate p : frame.plates()) {
            Vec3 a = frame.pos(p.a()), b = frame.pos(p.b()), c = frame.pos(p.c());
            Vec3 normal = frame.cross(p);
            if (normal.lengthSqr() < 1e-12)
                continue;
            Vec3 n = normal.normalize();
            Vec3[] edges = {b.subtract(a), c.subtract(b), a.subtract(c)};
            Vec3 longest = edges[0];
            for (Vec3 e : edges)
                if (e.lengthSqr() > longest.lengthSqr())
                    longest = e;
            Vec3 u = longest.normalize();
            Vec3 v = n.cross(u);
            double[] us = {a.dot(u), b.dot(u), c.dot(u)}, vs = {a.dot(v), b.dot(v), c.dot(v)};
            double u0 = Math.min(us[0], Math.min(us[1], us[2])), u1 = Math.max(us[0], Math.max(us[1], us[2]));
            double v0 = Math.min(vs[0], Math.min(vs[1], vs[2])), v1 = Math.max(vs[0], Math.max(vs[1], vs[2]));
            // a.dot(n) is the plane's offset along n; the in-plane coordinates give the rest.
            Vec3 centre = u.scale((u0 + u1) / 2).add(v.scale((v0 + v1) / 2)).add(n.scale(a.dot(n))).add(origin);
            Spec spec = new Spec(vec(centre), rotation(u, v, n),
                    new Vector3d((u1 - u0) / 2, (v1 - v0) / 2, Frame.PLATE_THICKNESS / 2.0),
                    mass(frame.volume(p), p.materialState()));
            out.put(key(p.a(), p.b(), p.c()), spec);
        }

        for (Map.Entry<Long, Frame.Tube> t : frame.tubes().entrySet()) {
            int ia = Frame.edgeA(t.getKey()), ib = Frame.edgeB(t.getKey());
            Vec3 a = frame.pos(ia), b = frame.pos(ib);
            Vec3 along = b.subtract(a);
            if (along.lengthSqr() < 1e-12)
                continue;
            Vec3 u = along.normalize();
            Vec3 helper = Math.abs(u.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 w = u.cross(helper).normalize();
            Vec3 v = w.cross(u);
            double r = t.getValue().diameter() / 2;
            Spec spec = new Spec(vec(a.add(b).scale(0.5).add(origin)), rotation(u, v, w),
                    new Vector3d(along.length() / 2, r, r),
                    mass(frame.tubeVolume(t.getKey()), t.getValue().materialState()));
            out.put(key(ia, ib, -1), spec);
        }
        return out;
    }

    private static List<Integer> key(int a, int b, int c) {
        Integer[] k = {a, b, c};
        Arrays.sort(k);
        return List.of(k);
    }

    private static Vector3d vec(Vec3 v) {
        return new Vector3d(v.x, v.y, v.z);
    }

    /** The turn that takes the x, y, z axes to {@code u}, {@code v}, {@code w}. */
    private static Quaterniond rotation(Vec3 u, Vec3 v, Vec3 w) {
        Matrix3d m = new Matrix3d(u.x, u.y, u.z, v.x, v.y, v.z, w.x, w.y, w.z);
        return new Quaterniond().setFromNormalized(m).normalize();
    }

    /** A plate material's Sable mass for a full block, times volume in blocks; never zero. */
    private static double mass(double volume, BlockState material) {
        double perBlock = PhysicsBlockPropertyHelper.getMass(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, material);
        return Math.max(1e-3, volume * perBlock);
    }
}
