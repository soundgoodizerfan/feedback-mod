// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * A taut line that catches on block edges: the line is a polyline from its anchor through a list of bend points to the held body.
 * Each tick a bend is dropped when the point before it sees the body again, and a new one is laid on the nearest edge when
 * something stands in the way. The pendulum then hangs from the last bend with the line that is left.
 *
 * <p>Idea from yyon's Grappling Hook Mod (GPL-3.0, {@code SegmentHandler}): keep the line as a list of bends, hang the swing
 * from the nearest one. Taken: the bend list and the shortened pendulum. Not taken: its recursive bend-splitting and its
 * "should not happen" fallbacks; here a bend lands on the box edge closest to the blocked segment and the next tick
 * re-tests the rest. Not taken either: its auto-attach when the bent line runs out, which a physical line does not do.
 */
public final class RopeWrap {

    private RopeWrap() {
    }

    /** Most bends laid in one tick; a corner cluster takes a couple of ticks to wrap, which reads as the line settling. */
    private static final int PER_TICK = 3;

    /**
     * Updates {@code bends} (anchor outward) for a line from {@code anchor} to {@code end}, in place.
     *
     * @return whether the list changed
     */
    public static boolean update(Level level, Entity context, Vec3 anchor, List<Vec3> bends, Vec3 end) {
        boolean changed = false;
        while (!bends.isEmpty()) {
            Vec3 prev = bends.size() >= 2 ? bends.get(bends.size() - 2) : anchor;
            // A line still running through the sheave it turns on stays turned: the sheave is not an edge it can slip off.
            if (!clear(level, context, prev, end) || onSheave(level, bends.get(bends.size() - 1)) && passes(prev, end, bends.get(bends.size() - 1)))
                break;
            bends.remove(bends.size() - 1);
            changed = true;
        }
        for (int i = 0; i < PER_TICK && bends.size() < ModularTuning.GRAPPLE_MAX_BENDS; i++) {
            Vec3 last = bends.isEmpty() ? anchor : bends.get(bends.size() - 1);
            Vec3 sheave = sheaveBetween(level, last, end);
            if (sheave != null) {
                bends.add(sheave);
                changed = true;
                continue;
            }
            BlockHitResult hit = level.clip(new ClipContext(last, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
            if (hit.getType() == HitResult.Type.MISS)
                break;
            Vec3 corner = bestEdgePoint(level, context, last, end, hit.getBlockPos());
            if (corner == null)
                break;
            bends.add(corner);
            changed = true;
        }
        return changed;
    }

    /** Whether the bend at {@code at} sits in a sheave block: a bend the line runs round without sawing. */
    public static boolean onSheave(Level level, Vec3 at) {
        return level.getBlockState(BlockPos.containing(at)).is(SHEAVES);
    }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> SHEAVES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    io.github.soundgoodizerfan.feedback.Feedback.id("sheaves"));

    /** Whether the segment {@code a} to {@code b} passes within the reach of a sheave's wheel at {@code at}. */
    private static boolean passes(Vec3 a, Vec3 b, Vec3 at) {
        Vec3 d = b.subtract(a);
        double len2 = d.lengthSqr();
        double t = len2 < 1.0e-9 ? 0 : Math.max(0, Math.min(1, at.subtract(a).dot(d) / len2));
        return a.add(d.scale(t)).distanceToSqr(at) < 0.8 * 0.8;
    }

    /** The centre of the first sheave the segment {@code from} to {@code to} runs through that it is not already turned on, or null. */
    private static Vec3 sheaveBetween(Level level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.5)
            return null;
        int steps = (int) Math.ceil(len / 0.5);
        for (int k = 1; k < steps; k++) {
            Vec3 p = from.add(d.scale(k / (double) steps));
            BlockPos pos = BlockPos.containing(p);
            if (level.getBlockState(pos).is(SHEAVES)) {
                Vec3 centre = Vec3.atCenterOf(pos);
                if (centre.distanceToSqr(from) > 0.8 * 0.8)
                    return centre;
            }
        }
        return null;
    }

    /** Total length of the polyline from {@code anchor} through {@code bends} to {@code end}. */
    public static double length(Vec3 anchor, List<Vec3> bends, Vec3 end) {
        return fixedLength(anchor, bends) + (bends.isEmpty() ? anchor : bends.get(bends.size() - 1)).distanceTo(end);
    }

