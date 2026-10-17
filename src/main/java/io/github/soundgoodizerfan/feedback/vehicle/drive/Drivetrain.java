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
package io.github.soundgoodizerfan.feedback.vehicle.drive;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameAssembly;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The free-form drivetrain of one frame, {@code feedback_vehicles_spec.md} section 5: one member
 * of the block network at the shaft a tube is welded to, and the ray tires that put it on the road.
 * <p>
 * The <b>coupling node</b> is a {@link RotationNode} that a tube is welded to
 * ({@link Frame#anchors()}) on either end of its axis -- the tube runs straight out of it, on its
 * axis. A shaft, an engine, anything turning; there is no flange block. From
 * there the <b>chain</b> runs along tubes, across driving bearings and straight joins, to the hubs
 * it reaches: a hub on the chain whose bearing drives is driven, every other hub rolls free.
 * <p>
 * Speed flows down, from the network to each hub; force flows up. Every physics step each hub
 * casts three rays at the road (Offroad's patch); a hit is a contact, pushed by the tire as its
 * own spring and gripping by a slip coupling capped at {@code friction x load}. The car gets the
 * force; the shaft gets the torque it costs, averaged over the game tick, as a signed push
 * through {@link RotationNetwork#driveExternally}. A tire spinning faster than the ground is load
 * on the engine, one turned by a rolling car is a push -- the spec's two-way slip coupling.
 * <p>
 * Derived from the frame and rebuilt when an edit marks it stale, like the cells; whether the
 * welded block is a shaft is asked live, since a block can change without the frame being edited.
 */
public final class Drivetrain {

    /** How far below the tire's own radius a ray still looks for road, in blocks -- droop. */
    private static final double REACH = 0.25;
    /** A hub whose axle sits nearer square to the shaft than this cosine is not turned by it. */
    private static final double MIN_JOINT_COS = 0.3;
    /** Guard rays round a tire's rim; those nearer straight down than the cosine are the ground rays' patch. */
    private static final int GUARD_RAYS = 12;
    private static final double GUARD_SKIP_COS = 0.85;
    private static final double RPM_PER_RAD_S = 60 / (2 * Math.PI);
    /**
     * Inertia (Su t/RPM) per kg m^2: the network steps speed by torque over inertia once a tick, so
     * one RPM a tick is 20 x 2 pi / 60 rad/s^2.
     */
    private static final float INERTIA_PER_KG_M2 = (float) (20 * 2 * Math.PI / 60);

    /**
     * A welded point that couples if its block has a shaft on {@code face}, with what its chain
     * reaches: each driven hub's vertex and its turning sense against the chain, and the chain's
     * own drag and inertia in the coupling node's frame.
     */
    private record Weld(BlockPos offset, Direction face, Map<Integer, Integer> driven, float drag, float inertia, Map<Long, Integer> tubes) {}

    /** A tire on a hinge, fixed until the next edit. {@code local} and {@code axis} are frame space. */
    private record Hub(int vertex, Vec3 local, Vec3 axis, double radius, double friction, double rolling, float inertia, boolean anchored) {}

    private final List<Weld> welds = new ArrayList<>();
    private final List<Hub> hubs = new ArrayList<>();
    /** The frame's {@code Vdrag} seen along +x, -x, +y, -y, +z, -z of the body. */
    private final float[] vdrag = new float[6];

    /**
     * A weld coupling this game tick: its shaft's spin about the face the tube leaves by (rad/s),
     * what its chain took from the network (Su), and -- summed over the physics steps since the
     * last game tick, then reported and cleared -- its tires' torque and slip. One per weld that
     * couples: a frame with two engines has two drive lines, each on its own network. The first
     * build kept one, the first weld found, and a second line stood still until the first was
     * broken (playtest 2026-10-16).
     */
    private static final class Coupling {
        final Weld weld;
        double omega;
        float su;
        double torqueTimesDt, slipTimesDt, groundedTime;

        Coupling(Weld weld) {
            this.weld = weld;
        }
    }

    /** Every weld's running sums, kept across ticks so a tick reports the steps since the last. */
    private final Map<Weld, Coupling> sums = new HashMap<>();
    /** The welds coupling this game tick. */
    private final List<Coupling> coupled = new ArrayList<>();
    private double stepTime;

    private final ForceTotal bodyForces = new ForceTotal();
    /**
     * What each hub did on its last physics step, for the eye ({@link #spin}) and the debug helmet
     * ({@link #status}): its spin in rad/s, and the load it bore (kpg blocks/s^2, so kN), 0 airborne.
     */
    private final Map<Integer, Float> hubSpin = new HashMap<>();
    private final Map<Integer, Float> hubLoad = new HashMap<>();
    private final Map<Block, Double> groundRolling = new HashMap<>();

    public void rebuild(Frame frame) {
        welds.clear();
        sums.clear();
        coupled.clear();
        groundRolling.clear();
        hubs.clear();
        hubSpin.clear();
        hubLoad.clear();
        var materials = MaterialTable.get().materials();
        frame.tires().forEach((vertex, tire) -> {
            Frame.Pivot pivot = frame.pivots().get(vertex);
            var found = Material.find(materials, tire.materialState().getBlock().asItem());
            double friction = found.flatMap(m -> m.material().mechanical().friction()).orElse(0f);
            double rolling = found.map(m -> deforms(m.material())).orElse((double) FTuning.ROLLING_UNKNOWN_GROUND);
            double r = tire.radius();
            // A disc the tire's radius across and as wide as its radius.
            double kg = FrameAssembly.mass(Math.PI * r * r * r, tire.materialState()) * Units.MU_PER_KPG;
            hubs.add(new Hub(vertex, frame.pos(vertex), pivot.axis().normalize(), r, friction, rolling,
                    (float) (0.5 * kg * r * r) * INERTIA_PER_KG_M2, frame.anchors().containsKey(vertex)));
        });
        frame.anchors().forEach((vertex, offset) -> {
            Frame.Vertex v = frame.vertices().get(vertex);
            Frame.Pivot joint = frame.pivots().get(vertex);
            // A point that carries rotation is a universal joint on the block's face: the tube may
            // leave it at any angle (playtest 2026-10-16). A plain weld is rigid, so its tube must
            // run straight out along the axis -- bent, it would be a crank.
            Direction onFace = joint != null && joint.drives() ? faceOf(frame.pos(vertex), offset) : null;
            for (long key : frame.tubesAt(vertex)) {
                Frame.Vertex o = frame.vertices().get(other(key, vertex));
                Direction face = onFace != null ? onFace : Direction.fromDelta(Integer.signum(o.x() - v.x()),
                        Integer.signum(o.y() - v.y()), Integer.signum(o.z() - v.z()));
                if (face != null)
                    welds.add(chain(frame, vertex, key, offset, face));
            }
        });
        Direction[] faces = {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};
        for (int i = 0; i < 6; i++)
            vdrag[i] = frame.vdrag(Vec3.atLowerCornerOf(faces[i].getNormal())).areaBlocks2();
    }

    /**
     * Walks out from the weld along tubes. A driving bearing passes rotation on at any angle (a
     * universal joint turns at the speed it is given); a plain point passes it only into a tube in
     * line, since a bent rigid shaft is a crank, not a shaft. A hub ends the walk.
     */
    private Weld chain(Frame frame, int start, long first, BlockPos offset, Direction face) {
        Map<Integer, Integer> driven = new HashMap<>();
        float drag = 0, inertia = tubeInertia(frame, first);
        Set<Long> seen = new HashSet<>(List.of(first));
        // Each tube's turning against its own first-to-second-point direction, for drawing it.
        Map<Long, Integer> tubes = new HashMap<>();
        tubes.put(first, against(frame, first, dir(frame, start, first)));
        // Each step carries its tube's spin as a direction: the coupling's spin (about the face)
        // times this vector is the tube's angular velocity. The first tube leaves the face outward,
        // so it spins about its own outward direction.
        record Step(long tube, int from, Vec3 spin) {}
        ArrayDeque<Step> todo = new ArrayDeque<>(List.of(new Step(first, start, dir(frame, start, first))));
        while (!todo.isEmpty()) {
            Step s = todo.pop();
            int at = other(s.tube, s.from);
            Vec3 along = frame.pos(at).subtract(frame.pos(s.from)).normalize();
            double sense = Math.signum(s.spin.dot(along)); // spin relative to the way the walk goes
            Frame.Pivot pivot = frame.pivots().get(at);
            boolean drives = pivot != null && pivot.drives();
            if (frame.tires().containsKey(at)) {
                Vec3 axis = pivot.axis().normalize();
                if (drives && Math.abs(along.dot(axis)) > MIN_JOINT_COS) {
                    driven.put(at, (int) Math.signum(s.spin.dot(axis)));
                    drag += FTuning.SHAFT_DRAG_SU_PER_RPM.suPerRpm();
                }
                continue;
            }
            if (drives)
                drag += FTuning.SHAFT_DRAG_SU_PER_RPM.suPerRpm();
            List<Long> outs = new ArrayList<>();
            for (long next : frame.tubesAt(at))
                if (!seen.contains(next))
                    outs.add(next);
            for (long next : outs) {
                Vec3 out = dir(frame, at, next);
                Vec3 spin;
                if (out.dot(along) > 0.999)
                    spin = s.spin; // straight on: one rod, one spin
                else if (!drives)
                    continue; // a bent rigid shaft is a crank
                else {
                    // A universal joint keeps the spin's sense relative to the walk. But two tubes
                    // leaving in one straight line are one axle through the joint, driven from the
                    // side like a solid rear axle off its ring gear: both halves turn as one rod.
                    // Walked outward as two U-joints, the halves spun mirror-image and the wheels
                    // fought (playtest 2026-10-16). The axle's spin is set by whichever half sorts
                    // first, so it is the same however the walk arrives.
                    Vec3 axle = out;
                    for (long o : outs)
                        if (o != next && dir(frame, at, o).dot(out) < -0.999) {
                            Vec3 d = dir(frame, at, o);
                            axle = first(out, d);
                        }
                    spin = axle.scale(sense);
                }
                seen.add(next);
                tubes.put(next, against(frame, next, spin));
                inertia += tubeInertia(frame, next);
                todo.push(new Step(next, at, spin));
            }
        }
        return new Weld(offset, face, driven, drag, inertia, tubes);
    }

    /** +1 if spin {@code spin} turns tube {@code key} about its first-to-second-point direction, else -1. */
    private static int against(Frame frame, long key, Vec3 spin) {
        return spin.dot(dir(frame, Frame.edgeA(key), key)) >= 0 ? 1 : -1;
    }

    /** Unit direction of tube {@code key} leaving vertex {@code from}. */
    private static Vec3 dir(Frame frame, int from, long key) {
        return frame.pos(other(key, from)).subtract(frame.pos(from)).normalize();
    }

    /** Of two opposite directions, the one that sorts first: x, then y, then z, positive first. */
    private static Vec3 first(Vec3 a, Vec3 b) {
        if (Math.abs(a.x - b.x) > 1e-6)
            return a.x > b.x ? a : b;
        if (Math.abs(a.y - b.y) > 1e-6)
            return a.y > b.y ? a : b;
        return a.z >= b.z ? a : b;
    }

    /** The face of the block at {@code offset} that point {@code p} sits on or beyond; null if inside. */
    @Nullable
    private static Direction faceOf(Vec3 p, BlockPos offset) {
        double dx = p.x - Math.clamp(p.x, offset.getX(), offset.getX() + 1),
                dy = p.y - Math.clamp(p.y, offset.getY(), offset.getY() + 1),
                dz = p.z - Math.clamp(p.z, offset.getZ(), offset.getZ() + 1);
        if (dx == 0 && dy == 0 && dz == 0)
            return null;
        return Direction.getNearest(dx, dy, dz);
    }

    /** A tube spinning about itself: a rod's half m r^2. */
    private static float tubeInertia(Frame frame, long key) {
        Frame.Tube tube = frame.tubes().get(key);
        double r = tube.diameter() / 2;
        double kg = FrameAssembly.mass(frame.tubeVolume(key), tube.materialState()) * Units.MU_PER_KPG;
        return (float) (0.5 * kg * r * r) * INERTIA_PER_KG_M2;
    }

    private static int other(long key, int vertex) {
        return Frame.edgeA(key) == vertex ? Frame.edgeB(key) : Frame.edgeA(key);
    }

    /**
     * Once a game tick: finds the coupling, reads its shaft's speed for the next physics steps,
     * and reports what the tires did over the last ones. Runs in the frame's block entity tick,
     * before the network's ({@code LevelTickEvent.Post}) -- inside a body's plot too.
     */
    public void tick(Level level, BlockPos framePos) {
        double steps = stepTime;
        stepTime = 0;
        coupled.clear();
        for (Weld w : welds) {
            Coupling c = sums.computeIfAbsent(w, Coupling::new);
            double torque = steps > 0 ? c.torqueTimesDt / steps : 0;
            double slip = c.groundedTime > 0 ? c.slipTimesDt / c.groundedTime : 0;
            c.torqueTimesDt = c.slipTimesDt = c.groundedTime = 0;
            c.su = 0;
            BlockPos at = framePos.offset(w.offset);
            BlockState state = level.getBlockState(at);
            // Either end of anything turning couples -- an engine's back as well as its output, a
            // cog's face -- not only a face with a shaft on it: a tube welded on the axis turns with
            // the block (author, 2026-10-16). A tube off the side would be a crank, so not that.
            if (!(state.getBlock() instanceof Rotatable shaft)
                    || !(shaft.hasShaftTowards(level, at, state, w.face) || shaft.getRotationAxis(state) == w.face.getAxis())
                    || !(level.getBlockEntity(at) instanceof RotationNode node) || node.getNetwork() == null)
                continue;
            // A hub on two lines turns with the first; the second does not drive it as well.
            if (coupled.stream().anyMatch(o -> o.weld.driven.keySet().stream().anyMatch(w.driven::containsKey)))
                continue;
            coupled.add(c);
            // A node's speed is about its axis's positive end; the tube leaves by w.face.
            float sense = shaft.shaftSignTowards(state, w.face)
                    * (w.face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
            c.omega = node.getRpm().value() * sense / RPM_PER_RAD_S;
            float ratio = node.getRatio();
            RotationNetwork run = node.getNetwork();
            float hubInertia = 0;
            for (Hub h : hubs)
                if (w.driven.containsKey(h.vertex))
                    hubInertia += h.inertia;
            float inertiaRun = (w.inertia + hubInertia) * ratio * ratio;
            // Into the run's frame: push by 1/ratio, as recalculate refers members.
            double push = torque * sense / ratio;
            // Never more than brings the hubs to the road's speed in one network step (spec
            // section 5, Stability): the slip is sampled once a tick against a network stepped
            // once a tick, and a stiff coupling between them would ring.
            double slipRun = slip * RPM_PER_RAD_S * sense / ratio;
            double bound = (run.getInertia().suTicksPerRpm() + inertiaRun) * Math.abs(slipRun) / FTuning.INERTIA_RESPONSE;
            push = Math.clamp(push, -bound, bound);
            run.driveExternally(new Su((float) push), new Drag(w.drag * ratio * ratio), new Inertia(inertiaRun));
            c.su = (float) (Math.abs(push) + w.drag * ratio * Math.abs(node.getRpm().value()));
        }
    }

    /** The coupling driving hub {@code vertex}, if any. */
    @Nullable
    private Coupling drivenBy(int vertex) {
        for (Coupling c : coupled)
            if (c.weld.driven.containsKey(vertex))
                return c;
        return null;
    }

    /**
     * Every physics step: each hub's contact with the road, and the air. Forces go to the body,
     * or to the body of the group holding the hub when it is not welded to a block -- the arms the
     * player built carry the car, so a bad wishbone still drives badly. The rays look past every
     * body of this frame: the cells round a hub are its own axle, not the road.
     */
    public void physicsTick(ServerLevel level, FrameBlockEntity frame, ServerSubLevel subLevel,
                            RigidBodyHandle handle, double dt) {
        stepTime += dt;
        BlockPos framePos = frame.getBlockPos();
        Set<dev.ryanhcode.sable.sublevel.SubLevel> mine = new HashSet<>(List.of(subLevel));
        for (FrameBlockEntity.GroupBody g : frame.groupBodies().values()) {
            var body = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level).getSubLevel(g.body());
            if (body != null)
                mine.add(body);
        }
        Pose3dc pose = subLevel.logicalPose();
        MassData mass = subLevel.getMassTracker();
        Vec3 origin = Vec3.atLowerCornerOf(framePos);
        double omegaN = 2 * Math.PI * FTuning.TIRE_SPRING_HZ;
        boolean any = false;

        for (Hub hub : hubs) {
            Vec3 p = hub.local.add(origin);
            Vector3d plot = new Vector3d(p.x, p.y, p.z);
            FrameBlockEntity.Mount mount = hub.anchored ? null : frame.mount(level, hub.vertex);
            if (mount != null && mount.body() == subLevel)
                mount = null;
            Pose3dc on;
            Vector3d centre;
            if (mount == null) {
                on = pose;
                centre = pose.transformPosition(plot, new Vector3d());
            } else {
                on = mount.body().logicalPose();
                Vec3 there = hub.local.add(Vec3.atLowerCornerOf(mount.origin()));
                centre = on.transformPosition(new Vector3d(there.x, there.y, there.z), new Vector3d());
            }
            Quaterniond rot = new Quaterniond(on.orientation());
            Vector3d axis = rot.transform(new Vector3d(hub.axis.x, hub.axis.y, hub.axis.z));
            Vector3d down = rot.transform(new Vector3d(0, -1, 0));
            down.fma(-down.dot(axis), axis);
            if (down.lengthSquared() < 0.01)
                continue; // an axle stood on end has no tread under it
            down.normalize();
            Vector3d along = axis.cross(down, new Vector3d());
            guard(level, hub, centre, axis, down, mount == null ? subLevel : mount.body(), on, mass, pose, omegaN, dt);

            // Three rays down the tread, Offroad's patch: front, middle, back.
            double reach = hub.radius + REACH, h = Double.MAX_VALUE;
            BlockState ground = null;
            Vector3d normal = null;
            for (int k = -1; k <= 1; k++) {
                Vector3d from = new Vector3d(along).mul(k * 0.5 * hub.radius).add(centre);
                Vec3 a = new Vec3(from.x, from.y, from.z);
                Vec3 b = a.add(down.x * reach, down.y * reach, down.z * reach);
                ClipContext ray = new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
                ((ClipContextExtension) ray).sable$setSubLevelIgnoring(mine::contains);
                BlockHitResult hit = level.clip(ray);
                if (hit.getType() == HitResult.Type.MISS)
                    continue;
                Vec3 at = hit.getLocation();
                var face = hit.getDirection().getNormal();
                Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
                var other = SableCompanion.INSTANCE.getContaining(level, at);
                if (other != null) {
                    at = other.logicalPose().transformPosition(at);
                    other.logicalPose().transformNormal(n);
                }
                double d = (at.x - a.x) * down.x + (at.y - a.y) * down.y + (at.z - a.z) * down.z;
                if (d < h) {
                    h = d;
                    ground = level.getBlockState(hit.getBlockPos());
                    normal = n;
                }
            }
            if (ground == null || h >= hub.radius) {
                hubLoad.put(hub.vertex, 0f);
                Coupling air = drivenBy(hub.vertex);
                if (air != null)
                    hubSpin.put(hub.vertex, (float) (air.omega * air.weld.driven.get(hub.vertex)));
                continue; // airborne: no force, and a driven hub spins free to its target
            }
            double compression = hub.radius - h;
            Vector3d lever = new Vector3d(down).mul(h);
            Vector3d contact = new Vector3d(centre).add(lever);
            // The road pushes along its own normal, not the car's: the spring sets how hard, the
            // arms carry it. Along the body's up instead, a car tilted on its springs was shoved
            // level-ground-wise forever -- the drift that rolled a car one way with the engine off.
            // A face the tread meets edge-on (a step's side) bears that much less.
            double bearing = -normal.dot(down);
            if (bearing <= 0.05)
                continue;

            Vector3d v = velocity(mount, on, handle, contact);
            Vector3d local = pose.transformPositionInverse(contact, new Vector3d());
            // This tire's share of the car: what the body weighs at this point, split between the hubs.
            double share = 1 / mass.getInverseNormalMass(local, pose.transformNormalInverse(normal, new Vector3d())) / hubs.size();
            // The damper never takes more than the closing speed in one step: past that an
            // explicit step overshoots, and the car leaves the ground.
            double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(normal) / dt;
            // The spring carries the car's weight split over its tires, so it squats g / (2 pi f)^2
            // however the tires are spread. Sized by the effective mass at the contact instead --
            // what the damper and grip rightly use -- a tire far from the middle was a soft spring
            // and the car sat a quarter of a tire deep in the road (playtest 2026-10-16).
            double carried = mass.getMass() / hubs.size();
            double load = (carried * omegaN * omegaN * compression - damping) * bearing;
            hubLoad.put(hub.vertex, (float) Math.max(load, 0));
            if (load <= 0)
                continue;
            double grip = hub.friction * fudge(PhysicsBlockPropertyHelper.getFriction(ground));

            // The tread's own speed at the contact, from the hub's spin about its axle.
            Coupling by = drivenBy(hub.vertex);
            Integer sense = by == null ? null : by.weld.driven.get(hub.vertex);
            Vector3d tread = axis.cross(lever, new Vector3d());
            double treadLength = tread.length();
            Vector3d slip;
            double omega = treadLength > 1e-9 ? -v.dot(tread) / (treadLength * treadLength) : 0; // rolling with the road
            if (sense != null) {
                omega = by.omega * sense;
                slip = new Vector3d(tread).mul(omega).add(v);
            } else
                slip = new Vector3d(axis).mul(v.dot(axis)); // a free wheel rolls: only sideways slips
            slip.fma(-slip.dot(normal), normal);
            Vector3d friction = new Vector3d(slip).mul(-share * FTuning.TIRE_SLIP_RESPONSE);
            double cap = grip * load * dt;
            if (friction.length() > cap)
                friction.normalize(cap);
            // Rolling resistance: the tread flexing as it rolls, against the way the road moves
            // under it, never more than stops it this step. Without it a pushed car coasted on.
            if (treadLength > 1e-9) {
                Vector3d roll = new Vector3d(tread).div(treadLength);
                roll.fma(-roll.dot(normal), normal);
                double rolling = v.dot(roll);
                double crr = hub.rolling + groundRolling(ground);
                double resist = Math.min(crr * load * dt, share * Math.abs(rolling));
                friction.fma(-Math.signum(rolling) * resist, roll);
            }
            Vector3d impulse = new Vector3d(normal).mul(load * dt).add(friction);
            push(mount, on, mass, contact, impulse);
            hubSpin.put(hub.vertex, (float) omega);
            any = true;

            if (sense != null && treadLength > 1e-9) {
                // The road's torque on the wheel, about its axle, back up the chain to the shaft.
                double onWheel = axis.dot(lever.cross(friction, new Vector3d())) / dt;
                by.torqueTimesDt += onWheel * sense * Units.SU_PER_KPG_TORQUE * dt;
                double rolling = -v.dot(tread) / (treadLength * treadLength);
                by.slipTimesDt += (omega - rolling) * sense * dt;
                by.groundedTime += dt;
            }
        }

        // Air, on the body as a whole: the frame's silhouette along the way it is going.
        Vector3d vel = handle.getLinearVelocity(new Vector3d());
        double speed = vel.length();
        if (speed > 1e-3) {
            Vector3d dir = pose.transformNormalInverse(new Vector3d(vel).div(speed), new Vector3d());
            double area = Math.max(dir.x, 0) * vdrag[0] + Math.max(-dir.x, 0) * vdrag[1]
                    + Math.max(dir.y, 0) * vdrag[2] + Math.max(-dir.y, 0) * vdrag[3]
                    + Math.max(dir.z, 0) * vdrag[4] + Math.max(-dir.z, 0) * vdrag[5];
            bodyForces.applyLinearImpulse(dir.mul(-FTuning.AIR_HALF_DENSITY_KPG_PER_BLOCK3 * area * speed * speed * dt));
            any = true;
        }
        if (any)
            handle.applyForcesAndReset(bodyForces);
    }

    /**
     * The rest of the rim: rays out from the hub in the tire's plane, all round but the patch the
     * ground rays already cover. A hit inside the radius pushes the tire out along the struck
     * face, with the ground contact's spring and damper and no grip -- so a wheel stops at a wall
     * or a step and no longer passes through the world at speed. Sable's colliders cannot carry it:
     * physics is half-block octants baked per state, and no tire is big enough to be one
     * (playtest 2026-10-16). The struck body, if any but the hub's own, takes the push back: a
     * tire on a swinging arm hitting the car's own blocks pushes the car too, and never itself.
     */
    private void guard(ServerLevel level, Hub hub, Vector3d centre, Vector3d axis, Vector3d down,
                          ServerSubLevel own, Pose3dc on, MassData mass, Pose3dc pose, double omegaN, double dt) {
        Vector3d side = axis.cross(down, new Vector3d());
        for (int k = 0; k < GUARD_RAYS; k++) {
            double a = 2 * Math.PI * k / GUARD_RAYS;
            Vector3d dir = new Vector3d(down).mul(Math.cos(a)).fma(Math.sin(a), side);
            if (dir.dot(down) > GUARD_SKIP_COS)
                continue; // the ground rays' patch
            Vec3 from = new Vec3(centre.x, centre.y, centre.z);
            Vec3 to = from.add(dir.x * hub.radius, dir.y * hub.radius, dir.z * hub.radius);
            ClipContext ray = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) ray).sable$setSubLevelIgnoring(b -> b == own);
            BlockHitResult hit = level.clip(ray);
            if (hit.getType() == HitResult.Type.MISS)
                continue;
            Vec3 at = hit.getLocation();
            var face = hit.getDirection().getNormal();
            Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
            var other = SableCompanion.INSTANCE.getContaining(level, at);
            if (other != null) {
                at = other.logicalPose().transformPosition(at);
                other.logicalPose().transformNormal(n);
            }
            Vector3d contact = new Vector3d(at.x, at.y, at.z);
            double depth = hub.radius - contact.distance(centre);
            double facing = -n.dot(dir);
            if (depth <= 0 || facing <= 0.05)
                continue;
            RigidBodyHandle mine = RigidBodyHandle.of(own);
            RigidBodyHandle theirs = other instanceof ServerSubLevel o ? RigidBodyHandle.of(o) : null;
            Vector3d v = mine == null ? new Vector3d() : pointVelocity(mine, on, contact);
            if (theirs != null)
                v.sub(pointVelocity(theirs, other.logicalPose(), contact));
            Vector3d local = pose.transformPositionInverse(contact, new Vector3d());
            double share = 1 / mass.getInverseNormalMass(local, pose.transformNormalInverse(n, new Vector3d())) / hubs.size();
            double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(n) / dt;
            double push = (share * omegaN * omegaN * depth - damping) * facing;
            if (push <= 0)
                continue;
            Vector3d impulse = new Vector3d(n).mul(push * dt);
            impulseAt(own, contact, impulse);
            if (other instanceof ServerSubLevel o)
                impulseAt(o, contact, impulse.negate(new Vector3d()));
        }
    }

    private static Vector3d pointVelocity(RigidBodyHandle h, Pose3dc pose, Vector3d at) {
        Vector3d lin = h.getLinearVelocity(new Vector3d());
        Vector3d ang = h.getAngularVelocity(new Vector3d());
        return ang.cross(new Vector3d(at).sub(pose.position()), new Vector3d()).add(lin);
    }

    /** A world impulse at a world point, straight onto {@code body}. */
    private static void impulseAt(ServerSubLevel body, Vector3d at, Vector3d impulse) {
        RigidBodyHandle h = RigidBodyHandle.of(body);
        if (h == null)
            return;
        Pose3dc pose = body.logicalPose();
        ForceTotal one = new ForceTotal();
        one.applyImpulseAtPoint(body.getMassTracker(), pose.transformPositionInverse(at, new Vector3d()),
                pose.transformNormalInverse(impulse, new Vector3d()));
        h.applyForcesAndReset(one);
    }

    /** World velocity of the world point {@code at}, moving with the body or with {@code mount}'s. */
    private static Vector3d velocity(@Nullable FrameBlockEntity.Mount mount, Pose3dc on, RigidBodyHandle handle, Vector3d at) {
        RigidBodyHandle h = mount == null ? handle : RigidBodyHandle.of(mount.body());
        if (h == null)
            return new Vector3d();
        Vector3d lin = h.getLinearVelocity(new Vector3d());
        Vector3d ang = h.getAngularVelocity(new Vector3d());
        // The pose's position is where its rotation point -- its centre of mass -- is in the world.
        return ang.cross(new Vector3d(at).sub(on.position()), new Vector3d()).add(lin);
    }

    /** A world impulse at a world point: summed for the body, applied at once to a group's own. */
    private void push(@Nullable FrameBlockEntity.Mount mount, Pose3dc on, MassData mass, Vector3d at, Vector3d impulse) {
        Vector3d where = on.transformPositionInverse(at, new Vector3d());
        Vector3d what = on.transformNormalInverse(impulse, new Vector3d());
        if (mount == null) {
            bodyForces.applyImpulseAtPoint(mass, where, what);
            return;
        }
        ForceTotal one = new ForceTotal();
        one.applyImpulseAtPoint(mount.body().getMassTracker(), where, what);
        RigidBodyHandle h = RigidBodyHandle.of(mount.body());
        if (h != null)
            h.applyForcesAndReset(one);
    }

    /**
     * One side's share of rolling resistance: soft things flex ({@link FTuning#ROLLING_SOFT}), and
     * slick things lose next to nothing at the contact ({@link FTuning#ROLLING_SLICK}).
     */
    private static double deforms(Material m) {
        double slick = Math.min(1, m.mechanical().friction().orElse(FTuning.ROLLING_SLICK) / FTuning.ROLLING_SLICK);
        return FTuning.ROLLING_SOFT / (1 + m.hardness().value() / FTuning.ROLLING_HARDNESS) * slick;
    }

    /**
     * The ground's share: its material's deformation, more if it is loose (it falls, in vanilla),
     * and more again if a walker sinks in it (vanilla's speed factor). Asked by properties, never
     * by which block; cached per block, since the material table does not change mid-drive.
     */
    private double groundRolling(BlockState ground) {
        return groundRolling.computeIfAbsent(ground.getBlock(), block -> {
            double c = Material.find(MaterialTable.get().materials(), block.asItem())
                    .map(m -> deforms(m.material()))
                    .orElse((double) FTuning.ROLLING_UNKNOWN_GROUND);
            if (block instanceof FallingBlock)
                c += FTuning.ROLLING_LOOSE;
            float speed = block.getSpeedFactor();
            if (speed > 0 && speed < 1)
                c += FTuning.ROLLING_SINK * (1 / speed - 1);
            return c;
        });
    }

    /** Offroad's floor under grip, so a slick block still holds a little. */
    private static double fudge(double friction) {
        return friction < 1 ? 0.1 + 0.9 * friction : friction;
    }

    /** Each tube a coupled chain turns, and that chain's spin, rad/s. */
    public Map<Long, Float> tubeSpin() {
        Map<Long, Float> out = new HashMap<>();
        for (Coupling c : coupled)
            c.weld.tubes.forEach((t, sense) -> out.putIfAbsent(t, (float) (c.omega * sense)));
        return out;
    }

    /** Each tube a coupled chain turns, and what that chain takes from its network, Su. */
    public Map<Long, Float> tubeSu() {
        Map<Long, Float> out = new HashMap<>();
        for (Coupling c : coupled)
            for (long t : c.weld.tubes.keySet())
                out.putIfAbsent(t, c.su);
        return out;
    }

    /** Each hub's spin, rad/s, as of its last physics step. */
    public Map<Integer, Float> hubSpin() {
        return Map.copyOf(hubSpin);
    }

    /**
     * What the drivetrain is doing, figure by figure, for the debug helmet: why a tube that ought
     * to drive does not was the question ten hours of playtesting could not answer (2026-10-16).
     */
    public List<String> status() {
        List<String> out = new ArrayList<>();
        if (welds.isEmpty())
            out.add("Drive: no tube welded on a turning block's axis");
        else if (coupled.isEmpty())
            out.add("Drive: " + welds.size() + " weld(s), none on a turning block with a network");
        else
            for (Coupling c : coupled)
                out.add(String.format(java.util.Locale.ROOT, "Drive: coupled at %s, %.1f RPM, %.2f Su, drag %.3f Su/RPM",
                        c.weld.offset.toShortString(), c.omega * RPM_PER_RAD_S, c.su, c.weld.drag));
        for (Hub h : hubs) {
            boolean driven = drivenBy(h.vertex) != null;
            out.add(String.format(java.util.Locale.ROOT, "Tire %d: %s, %.0f RPM, load %.2f kN, grip %.2f, roll %.4f",
                    h.vertex, driven ? "driven" : "free", hubSpin.getOrDefault(h.vertex, 0f) * RPM_PER_RAD_S,
                    hubLoad.getOrDefault(h.vertex, 0f), h.friction, h.rolling));
        }
        if (hubs.isEmpty())
            out.add("No tires");
        return out;
    }

    /** What the coupled chain reports as drag, Su/RPM in the coupling node's frame; 0 when nothing couples. For tests. */
    public float coupledDrag() {
        return coupled.isEmpty() ? 0 : coupled.get(0).weld.drag;
    }
}
