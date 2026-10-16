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
    private record Weld(BlockPos offset, Direction face, Map<Integer, Integer> driven, float drag, float inertia, Set<Long> tubes) {}

    /** A tire on a hinge, fixed until the next edit. {@code local} and {@code axis} are frame space. */
    private record Hub(int vertex, Vec3 local, Vec3 axis, double radius, double friction, double rolling, float inertia, boolean anchored) {}

    private final List<Weld> welds = new ArrayList<>();
    private final List<Hub> hubs = new ArrayList<>();
    /** The frame's {@code Vdrag} seen along +x, -x, +y, -y, +z, -z of the body. */
    private final float[] vdrag = new float[6];

    /** The weld coupling this game tick, and the node behind it; null when nothing couples. */
    @Nullable
    private Weld coupled;
    /** The coupling shaft's spin about the face the tube leaves by, rad/s, as of the last game tick. */
    private double omegaOut;

    // Summed over the physics steps since the last game tick, then reported and cleared.
    private double torqueTimesDt;
    private double slipTimesDt;
    private double groundedTime;
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
            for (long key : frame.tubesAt(vertex)) {
                Frame.Vertex o = frame.vertices().get(other(key, vertex));
                Direction face = Direction.fromDelta(Integer.signum(o.x() - v.x()),
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
        record Step(long tube, int from) {}
        ArrayDeque<Step> todo = new ArrayDeque<>(List.of(new Step(first, start)));
        while (!todo.isEmpty()) {
            Step s = todo.pop();
            int at = other(s.tube, s.from);
            Vec3 along = frame.pos(at).subtract(frame.pos(s.from)).normalize();
            Frame.Pivot pivot = frame.pivots().get(at);
            boolean drives = pivot != null && pivot.drives();
            if (frame.tires().containsKey(at)) {
                double cos = along.dot(pivot.axis().normalize());
                if (drives && Math.abs(cos) > MIN_JOINT_COS) {
                    driven.put(at, (int) Math.signum(cos));
                    drag += FTuning.SHAFT_DRAG_SU_PER_RPM.suPerRpm();
                }
                continue;
            }
            if (drives)
                drag += FTuning.SHAFT_DRAG_SU_PER_RPM.suPerRpm();
            for (long next : frame.tubesAt(at)) {
                if (seen.contains(next))
                    continue;
                Vec3 out = frame.pos(other(next, at)).subtract(frame.pos(at)).normalize();
                if (drives || out.dot(along) > 0.999) {
                    seen.add(next);
                    inertia += tubeInertia(frame, next);
                    todo.push(new Step(next, at));
                }
            }
        }
        return new Weld(offset, face, driven, drag, inertia, seen);
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
        double torque = stepTime > 0 ? torqueTimesDt / stepTime : 0;
        double slip = groundedTime > 0 ? slipTimesDt / groundedTime : 0;
        torqueTimesDt = slipTimesDt = groundedTime = stepTime = 0;
        coupled = null;
        for (Weld w : welds) {
            BlockPos at = framePos.offset(w.offset);
            BlockState state = level.getBlockState(at);
            // Either end of anything turning couples -- an engine's back as well as its output, a
            // cog's face -- not only a face with a shaft on it: a tube welded on the axis turns with
            // the block (author, 2026-10-16). A tube off the side would be a crank, so not that.
            if (!(state.getBlock() instanceof Rotatable shaft)
                    || !(shaft.hasShaftTowards(level, at, state, w.face) || shaft.getRotationAxis(state) == w.face.getAxis())
                    || !(level.getBlockEntity(at) instanceof RotationNode node) || node.getNetwork() == null)
                continue;
            coupled = w;
            // A node's speed is about its axis's positive end; the tube leaves by w.face.
            float sense = shaft.shaftSignTowards(state, w.face)
                    * (w.face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
            omegaOut = node.getRpm().value() * sense / RPM_PER_RAD_S;
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
            return;
        }
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
                Integer airSense = coupled == null ? null : coupled.driven.get(hub.vertex);
                if (airSense != null)
                    hubSpin.put(hub.vertex, (float) (omegaOut * airSense));
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
            double load = (share * omegaN * omegaN * compression - damping) * bearing;
            hubLoad.put(hub.vertex, (float) Math.max(load, 0));
            if (load <= 0)
                continue;
            double grip = hub.friction * fudge(PhysicsBlockPropertyHelper.getFriction(ground));

            // The tread's own speed at the contact, from the hub's spin about its axle.
            Integer sense = coupled == null ? null : coupled.driven.get(hub.vertex);
            Vector3d tread = axis.cross(lever, new Vector3d());
            double treadLength = tread.length();
            Vector3d slip;
            double omega = treadLength > 1e-9 ? -v.dot(tread) / (treadLength * treadLength) : 0; // rolling with the road
            if (sense != null) {
                omega = omegaOut * sense;
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
                torqueTimesDt += onWheel * sense * Units.SU_PER_KPG_TORQUE * dt;
                double rolling = -v.dot(tread) / (treadLength * treadLength);
                slipTimesDt += (omega - rolling) * sense * dt;
                groundedTime += dt;
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

    /** The coupled chain's own spin, rad/s; 0 when nothing couples. */
    public float chainSpin() {
        return coupled == null ? 0 : (float) omegaOut;
    }

    /** The tubes the coupled chain turns; empty when nothing couples. */
    public Set<Long> chainTubes() {
        return coupled == null ? Set.of() : coupled.tubes;
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
        else if (coupled == null)
            out.add("Drive: " + welds.size() + " weld(s), none on a turning block with a network");
        else
            out.add(String.format(java.util.Locale.ROOT, "Drive: coupled at %s, chain %.1f RPM, drag %.3f Su/RPM",
                    coupled.offset.toShortString(), omegaOut * RPM_PER_RAD_S, coupled.drag));
        for (Hub h : hubs) {
            boolean driven = coupled != null && coupled.driven.containsKey(h.vertex);
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
        return coupled == null ? 0 : coupled.drag;
    }
}