    /** Length of the part of the line that is fixed: anchor to the last bend. */
    public static double fixedLength(Vec3 anchor, List<Vec3> bends) {
        double sum = 0;
        Vec3 prev = anchor;
        for (Vec3 b : bends) {
            sum += prev.distanceTo(b);
            prev = b;
        }
        return sum;
    }

    private static boolean clear(Level level, Entity context, Vec3 a, Vec3 b) {
        return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context)).getType() == HitResult.Type.MISS;
    }

    /**
     * The point on an edge of the blocked block, lifted a little off it, where bending the line costs least; null when no edge
     * point is both in open air and visible from {@code from}.
     */
    private static Vec3 bestEdgePoint(Level level, Entity context, Vec3 from, Vec3 to, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty())
            return null;
        AABB box = shape.bounds().move(pos);
        Vec3 centre = box.getCenter();
        double[] xs = {box.minX, box.maxX}, ys = {box.minY, box.maxY}, zs = {box.minZ, box.maxZ};
        Vec3 best = null;
        double bestCost = Double.MAX_VALUE;
        List<Vec3[]> edges = new ArrayList<>(12);
        for (double y : ys)
            for (double z : zs)
                edges.add(new Vec3[] {new Vec3(box.minX, y, z), new Vec3(box.maxX, y, z)});
        for (double x : xs)
            for (double z : zs)
                edges.add(new Vec3[] {new Vec3(x, box.minY, z), new Vec3(x, box.maxY, z)});
        for (double x : xs)
            for (double y : ys)
                edges.add(new Vec3[] {new Vec3(x, y, box.minZ), new Vec3(x, y, box.maxZ)});
        for (Vec3[] e : edges) {
            Vec3 on = closestOnEdge(from, to, e[0], e[1]);
            Vec3 axis = e[1].subtract(e[0]).normalize();
            Vec3 out = on.subtract(centre);
            out = out.subtract(axis.scale(out.dot(axis)));
            if (out.lengthSqr() < 1.0e-9)
                continue;
            Vec3 point = on.add(out.normalize().scale(ModularTuning.GRAPPLE_BEND_CLEARANCE));
            if (point.distanceToSqr(from) < 0.01 || !level.getBlockState(BlockPos.containing(point)).getCollisionShape(level, BlockPos.containing(point)).isEmpty())
                continue;
            double cost = from.distanceTo(point) + point.distanceTo(to);
            if (cost < bestCost && clear(level, context, from, point)) {
                bestCost = cost;
                best = point;
            }
        }
        return best;
    }

    /** The point on edge {@code p0..p1} nearest the segment {@code a..b}, both clamped (axis-aligned edge, so the closed form is short). */
    private static Vec3 closestOnEdge(Vec3 a, Vec3 b, Vec3 p0, Vec3 p1) {
        Vec3 d1 = b.subtract(a), d2 = p1.subtract(p0), r = a.subtract(p0);
        double aa = d1.lengthSqr(), e = d2.lengthSqr(), f = d2.dot(r);
        double s, t;
        if (aa < 1.0e-9) {
            s = 0;
            t = clamp(f / e);
        } else {
            double c = d1.dot(r), bb = d1.dot(d2), denom = aa * e - bb * bb;
            s = denom > 1.0e-9 ? clamp((bb * f - c * e) / denom) : 0;
            t = (bb * s + f) / e;
            if (t < 0) {
                t = 0;
                s = clamp(-c / aa);
            } else if (t > 1) {
                t = 1;
                s = clamp((bb - c) / aa);
            }
        }
        return p0.add(d2.scale(t));
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    // --- the synced form: a flat list of doubles in an entity-data compound ---------------------

    private static final String KEY = "b";

    /** Bends as a tag, x y z in turn, for the hook's synced data. */
    public static net.minecraft.nbt.CompoundTag toTag(List<Vec3> bends) {
        ListTag list = new ListTag();
        for (Vec3 b : bends) {
            list.add(DoubleTag.valueOf(b.x));
            list.add(DoubleTag.valueOf(b.y));
            list.add(DoubleTag.valueOf(b.z));
        }
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put(KEY, list);
        return tag;
    }

    public static List<Vec3> fromTag(net.minecraft.nbt.CompoundTag tag) {
        ListTag list = tag.getList(KEY, Tag.TAG_DOUBLE);
        List<Vec3> out = new ArrayList<>(list.size() / 3);
        for (int i = 0; i + 2 < list.size(); i += 3)
            out.add(new Vec3(list.getDouble(i), list.getDouble(i + 1), list.getDouble(i + 2)));
        return out;
    }
}
