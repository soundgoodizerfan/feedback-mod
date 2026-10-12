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
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
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
 * An assembled frame's collision and mass: one free Sable box per plate and per tube
 * ({@code feedback_vehicles_spec.md} section 4, <em>Bridging to Sable</em>). The boxes of each
 * group -- the frame cut at its bearings -- are welded to one root box, and a group meets the
 * body only where the player welded one of its points to a block of the base ({@link
 * Frame#anchors()}). A group with no anchor, and no bearing to a group that has one, is loose. Replaces the 1/16 block cells -- no block exists for a plate; the frame is what
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
public final class FrameBodies {

    private record Spec(Vector3d centre, Quaterniond rot, Vector3d half, double mass) {
        boolean same(Spec o) {
            return centre.distanceSquared(o.centre) < 1e-12 && half.distanceSquared(o.half) < 1e-12
                    && Math.abs(mass - o.mass) < 1e-9 && Math.abs(Math.abs(rot.dot(o.rot)) - 1) < 1e-9;
        }
    }

    private static final class Part {
        final Spec spec;
        final BoxPhysicsObject box;
        /** The {@link FrameAssembly.Part#id} this box moves with; 0 if there are no bearings. */
        int group;
        /** True for the one box its group hangs from: the others weld to it, it takes the bearings and anchors. */
        boolean root;
        /** To the group's root box. Null for a root. */
        PhysicsConstraintHandle weld;
        /** Contact with the body switched off: boxes overlap the base's blocks by design. */
        PhysicsConstraintHandle quiet;

        Part(Spec spec, BoxPhysicsObject box, int group) {
            this.spec = spec;
            this.box = box;
            this.group = group;
        }

        boolean ready() {
            if (box.getRuntimeId() == PhysicsPipelineBody.NULL_RUNTIME_ID)
                return false;
            return quiet != null && quiet.isValid() && (root || (weld != null && weld.isValid()));
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
    /** The frame's own bearings, as live constraints between groups; rebuilt when they or the boxes change. */
    private final List<PhysicsConstraintHandle> pivotJoints = new ArrayList<>();
    private String pivotSignature = "";
    /** Each anchor as a live constraint between the body and the group holding its point. */
    private final List<PhysicsConstraintHandle> anchorJoints = new ArrayList<>();
    /** Anchors and whether each one's block is still there, as last made; see {@link #anchorsChanged}. */
    private String anchorSignature = "";

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

    /** How many of the frame's bearings are live constraints right now. */
    int livePivots() {
        return (int) pivotJoints.stream().filter(PhysicsConstraintHandle::isValid).count();
    }

    /** How many anchors are live constraints right now. */
    int liveAnchors() {
        return (int) anchorJoints.stream().filter(PhysicsConstraintHandle::isValid).count();
    }

    /** True if an anchor was added, removed, or lost its block since the joints were last made. */
    boolean anchorsChanged(ServerLevel level, FrameBlockEntity be) {
        return !anchorSignature.equals(anchorSignature(level, be));
    }

    private static String anchorSignature(ServerLevel level, FrameBlockEntity be) {
        StringBuilder out = new StringBuilder();
        new java.util.TreeMap<>(be.frame().anchors()).forEach((v, off) -> out.append(v).append('@').append(off.toShortString())
                .append(level.getBlockState(be.getBlockPos().offset(off)).isAir() ? '-' : '+').append(';'));
        return out.toString();
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
        List<FrameAssembly.Part> cut = FrameAssembly.parts(be.frame());
        if (cut.size() < 2)
            cut = List.of();
        Map<List<Integer>, Integer> groupOf = groups(be.frame(), cut);

        boolean changed = false;
        java.util.Set<Integer> dirty = new java.util.HashSet<>();
        for (Part p : parts.values())
            if (cut.isEmpty() ? p.group != 0 : !groupOf.containsValue(p.group))
                dirty.add(p.group); // the bearing that made this group is gone, or a new one cut it
        for (var e : parts.entrySet()) {
            Spec spec = want.get(e.getKey());
            if (spec == null || !spec.same(e.getValue().spec) || e.getValue().box.isRemoved()
                    || e.getValue().group != groupOf.getOrDefault(e.getKey(), 0))
                dirty.add(e.getValue().group);
        }
        // A group's boxes hang off one another, so one going takes the group with it.
        for (Iterator<Map.Entry<List<Integer>, Part>> it = parts.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Spec spec = want.get(e.getKey());
            if (dirty.contains(e.getValue().group) || spec == null) {
                retire(level, e.getValue());
                it.remove();
                changed = true;
            }
        }
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(level);
        for (Map.Entry<List<Integer>, Spec> e : want.entrySet()) {
            if (parts.containsKey(e.getKey()))
                continue;
            Spec s = e.getValue();
            Pose3d pose = new Pose3d(body.logicalPose().transformPosition(s.centre, new Vector3d()),
                    new Quaterniond(body.logicalPose().orientation()).mul(s.rot), new Vector3d(), new Vector3d(1, 1, 1));
            Part part = new Part(s, new BoxPhysicsObject(pose, s.half, s.mass), groupOf.getOrDefault(e.getKey(), 0));
            system.addObject(part.box);
            parts.put(e.getKey(), part);
            changed = true;
        }

        // The root of each group: the lowest key, so it is the same one every time.
        Map<Integer, Part> roots = new HashMap<>();
        parts.entrySet().stream()
                .sorted((x, y) -> compare(x.getKey(), y.getKey()))
                .forEach(e -> roots.putIfAbsent(e.getValue().group, e.getValue()));
        for (Part part : parts.values()) {
            part.root = roots.get(part.group) == part;
            if (part.box.getRuntimeId() == PhysicsPipelineBody.NULL_RUNTIME_ID)
                continue;
            if (part.quiet == null || !part.quiet.isValid()) {
                // Anchors only have to be valid: the body's must sit inside its plot, so the frame block.
                part.quiet = free(system.getPipeline(), body, part.box, block(be), new Vector3d(), new Quaterniond());
                changed = true;
            }
            Part root = roots.get(part.group);
            if (part.root || root.box.getRuntimeId() == PhysicsPipelineBody.NULL_RUNTIME_ID
                    || (part.weld != null && part.weld.isValid()))
                continue;
            // Hung from the group's root: its anchor is where this box's centre is, in the root's space.
            Quaterniond back = new Quaterniond(root.spec.rot).conjugate();
            part.weld = system.getPipeline().addConstraint(root.box, part.box,
                    new FixedConstraintConfiguration(back.transform(part.spec.centre.sub(root.spec.centre, new Vector3d())),
                            new Vector3d(), new Quaterniond(back).mul(part.spec.rot)));
            part.weld.setContactsEnabled(false);
            changed = true;
        }
        if (changed && settled())
            quieten(level);
        if (settled()) {
            String sig = signature(be.frame(), cut);
            if (changed || !sig.equals(pivotSignature)) {
                pivots(level, be, body, cut, roots);
                pivotSignature = sig;
            }
        }
        // Anchors go in as soon as their roots exist, not once everything has settled: until a
        // group is welded it is loose and falling, and the sooner it is held the less it falls.
        String anchors = anchorSignature(level, be);
        if (changed || !anchors.equals(anchorSignature))
            if (anchors(level, be, body, cut, roots))
                anchorSignature = anchors;
    }

    /**
     * Joins the body to each group holding an anchored point, at that point. The joint is a weld,
     * unless the point is a bearing -- then it is that bearing, so a wishbone's inner end set to a
     * hinge and welded to a block hinges on the block. An anchor whose block is gone holds nothing.
     * False if some group's root box is not in the physics system yet, so it is tried again.
     */
    private boolean anchors(ServerLevel level, FrameBlockEntity be, ServerSubLevel body,
                         List<FrameAssembly.Part> cut, Map<Integer, Part> roots) {
        for (PhysicsConstraintHandle h : anchorJoints)
            if (h.isValid())
                h.remove();
        anchorJoints.clear();
        boolean complete = true;
        var pipeline = SubLevelPhysicsSystem.get(level).getPipeline();
        Quaterniond frame = new Quaterniond(body.logicalPose().orientation());
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        for (Map.Entry<Integer, BlockPos> e : be.frame().anchors().entrySet()) {
            if (level.getBlockState(be.getBlockPos().offset(e.getValue())).isAir())
                continue;
            Vec3 at = be.frame().pos(e.getKey()).add(origin);
            Vector3d plot = new Vector3d(at.x, at.y, at.z);
            Frame.Pivot pivot = be.frame().pivots().get(e.getKey());
            FrameJoint.Kind kind = pivot == null ? FrameJoint.Kind.WELD : pivot.kind();
            Vec3 axis = pivot == null ? new Vec3(0, 1, 0) : pivot.axis();
            List<Integer> groups = new ArrayList<>();
            if (cut.isEmpty()) {
                if (roots.containsKey(0) && be.frame().vertices().containsKey(e.getKey()))
                    groups.add(0);
            } else
                for (FrameAssembly.Part c : cut)
                    if (c.vertices().contains(e.getKey()))
                        groups.add(c.id());
            for (int group : groups) {
                Part root = roots.get(group);
                if (root == null)
                    continue;
                if (root.box.getRuntimeId() == PhysicsPipelineBody.NULL_RUNTIME_ID) {
                    complete = false;
                    continue;
                }
                // The box's turn at rest, not as it is now: a group welded a few ticks late may
                // already be tumbling, and a weld holds whatever relative turn it is made with.
                PhysicsConstraintHandle h = FrameBlockEntity.constrain(pipeline,
                        body, new Vector3d(plot), new Quaterniond(frame),
                        root.box, anchor(group, plot, roots), new Quaterniond(frame).mul(root.spec.rot),
                        frame, kind, axis);
                h.setContactsEnabled(false);
                anchorJoints.add(h);
            }
        }
        return complete;
    }

    private static int compare(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < a.size(); i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0)
                return c;
        }
        return 0;
    }

    /** Which group each box belongs to; boxes not named here are in the main group. */
    private static Map<List<Integer>, Integer> groups(Frame frame, List<FrameAssembly.Part> cut) {
        Map<List<Integer>, Integer> out = new HashMap<>();
        for (FrameAssembly.Part c : cut) {
            for (Frame.Plate p : c.plates())
                out.putIfAbsent(key(p.a(), p.b(), p.c()), c.id());
            for (long e : c.edges())
                if (frame.tubes().containsKey(e))
                    out.putIfAbsent(key(Frame.edgeA(e), Frame.edgeB(e), -1), c.id());
        }
        return out;
    }

    private static String signature(Frame frame, List<FrameAssembly.Part> cut) {
        StringBuilder out = new StringBuilder();
        for (FrameAssembly.Part c : cut)
            out.append(c.id()).append('/').append(c.solids()).append(';');
        new java.util.TreeMap<>(frame.pivots()).forEach((v, p) -> out.append(v).append(':').append(p.kind())
                .append(p.axis()).append(';'));
        return out.toString();
    }

    /**
     * Makes each of the frame's bearings a constraint between the groups it separates. A group's
     * anchor is the body itself for the main group (a point in the plot) and its root box for any
     * other (the same point, in the box's own space). More than two groups meeting at one pivot
     * all join the first -- one body, not a chain.
     */
    private void pivots(ServerLevel level, FrameBlockEntity be, ServerSubLevel body,
                        List<FrameAssembly.Part> cut, Map<Integer, Part> roots) {
        for (PhysicsConstraintHandle h : pivotJoints)
            if (h.isValid())
                h.remove();
        pivotJoints.clear();
        if (cut.isEmpty())
            return;
        var pipeline = SubLevelPhysicsSystem.get(level).getPipeline();
        Quaterniond frame = new Quaterniond(body.logicalPose().orientation());
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        for (Map.Entry<Integer, Frame.Pivot> e : be.frame().pivots().entrySet()) {
            Vec3 at = be.frame().pos(e.getKey()).add(origin);
            Vector3d plot = new Vector3d(at.x, at.y, at.z);
            Integer first = null;
            for (FrameAssembly.Part c : cut) {
                if (!c.vertices().contains(e.getKey()))
                    continue;
                if (first == null) {
                    first = c.id();
                    continue;
                }
                PhysicsPipelineBody a = bodyOf(first, roots), b = bodyOf(c.id(), roots);
                if (a == null || b == null)
                    continue;
                PhysicsConstraintHandle h = FrameBlockEntity.constrain(pipeline,
                        a, anchor(first, plot, roots), orientation(first, roots),
                        b, anchor(c.id(), plot, roots), orientation(c.id(), roots),
                        frame, e.getValue().kind(), e.getValue().axis());
                h.setContactsEnabled(false); // the two sides touch at the bearing by design
                pivotJoints.add(h);
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    private PhysicsPipelineBody bodyOf(int group, Map<Integer, Part> roots) {
        Part root = roots.get(group);
        return root == null ? null : root.box;
    }

    /** {@code plot} point in the space of the thing {@code group} hangs from. */
    private Vector3d anchor(int group, Vector3d plot, Map<Integer, Part> roots) {
        Spec root = roots.get(group).spec;
        return new Quaterniond(root.rot).conjugate().transform(plot.sub(root.centre, new Vector3d()));
    }

    private Quaterniond orientation(int group, Map<Integer, Part> roots) {
        Part root = roots.get(group);
        root.box.updatePose();
        return new Quaterniond(root.box.getPose().orientation());
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
        for (PhysicsConstraintHandle h : pivotJoints)
            if (h.isValid())
                h.remove();
        pivotJoints.clear();
        for (PhysicsConstraintHandle h : anchorJoints)
            if (h.isValid())
                h.remove();
        anchorJoints.clear();
        anchorSignature = "";
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
        if (p.quiet != null && p.quiet.isValid())
            p.quiet.remove();
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

    /**
     * A plate's or tube's Sable mass, {@code volume} in blocks; never zero. Its material's density
     * times its volume (a block is 1000 mB), read from the material system. A block no material
     * claims, or one nobody has weighed, falls back to Sable's own per-block mass.
     */
    public static double mass(double volume, BlockState material) {
        var found = Material.find(MaterialTable.get().materials(), material.getBlock().asItem());
        if (found.isPresent() && found.get().material().density().muPerMb() > 0)
            return Math.max(1e-3, Units.kpg(new Mu(found.get().material().density().muPerMb() * (float) volume * 1000f)));
        double perBlock = PhysicsBlockPropertyHelper.getMass(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, material);
        return Math.max(1e-3, volume * perBlock);
    }
}
