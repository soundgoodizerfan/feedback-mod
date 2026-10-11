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

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Places, welds, moves, splits and removes vertices ({@code feedback_vehicles_spec.md} section 4).
 *
 * <ul>
 *   <li><b>Use on nothing in particular</b>: a vertex at the block face you are looking at, or --
 *       in mid air -- at the same distance as the vertex you hold, snapped to the 1/16 grid. If a
 *       vertex was selected, the new one is welded to it, so a chassis is drawn as a chain.</li>
 *   <li><b>Use on a vertex</b>: select it; with another selected, weld or cut the edge between
 *       them. Use on the <em>selected</em> vertex picks it up.</li>
 *   <li><b>While carrying</b>: use anywhere puts it down there (same placement rule as a new
 *       vertex). Its edges and plates follow it.</li>
 *   <li><b>Use on the middle of an edge</b>: a new vertex at the edge's midpoint, splitting the edge
 *       and its plates, already picked up so the next use moves it.</li>
 *   <li><b>Use on a plate, or a closed triangle with none, holding a block in the off hand</b>:
 *       re-plates it in that block -- a new material, or a missing plate laid.</li>
 *   <li><b>With a vertex selected, use on a vertex of another frame</b>: joins the two frames
 *       there (section 4a) -- a weld, or a bearing if one is in the off hand. Again cuts it.</li>
 *   <li><b>Sneak-use</b>: on a vertex, removes it and everything welded to it; on a plate,
 *       removes the plate and keeps its edges; anywhere else, lets go.</li>
 * </ul>
 *
 * <p>All of it works the same on a chassis that is already a moving vehicle: the look ray is
 * carried into the body's own frame, and every edit re-cuts its collision cells on the spot
 * ({@link FrameAssembly#recut}).
 *
 * <p>When a weld closes a triangle, a plate is laid on it in whatever block is in the off hand,
 * and one of that block is used up. No block in the off hand, no plate -- the edges stay a bare
 * frame. The material is recorded and its properties are read from the block; nothing here ever
 * asks which block it is.
 *
 * <p>The interaction shape is the Data Connector's -- select, then select again -- with a
 * raycast of its own, because vertices hang in the air where vanilla's block hit cannot see them.
 * What is held lives on the stack ({@link WelderSelection}), so the client can draw it.
 */
public class WelderItem extends Item {

    /** How close the look ray must pass to a vertex or edge to touch it, in blocks. */
    private static final double PICK_RADIUS = 0.12;
    /** How far from the player a frame block is looked for. */
    private static final int SEARCH = 9;

    public WelderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.success(stack);

        WelderSelection held = stack.get(FVehicles.WELDER_SELECTION.get());
        if (held != null && !held.grabbed() && !player.isShiftKeyDown()
                && level.getBlockEntity(held.frame()) instanceof FrameBlockEntity from
                && from.frame().vertices().containsKey(held.vertex())) {
            for (Aim other : aims(level, player)) {
                if (other.be == from)
                    continue;
                int hit = pick(other.be.frame(), Vec3.atLowerCornerOf(other.be.getBlockPos()), other.eye, other.look,
                        player.blockInteractionRange());
                if (hit < 0)
                    continue;
                join(other.be, hit, from, held.vertex(), other.look, player);
                stack.remove(FVehicles.WELDER_SELECTION.get());
                return InteractionResultHolder.consume(stack);
            }
        }

        Aim aim = aim(level, player);
        if (aim == null) {
            player.displayClientMessage(Component.translatable("message.feedback.welder.no_frame"), true);
            return InteractionResultHolder.consume(stack);
        }
        FrameBlockEntity be = aim.be;
        Frame frame = be.frame();
        BlockPos at = be.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(at);
        Vec3 eye = aim.eye;
        Vec3 look = aim.look;
        double reach = player.blockInteractionRange();

        WelderSelection sel = stack.get(FVehicles.WELDER_SELECTION.get());
        if (sel != null && (!sel.frame().equals(at) || !frame.vertices().containsKey(sel.vertex())))
            sel = null;

        int hit = pick(frame, origin, eye, look, reach);

        if (player.isShiftKeyDown()) {
            int[] face = hit < 0 ? pickFace(frame, origin, eye, look, reach) : null;
            if (hit >= 0) {
                frame.remove(hit);
                be.changed();
                if (sel != null && sel.vertex() == hit)
                    sel = null;
            } else if (face != null && frame.removePlate(face[0], face[1], face[2])) {
                be.changed();
            } else {
                sel = null;
            }
        } else if (sel != null && sel.grabbed()) {
            if (hit != sel.vertex()) {
                Frame.Vertex to = Frame.Vertex.snap(target(level, player, frame, origin, eye, look, reach, sel.vertex()).subtract(origin));
                if (!frame.move(sel.vertex(), to)) {
                    player.displayClientMessage(Component.translatable("message.feedback.welder.cannot_move"), true);
                    return InteractionResultHolder.consume(stack);
                }
                be.changed();
            }
            sel = new WelderSelection(at, sel.vertex(), false);
        } else if (hit >= 0) {
            if (sel == null)
                sel = new WelderSelection(at, hit, false);
            else if (sel.vertex() == hit)
                sel = new WelderSelection(at, hit, true);
            else {
                weld(frame, sel.vertex(), hit, player);
                be.changed();
                sel = new WelderSelection(at, hit, false);
            }
        } else {
            long edge = pickEdge(frame, origin, eye, look, reach);
            if (edge != NO_EDGE) {
                int mid = frame.split(Frame.edgeA(edge), Frame.edgeB(edge));
                if (mid < 0) {
                    player.displayClientMessage(Component.translatable("message.feedback.welder.too_short"), true);
                    return InteractionResultHolder.consume(stack);
                }
                be.changed();
                sel = new WelderSelection(at, mid, true);
            } else if (paint(frame, pickFace(frame, origin, eye, look, reach), player)) {
                be.changed();
            } else {
                Frame.Vertex v = Frame.Vertex.snap(target(level, player, frame, origin, eye, look, reach,
                        sel != null ? sel.vertex() : -1).subtract(origin));
                if (!v.inRange()) {
                    player.displayClientMessage(Component.translatable("message.feedback.welder.out_of_range"), true);
                    return InteractionResultHolder.consume(stack);
                }
                int added = frame.add(v);
                if (sel != null && sel.vertex() != added)
                    weld(frame, sel.vertex(), added, player);
                be.changed();
                sel = new WelderSelection(at, added, false);
            }
        }

        if (sel == null)
            stack.remove(FVehicles.WELDER_SELECTION.get());
        else
            stack.set(FVehicles.WELDER_SELECTION.get(), sel);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Where a placed or carried vertex goes: the block face being looked at, or in mid air at the
     * same distance as vertex {@code depthOf} (two blocks if none).
     */
    private static Vec3 target(Level level, Player player, Frame frame, Vec3 origin, Vec3 eye, Vec3 look,
                               double reach, int depthOf) {
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() == HitResult.Type.BLOCK)
            return block.getLocation();
        double depth = depthOf >= 0 ? frame.pos(depthOf).add(origin).distanceTo(eye) : 2.0;
        return eye.add(look.scale(Math.min(depth, reach)));
    }

    /** Toggles edge {@code a-b}; if it now closes triangles, plates them in the off-hand block. */
    static void weld(Frame frame, int a, int b, Player player) {
        if (!frame.toggleEdge(a, b))
            return;
        for (int c : frame.closing(a, b)) {
            ItemStack off = player.getOffhandItem();
            if (!(off.getItem() instanceof BlockItem block))
                return;
            if (frame.addPlate(a, b, c, BuiltInRegistries.BLOCK.getKey(block.getBlock())) && !player.getAbilities().instabuild)
                off.shrink(1);
        }
    }

    /**
     * Joins two frames at a vertex of each ({@code feedback_vehicles_spec.md} section 4a), or cuts
     * the joint if they were already joined there -- a toggle, like an edge. A bearing in the off
     * hand makes it turn about the axis nearest the look; anything else welds it rigid.
     */
    private static void join(FrameBlockEntity to, int toVertex, FrameBlockEntity from, int fromVertex, Vec3 look, Player player) {
        if (to.unjoin(toVertex, from, fromVertex)) {
            to.changed();
            player.displayClientMessage(Component.translatable("message.feedback.welder.unjoined"), true);
            return;
        }
        boolean bearing = player.getOffhandItem().is(FVehicles.BEARINGS);
        Direction.Axis axis = Direction.getNearest(look.x, look.y, look.z).getAxis();
        if (bearing && !player.getAbilities().instabuild)
            player.getOffhandItem().shrink(1);
        to.join(toVertex, from, fromVertex, bearing ? FrameJoint.Kind.BEARING : FrameJoint.Kind.WELD, axis);
        player.displayClientMessage(Component.translatable(bearing ? "message.feedback.welder.bearing" : "message.feedback.welder.joined"), true);
    }

    /** The vertex nearest along the look ray, within {@link #PICK_RADIUS} of it; -1 if none. */
    static int pick(Frame frame, Vec3 origin, Vec3 eye, Vec3 look, double reach) {
        int best = -1;
        double bestT = Double.MAX_VALUE;
        for (Map.Entry<Integer, Frame.Vertex> e : frame.vertices().entrySet()) {
            Vec3 p = e.getValue().local().add(origin);
            double t = p.subtract(eye).dot(look);
            if (t < 0 || t > reach || t >= bestT)
                continue;
            if (p.distanceTo(eye.add(look.scale(t))) <= PICK_RADIUS) {
                best = e.getKey();
                bestT = t;
            }
        }
        return best;
    }

    /** The frame being worked on, and the look ray in that frame's own space. */
    private record Aim(FrameBlockEntity be, Vec3 eye, Vec3 look) {
    }

    /**
     * The nearest frame: one standing in the world, or one inside an assembled body, in which case
     * the eye and look are transformed into the body's local space so every pick and placement
     * below works unchanged on a vehicle that has moved or turned.
     */
    @Nullable
    private static Aim aim(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        Aim best = null;
        double bestDist = Double.MAX_VALUE;

        FrameBlockEntity world = nearestFrame(level, BlockPos.containing(eye));
        if (world != null) {
            best = new Aim(world, eye, look);
            bestDist = Vec3.atCenterOf(world.getBlockPos()).distanceToSqr(eye);
        }
        BoundingBox3d around = new BoundingBox3d(player.getBoundingBox().inflate(SEARCH));
        for (SubLevelAccess body : SableCompanion.INSTANCE.getAllIntersecting(level, around)) {
            Pose3dc pose = body.logicalPose();
            Vec3 localEye = pose.transformPositionInverse(eye);
            Vector3d l = pose.transformNormalInverse(new Vector3d(look.x, look.y, look.z), new Vector3d());
            FrameBlockEntity inBody = nearestFrame(level, BlockPos.containing(localEye));
            if (inBody == null)
                continue;
            double d = Vec3.atCenterOf(inBody.getBlockPos()).distanceToSqr(localEye);
            if (d < bestDist) {
                best = new Aim(inBody, localEye, new Vec3(l.x, l.y, l.z).normalize());
                bestDist = d;
            }
        }
        return best;
    }

    /** Every frame near the player, each with the look ray in its own space. */
    private static List<Aim> aims(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        List<Aim> out = new ArrayList<>();
        for (FrameBlockEntity be : frames(level, BlockPos.containing(eye)))
            out.add(new Aim(be, eye, look));
        BoundingBox3d around = new BoundingBox3d(player.getBoundingBox().inflate(SEARCH));
        for (SubLevelAccess body : SableCompanion.INSTANCE.getAllIntersecting(level, around)) {
            Pose3dc pose = body.logicalPose();
            Vec3 localEye = pose.transformPositionInverse(eye);
            Vector3d l = pose.transformNormalInverse(new Vector3d(look.x, look.y, look.z), new Vector3d());
            for (FrameBlockEntity be : frames(level, BlockPos.containing(localEye)))
                out.add(new Aim(be, localEye, new Vec3(l.x, l.y, l.z).normalize()));
        }
        return out;
    }

    private static List<FrameBlockEntity> frames(Level level, BlockPos around) {
        List<FrameBlockEntity> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-SEARCH, -SEARCH, -SEARCH), around.offset(SEARCH, SEARCH, SEARCH)))
            if (level.getBlockEntity(pos) instanceof FrameBlockEntity be)
                out.add(be);
        return out;
    }

    /** Re-plates triangle {@code face} in the off-hand block, using one up. False if nothing to do. */
    private static boolean paint(Frame frame, @Nullable int[] face, Player player) {
        if (face == null || !(player.getOffhandItem().getItem() instanceof BlockItem block))
            return false;
        ResourceLocation material = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        Frame.Plate existing = frame.plate(face[0], face[1], face[2]);
        if (existing != null && existing.material().equals(material))
            return false; // already that: let the click place a vertex instead
        if (!frame.addPlate(face[0], face[1], face[2], material))
            return false;
        if (!player.getAbilities().instabuild)
            player.getOffhandItem().shrink(1);
        return true;
    }

    /** The closed triangle the look ray meets first, plated or not; null if none. Moller-Trumbore. */
    @Nullable
    static int[] pickFace(Frame frame, Vec3 origin, Vec3 eye, Vec3 look, double reach) {
        int[] best = null;
        double bestT = reach;
        for (int[] tri : frame.triangles()) {
            Vec3 a = frame.pos(tri[0]).add(origin);
            Vec3 e1 = frame.pos(tri[1]).add(origin).subtract(a), e2 = frame.pos(tri[2]).add(origin).subtract(a);
            Vec3 p = look.cross(e2);
            double det = e1.dot(p);
            if (Math.abs(det) < 1e-12)
                continue;
            Vec3 t0 = eye.subtract(a);
            double u = t0.dot(p) / det;
            if (u < 0 || u > 1)
                continue;
            Vec3 q = t0.cross(e1);
            double v = look.dot(q) / det;
            if (v < 0 || u + v > 1)
                continue;
            double t = e2.dot(q) / det;
            if (t > 0 && t < bestT) {
                best = tri;
                bestT = t;
            }
        }
        return best;
    }

    static final long NO_EDGE = Long.MIN_VALUE;

    /** The edge nearest along the look ray, within {@link #PICK_RADIUS} of it; {@link #NO_EDGE} if none. */
    static long pickEdge(Frame frame, Vec3 origin, Vec3 eye, Vec3 look, double reach) {
        long best = NO_EDGE;
        double bestT = Double.MAX_VALUE;
        for (long key : frame.edgeKeys()) {
            Vec3 p = frame.pos(Frame.edgeA(key)).add(origin);
            Vec3 seg = frame.pos(Frame.edgeB(key)).add(origin).subtract(p);
            // Closest approach between the ray eye + t*look and the segment p + s*seg.
            Vec3 w = eye.subtract(p);
            double b = look.dot(seg), c = seg.dot(seg), d = look.dot(w), e = seg.dot(w);
            double denom = c - b * b; // look is unit length
            double s = denom < 1e-9 ? 0 : Math.max(0, Math.min(1, (e - b * d) / denom));
            double t = seg.scale(s).subtract(w).dot(look);
            if (t < 0 || t > reach || t >= bestT)
                continue;
            if (eye.add(look.scale(t)).distanceTo(p.add(seg.scale(s))) <= PICK_RADIUS) {
                best = key;
                bestT = t;
            }
        }
        return best;
    }

    @Nullable
    private static FrameBlockEntity nearestFrame(Level level, BlockPos around) {
        FrameBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-SEARCH, -SEARCH, -SEARCH), around.offset(SEARCH, SEARCH, SEARCH))) {
            if (level.getBlockEntity(pos) instanceof FrameBlockEntity be) {
                double d = pos.distSqr(around);
                if (d < bestDist) {
                    best = be;
                    bestDist = d;
                }
            }
        }
        return best;
    }
}
