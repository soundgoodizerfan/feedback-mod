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
 * Places, welds, moves, splits and removes vertices ({@code docs/feedback_vehicles_spec.md} section 4).
 *
 * <p>Shift-scroll cycles the mode ({@link WelderMode}); what a plain use does depends on it.
 * Sneak-use lets go of what is held; only Remove mode removes.
 *
 * <ul>
 *   <li><b>Point</b>: use on nothing places a point at the block face you are looking at, or in
 *       mid air at the distance of the point you hold, in its own 1/16 cell. Use on a point selects
 *       it; again on the selected one picks it up, and the next use puts it down (its edges and
 *       plates follow). Use on the middle of an edge splits it with a carried point.</li>
 *   <li><b>Connect</b>: use on a point, then another, toggles the edge between them; with a block
 *       in the off hand a new edge is a tube of it (spec section 4a), and use on an edge with a
 *       block makes it thicker. With a point selected, use on a point of <em>another</em> frame
 *       joins the two with a weld, and on a joint already there cuts it. Turning a point or a
 *       joint into a bearing is the {@link PivotItem}'s job, not this tool's.</li>
 *   <li><b>Plate</b>: use on three points in turn (again on one un-chooses it); the third lays a
 *       plate in the off-hand block, adding any edge it lacks. Use on a closed triangle re-plates
 *       it.</li>
 *   <li><b>Weld</b>: use on a point welds it to the block it touches, which is what holds a frame
 *       to its vehicle (spec section 4); again unwelds it.</li>
 *   <li><b>Remove</b>: use on a point removes it and everything welded to it; on a plate, removes
 *       the plate and keeps its edges; on a tube, makes it a bare edge again; anywhere else, lets go.</li>
 * </ul>
 *
 * <p>All of it works the same on a chassis that is already a moving vehicle: the look ray is
 * carried into the body's own frame, and every edit re-cuts its collision cells on the spot
 * ({@link FrameAssembly#recut}).
 *
 * <p>A plate uses one of the off-hand block up and records it as its material; its properties are
 * read from the block, nothing here ever asks which block it is.
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
    /** Half a 1/16 cell, in blocks. */
    private static final double HALF_CELL = 0.5 / Frame.GRID;

    public WelderItem(Properties properties) {
        super(properties);
    }

    /**
     * Claims the click before the block under the crosshair gets it. Points hang in the air where
     * vanilla's block hit cannot see them, so a point in front of an engine used to send the click
     * to the engine -- switching it on and off -- and never to this tool (playtest 2026-10-16).
     * Near a frame, every click is the tool's.
     */
    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || WelderItem.aim(context.getLevel(), player) == null)
            return net.minecraft.world.InteractionResult.PASS;
        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Aim aim = aim(level, player);
            if (aim != null && !player.isShiftKeyDown()
                    && pick(aim.be.frame(), Vec3.atLowerCornerOf(aim.be.getBlockPos()), aim.eye, aim.look, player.blockInteractionRange()) < 0
                    && onFrameBlock(level, player, aim.be, aim.eye, aim.look, player.blockInteractionRange()))
                io.github.soundgoodizerfan.feedback.client.MirrorScreen.open(aim.be.getBlockPos(), aim.be.frame().mirror(), aim.be.frame().locked());
            return InteractionResultHolder.success(stack);
        }

        WelderSelection held = stack.get(FVehicles.WELDER_SELECTION.get());
        if (held != null && !held.grabbed() && stack.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT) == WelderMode.CONNECT
                && !player.isShiftKeyDown()
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

        WelderMode mode = stack.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT);
        WelderSelection sel = stack.get(FVehicles.WELDER_SELECTION.get());
        if (sel != null && (!sel.frame().equals(at) || !frame.vertices().containsKey(sel.vertex())))
            sel = null;
        WelderSelected chosen = selected(stack, be);

        int hit = pick(frame, origin, eye, look, reach);

        if (player.isShiftKeyDown()) {
            // Let go of everything held; removing is its own mode.
            sel = null;
            chosen = null;
            stack.remove(FVehicles.WELDER_PICKS.get());
            stack.remove(FVehicles.WELDER_CORNER.get());
        } else if (hit < 0 && onFrameBlock(level, player, be, eye, look, reach)) {
            // The frame block itself: the mirror screen, opened on the client; nothing here.
        } else if (mode == WelderMode.REMOVE) {
            int[] face = hit < 0 ? pickFace(frame, origin, eye, look, reach) : null;
            if (hit >= 0) {
                List<Integer> gone = chosen != null && chosen.vertices().contains(hit) ? chosen.vertices() : List.of(hit);
                for (int v : List.copyOf(gone)) {
                    Symmetry.removeImages(frame, v);
                    frame.remove(v);
                }
                chosen = null;
                be.changed();
                if (sel != null && gone.contains(sel.vertex()))
                    sel = null;
            } else if (face != null && frame.removePlate(face[0], face[1], face[2])) {
                Symmetry.copyPlate(frame, face[0], face[1], face[2]);
                be.changed();
            } else {
                long edge = pickEdge(frame, origin, eye, look, reach);
                if (removeTube(frame, edge)) {
                    Symmetry.copyEdge(frame, Frame.edgeA(edge), Frame.edgeB(edge));
                    be.changed();
                }
            }
        } else if (mode == WelderMode.WELD) {
            if (hit >= 0 && weld(level, player, be, hit, look)) {
                Symmetry.copyPoint(frame, hit);
                be.changed();
            }
        } else if (mode == WelderMode.PLATE) {
            WelderPicks picked = stack.get(FVehicles.WELDER_PICKS.get());
            List<Integer> picks = new ArrayList<>();
            if (picked != null && picked.frame().equals(at))
                for (int v : picked.vertices())
                    if (frame.vertices().containsKey(v))
                        picks.add(v);
            if (hit >= 0) {
                if (!picks.remove((Integer) hit))
                    picks.add(hit);
                if (picks.size() == 3) {
                    if (laidPlate(frame, picks, player)) {
                        pay(player, Symmetry.copyPlate(frame, picks.get(0), picks.get(1), picks.get(2)));
                        be.changed();
                        picks.clear();
                    } else if (FrameMaterial.plate(player.getOffhandItem()) == null) {
                        player.displayClientMessage(Component.translatable("message.feedback.welder.need_block"), true);
                        picks.remove(2);
                    }
                }
            } else {
                int[] face = pickFace(frame, origin, eye, look, reach);
                if (paint(frame, face, player)) {
                    Symmetry.copyPlate(frame, face[0], face[1], face[2]);
                    be.changed();
                }
            }
            if (picks.isEmpty())
                stack.remove(FVehicles.WELDER_PICKS.get());
            else
                stack.set(FVehicles.WELDER_PICKS.get(), new WelderPicks(at, picks));
        } else if (mode == WelderMode.CONNECT) {
            if (hit >= 0) {
                if (sel == null)
                    sel = new WelderSelection(at, hit, false);
                else if (sel.vertex() == hit)
                    sel = null;
                else {
                    connect(frame, sel.vertex(), hit, player);
                    pay(player, Symmetry.copyEdge(frame, sel.vertex(), hit));
                    be.changed();
                    sel = new WelderSelection(at, hit, false);
                }
            } else {
                long edge = pickEdge(frame, origin, eye, look, reach);
                if (edge != NO_EDGE && tube(frame, edge, player)) {
                    Symmetry.copyEdge(frame, Frame.edgeA(edge), Frame.edgeB(edge));
                    be.changed();
                }
            }
        } else if (hit >= 0) {
            // Point, Select, Extrude: a click on a point takes it into the selection, or out.
            chosen = chosen == null ? new WelderSelected(at, List.of(hit), true) : chosen.toggled(hit);
        } else if (mode == WelderMode.SELECT) {
            Frame.Vertex c = Frame.Vertex.snap(target(level, player, frame, origin, eye, look, reach,
                    chosen != null ? chosen.last() : -1).subtract(origin));
            WelderCorner first = stack.get(FVehicles.WELDER_CORNER.get());
            if (first == null || !first.frame().equals(at))
                stack.set(FVehicles.WELDER_CORNER.get(), new WelderCorner(at, c.x(), c.y(), c.z()));
            else {
                List<Integer> inside = new ArrayList<>(chosen == null ? List.of() : chosen.vertices());
                frame.vertices().forEach((id, v) -> {
                    if (between(v.x(), first.x(), c.x()) && between(v.y(), first.y(), c.y()) && between(v.z(), first.z(), c.z())
                            && !inside.contains(id))
                        inside.add(id);
                });
                chosen = inside.isEmpty() ? null : new WelderSelected(at, inside, false);
                stack.remove(FVehicles.WELDER_CORNER.get());
            }
        } else if (mode == WelderMode.POINT) {
            long edge = pickEdge(frame, origin, eye, look, reach);
            if (edge != NO_EDGE) {
                int mid = frame.split(Frame.edgeA(edge), Frame.edgeB(edge));
                if (mid < 0) {
                    player.displayClientMessage(Component.translatable("message.feedback.welder.too_short"), true);
                    return InteractionResultHolder.consume(stack);
                }
                Symmetry.copySplit(frame, Frame.edgeA(edge), Frame.edgeB(edge));
                be.changed();
                chosen = new WelderSelected(at, List.of(mid), true);
            } else {
                Frame.Vertex v = Frame.Vertex.snap(target(level, player, frame, origin, eye, look, reach,
                        chosen != null ? chosen.last() : -1).subtract(origin));
                if (!v.inRange()) {
                    player.displayClientMessage(Component.translatable("message.feedback.welder.out_of_range"), true);
                    return InteractionResultHolder.consume(stack);
                }
                int placed = frame.add(v);
                bearOnShaft(level, player, be, placed, eye, look, reach);
                Symmetry.copyPoint(frame, placed);
                chosen = new WelderSelected(at, List.of(placed), true);
                be.changed();
            }
        }

        if (sel == null)
            stack.remove(FVehicles.WELDER_SELECTION.get());
        else
            stack.set(FVehicles.WELDER_SELECTION.get(), sel);
        if (chosen == null || chosen.vertices().isEmpty())
            stack.remove(FVehicles.WELDER_SELECTED.get());
        else
            stack.set(FVehicles.WELDER_SELECTED.get(), chosen);
        return InteractionResultHolder.consume(stack);
    }

    /** The selection held, if it is on {@code be} and its points still exist. */
    @Nullable
    public static WelderSelected selected(ItemStack stack, FrameBlockEntity be) {
        WelderSelected s = stack.get(FVehicles.WELDER_SELECTED.get());
        if (s == null || !s.frame().equals(be.getBlockPos()))
            return null;
        List<Integer> live = s.vertices().stream().filter(be.frame().vertices()::containsKey).toList();
        return live.isEmpty() ? null : new WelderSelected(s.frame(), live, s.clicked());
    }

    private static boolean between(int v, int a, int b) {
        return v >= Math.min(a, b) && v <= Math.max(a, b);
    }

    /** Whether the look meets the frame block itself before anything else. */
    static boolean onFrameBlock(Level level, Player player, FrameBlockEntity be, Vec3 eye, Vec3 look, double reach) {
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(be.getBlockPos())
                && pickEdge(be.frame(), Vec3.atLowerCornerOf(be.getBlockPos()), eye, look, reach) == NO_EDGE;
    }

    /** Takes {@code n} more of the off-hand block, for parts the mirror made; never more than there is. */
    private static void pay(Player player, int n) {
        if (n > 0 && !player.getAbilities().instabuild)
            player.getOffhandItem().shrink(Math.min(n, player.getOffhandItem().getCount()));
    }

    /**
     * Where a placed or carried vertex goes: the block face being looked at, or in mid air at the
     * same distance as vertex {@code depthOf} (two blocks if none).
     */
    public static Vec3 target(Level level, Player player, Frame frame, Vec3 origin, Vec3 eye, Vec3 look,
                               double reach, int depthOf) {
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() == HitResult.Type.BLOCK) {
            // Half a cell out from the face, so the point always lands in the cell touching it:
            // snapping the face itself rounded down, which put points inside a block approached
            // from below, west or north and outside one approached the other ways (playtest #12).
            var n = block.getDirection().getNormal();
            return block.getLocation().add(n.getX() * HALF_CELL, n.getY() * HALF_CELL, n.getZ() * HALF_CELL);
        }
        double depth = depthOf >= 0 ? frame.pos(depthOf).add(origin).distanceTo(eye) : 2.0;
        return eye.add(look.scale(Math.min(depth, reach)));
    }

    /**
     * A point placed on the end of anything turning -- a shaft's end, an engine's output -- is what
     * a drive tube starts from, so it is made one: welded to that block, and a hinge along its axis
     * that carries rotation (author, 2026-10-16). Setting both by hand was three tools' worth of
     * clicks to get one tube turning.
     */
    private static void bearOnShaft(Level level, Player player, FrameBlockEntity be, int vertex, Vec3 eye, Vec3 look, double reach) {
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos pos = block.getBlockPos();
        var state = level.getBlockState(pos);
        Direction face = block.getDirection();
        if (!(state.getBlock() instanceof io.github.soundgoodizerfan.feedback.core.rotation.Rotatable turning)
                || !(turning.hasShaftTowards(level, pos, state, face) || turning.getRotationAxis(state) == face.getAxis())
                || SableCompanion.INSTANCE.getContaining(level, pos) != SableCompanion.INSTANCE.getContaining(be))
            return;
        Frame frame = be.frame();
        frame.setAnchor(vertex, pos.subtract(be.getBlockPos()));
        frame.setPivot(vertex, new Frame.Pivot(FrameJoint.Kind.HINGE,
                Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, face.getAxis()).getNormal()), true));
        player.displayClientMessage(Component.translatable("message.feedback.welder.on_shaft"), true);
    }

    /**
     * Welds point {@code vertex} to the block it touches, or unwelds it ({@code
     * docs/feedback_vehicles_spec.md} section 4). Only a welded point holds the frame to its vehicle.
     * One click on the point: the old way -- select it, then click the block -- had the point and
     * its tube sitting right where the block was clicked, so the click took the point or the tube
     * instead. Where the point touches two blocks, the one further along the look wins: you weld to
     * what is behind the point.
     */
    private static boolean weld(Level level, Player player, FrameBlockEntity be, int vertex, Vec3 look) {
        Frame frame = be.frame();
        Vec3 p = frame.pos(vertex).add(Vec3.atLowerCornerOf(be.getBlockPos()));
        BlockPos best = null;
        // A point sits in the middle of its 1/16 cell, so one touching a face is 1/32 off it: look a
        // whole cell out, or the touching block is never reached (playtest 2026-10-16).
        double bestAlong = -Double.MAX_VALUE, touch = 1.0 / Frame.GRID;
        for (BlockPos b : BlockPos.betweenClosed(BlockPos.containing(p.subtract(touch, touch, touch)),
                BlockPos.containing(p.add(touch, touch, touch)))) {
            // The block must share the frame's space: Sable's world also holds other bodies' blocks
            // at their plot positions, which once welded a frame to a point millions of blocks out.
            if (level.getBlockState(b).isAir()
                    || SableCompanion.INSTANCE.getContaining(level, b) != SableCompanion.INSTANCE.getContaining(be))
                continue;
            double along = Vec3.atCenterOf(b).subtract(p).dot(look);
            if (along > bestAlong) {
                bestAlong = along;
                best = b.immutable();
            }
        }
        BlockPos was = frame.anchors().get(vertex);
        if (best == null && was == null) {
            player.displayClientMessage(Component.translatable("message.feedback.welder.not_touching"), true);
            return false;
        }
        BlockPos offset = best == null ? null : best.subtract(be.getBlockPos());
        boolean unweld = offset == null || offset.equals(was);
        frame.setAnchor(vertex, unweld ? null : offset);
        player.displayClientMessage(Component.translatable(unweld ? "message.feedback.welder.unanchored" : "message.feedback.welder.anchored"), true);
        return true;
    }

    /**
     * Toggles edge {@code a-b}; a new one becomes a tube if a block is in the off hand. A new edge
     * from a hinge that carries rotation to a plain point makes that point one too, along the edge
     * (author, 2026-10-16): a drive line is built point by point, and each was a Pivot Setter
     * round trip. An existing bearing is never overwritten.
     */
    static void connect(Frame frame, int a, int b, Player player) {
        if (a == b || !frame.toggleEdge(a, b))
            return;
        if (frame.hasEdge(a, b)) {
            carryOn(frame, a, b);
            carryOn(frame, b, a);
        }
        ItemStack off = player.getOffhandItem();
        if (frame.hasEdge(a, b) && off.getItem() instanceof BlockItem block
                && frame.setTube(a, b, BuiltInRegistries.BLOCK.getKey(block.getBlock()))
                && !player.getAbilities().instabuild)
            off.shrink(1);
    }

    /** If {@code from} is a hinge carrying rotation and {@code to} is a plain point, {@code to} becomes one, along the edge. */
    private static void carryOn(Frame frame, int from, int to) {
        Frame.Pivot p = frame.pivots().get(from);
        if (p == null || p.kind() != FrameJoint.Kind.HINGE || !p.drives() || frame.pivots().containsKey(to))
            return;
        frame.setPivot(to, new Frame.Pivot(FrameJoint.Kind.HINGE, frame.pos(to).subtract(frame.pos(from)).normalize(), true));
    }

    /** Plates the three picked points, adding any edge they lack. False with no block in the off hand. */
    private static boolean laidPlate(Frame frame, List<Integer> v, Player player) {
        ResourceLocation material = FrameMaterial.plate(player.getOffhandItem());
        if (material == null)
            return false;
        int a = v.get(0), b = v.get(1), c = v.get(2);
        for (int[] e : new int[][]{{a, b}, {b, c}, {a, c}})
            if (!frame.hasEdge(e[0], e[1]))
                frame.toggleEdge(e[0], e[1]);
        if (!frame.addPlate(a, b, c, material))
            return false;
        if (!player.getAbilities().instabuild)
            player.getOffhandItem().shrink(1);
        return true;
    }

    /**
     * Joins two frames at a vertex of each ({@code docs/feedback_vehicles_spec.md} section 4a): a weld
     * if they are not joined yet, a cut if they are -- a toggle, like an edge. What the joint
     * lets move is the {@link PivotItem}'s business, not this tool's.
     */
    private static void join(FrameBlockEntity to, int toVertex, FrameBlockEntity from, int fromVertex, Vec3 look, Player player) {
        if (to.joint(toVertex, from, fromVertex) == null) {
            if (player.isShiftKeyDown())
                return;
            to.join(toVertex, from, fromVertex, FrameJoint.Kind.WELD, hingeAxis(to, toVertex, from, fromVertex, look));
            say(player, to.joint(toVertex, from, fromVertex));
        } else {
            to.unjoin(toVertex, from, fromVertex);
            to.changed();
            player.displayClientMessage(Component.translatable("message.feedback.welder.unjoined"), true);
        }
    }

    /**
     * A hinge's axis, in {@code to}'s space: along the tube at either joined vertex if there is
     * exactly one there -- a drive shaft on a bearing turns about itself -- else the x, y or z axis
     * nearest the look. A tube on {@code from} is carried across through both bodies' rotations.
     */
    public static Vec3 hingeAxis(FrameBlockEntity to, int toVertex, FrameBlockEntity from, int fromVertex, Vec3 look) {
        Vec3 own = tubeDirection(to.frame(), toVertex);
        if (own != null)
            return own;
        Vec3 theirs = tubeDirection(from.frame(), fromVertex);
        if (theirs != null) {
            Vector3d d = FrameBlockEntity.rotation(from).transform(new Vector3d(theirs.x, theirs.y, theirs.z));
            FrameBlockEntity.rotation(to).conjugate().transform(d);
            return new Vec3(d.x, d.y, d.z).normalize();
        }
        return nearestAxis(look);
    }

    /** The x, y or z axis, positive, nearest {@code look}. */
    static Vec3 nearestAxis(Vec3 look) {
        Direction nearest = Direction.getNearest(look.x, look.y, look.z);
        return Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, nearest.getAxis()).getNormal());
    }

    @Nullable
    static Vec3 tubeDirection(Frame frame, int vertex) {
        List<Long> tubes = frame.tubesAt(vertex);
        if (tubes.size() != 1)
            return null;
        long key = tubes.get(0);
        return frame.pos(Frame.edgeB(key)).subtract(frame.pos(Frame.edgeA(key))).normalize();
    }

    static void say(Player player, @Nullable FrameJoint j) {
        if (j == null)
            return;
        Component what = Component.translatable("message.feedback.joint." + j.kind().name().toLowerCase(java.util.Locale.ROOT),
                String.format("(%.2f, %.2f, %.2f)", j.axis().x, j.axis().y, j.axis().z));
        player.displayClientMessage(j.kind().bearing()
                ? Component.translatable(j.drives() ? "message.feedback.joint.drives" : "message.feedback.joint.free", what)
                : what, true);
    }

    /** The vertex nearest along the look ray, within {@link #PICK_RADIUS} of it; -1 if none. */
    public static int pick(Frame frame, Vec3 origin, Vec3 eye, Vec3 look, double reach) {
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
    public record Aim(FrameBlockEntity be, Vec3 eye, Vec3 look) {
    }

    /**
     * The nearest frame: one standing in the world, or one inside an assembled body, in which case
     * the eye and look are transformed into the body's local space so every pick and placement
     * below works unchanged on a vehicle that has moved or turned.
     */
    @Nullable
    public static Aim aim(Level level, Player player) {
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

    /**
     * Makes {@code edge} a tube of the off-hand block, or re-tubes it in a new one; on a tube
     * already that material, makes it one size thicker (thick wraps to thin). Uses one block up
     * either way. False with no block in hand, so the click splits the edge instead.
     */
    private static boolean tube(Frame frame, long edge, Player player) {
        ItemStack off = player.getOffhandItem();
        if (!(off.getItem() instanceof BlockItem block))
            return false;
        ResourceLocation material = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        int a = Frame.edgeA(edge), b = Frame.edgeB(edge);
        Frame.Tube was = frame.tube(a, b);
        boolean done = was != null && was.material().equals(material) ? frame.thicken(a, b) : frame.setTube(a, b, material);
        if (done && !player.getAbilities().instabuild)
            off.shrink(1);
        return done;
    }

    private static boolean removeTube(Frame frame, long edge) {
        return edge != NO_EDGE && frame.removeTube(Frame.edgeA(edge), Frame.edgeB(edge));
    }

    /** Re-plates triangle {@code face} in the off-hand block, using one up. False if nothing to do. */
    private static boolean paint(Frame frame, @Nullable int[] face, Player player) {
        ResourceLocation material = face == null ? null : FrameMaterial.plate(player.getOffhandItem());
        if (material == null)
            return false;
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
