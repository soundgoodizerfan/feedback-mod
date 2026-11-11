// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Micro Core's building modes (vehicles v2 spec, <em>building tools</em>), kept in the core's own
 * data: how a block held in the other hand is placed. {@link Mode#SINGLE} puts one voxel; {@link Mode#LINE}
 * and {@link Mode#BOX} take two clicks, the first marking a corner and the second filling from it; and up to
 * three mirror planes, set in {@link Mode#MIRROR}, copy every voxel placed across each of them. Pure
 * geometry on voxel coordinates (a block is four on a side); the placer pays and puts.
 */
public final class MicroTool {

    public enum Mode { SINGLE, LINE, BOX, MIRROR }

    public static final int MAX_PLANES = 3;
    private static final String MODE = "MicroMode", CORNER = "MicroCorner", HAS_CORNER = "MicroHasCorner", PLANES = "MicroPlanes";

    private MicroTool() {
    }

    private static CompoundTag tag(ItemStack core) {
        return core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack core, CompoundTag tag) {
        core.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Mode mode(ItemStack core) {
        return Mode.values()[Math.floorMod(tag(core).getInt(MODE), Mode.values().length)];
    }

    /** Steps to the next mode and forgets a half-made corner. */
    public static Mode cycle(ItemStack core) {
        CompoundTag t = tag(core);
        Mode next = Mode.values()[(mode(core).ordinal() + 1) % Mode.values().length];
        t.putInt(MODE, next.ordinal());
        t.remove(HAS_CORNER);
        save(core, t);
        return next;
    }

    @org.jetbrains.annotations.Nullable
    public static BlockPos corner(ItemStack core) {
        CompoundTag t = tag(core);
        return t.getBoolean(HAS_CORNER) ? BlockPos.of(t.getLong(CORNER)) : null;
    }

    public static void setCorner(ItemStack core, @org.jetbrains.annotations.Nullable BlockPos corner) {
        CompoundTag t = tag(core);
        if (corner == null)
            t.remove(HAS_CORNER);
        else {
            t.putBoolean(HAS_CORNER, true);
            t.putLong(CORNER, corner.asLong());
        }
        save(core, t);
    }

    /** A mirror plane: normal to {@code axis}, at {@code at} half-voxels from the origin (even: between voxels, odd: through one). */
    public record Plane(Direction.Axis axis, int at) {
    }

    public static List<Plane> planes(ItemStack core) {
        List<Plane> out = new ArrayList<>();
        int[] raw = tag(core).getIntArray(PLANES);
        for (int i = 0; i + 1 < raw.length; i += 2)
            out.add(new Plane(Direction.Axis.values()[raw[i]], raw[i + 1]));
        return out;
    }

    /** Toggles the plane through the face {@code face} of voxel {@code v}; false when three are set already. */
    public static boolean togglePlane(ItemStack core, BlockPos v, Direction face) {
        List<Plane> now = new ArrayList<>(planes(core));
        int coord = switch (face.getAxis()) { case X -> v.getX(); case Y -> v.getY(); case Z -> v.getZ(); };
        // The face between this voxel and the next one along the face's direction, in half-voxels.
        int at = 2 * coord + 1 + face.getAxisDirection().getStep();
        Plane plane = new Plane(face.getAxis(), at);
        if (!now.remove(plane)) {
            if (now.size() >= MAX_PLANES)
                return false;
            now.add(plane);
        }
        int[] raw = new int[now.size() * 2];
        for (int i = 0; i < now.size(); i++) {
            raw[2 * i] = now.get(i).axis().ordinal();
            raw[2 * i + 1] = now.get(i).at();
        }
        CompoundTag t = tag(core);
        t.putIntArray(PLANES, raw);
        save(core, t);
        return true;
    }

    public static void clearPlanes(ItemStack core) {
        CompoundTag t = tag(core);
        t.remove(PLANES);
        save(core, t);
    }

    /** The voxels from {@code a} to {@code b}: a straight line along the longest axis, or the whole box. */
    public static Set<BlockPos> span(Mode mode, BlockPos a, BlockPos b) {
        Set<BlockPos> out = new LinkedHashSet<>();
        if (mode == Mode.BOX) {
            for (BlockPos p : BlockPos.betweenClosed(a, b))
                out.add(p.immutable());
            return out;
        }
        int dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), dz = b.getZ() - a.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        for (int i = 0; i <= steps; i++)
            out.add(new BlockPos(a.getX() + Math.round((float) dx * i / Math.max(steps, 1)),
                    a.getY() + Math.round((float) dy * i / Math.max(steps, 1)), a.getZ() + Math.round((float) dz * i / Math.max(steps, 1))));
        return out;
    }

    /**
     * Every voxel of {@code voxels} (each with the state it takes) and its images across each plane in
     * turn -- three planes make up to eight copies -- the states' Direction properties flipped along each
     * plane's axis, so a stair or a bearing facing out faces out on the other side.
     */
    public static java.util.Map<BlockPos, BlockState> mirrored(java.util.Map<BlockPos, BlockState> voxels, List<Plane> planes) {
        java.util.Map<BlockPos, BlockState> out = new java.util.LinkedHashMap<>(voxels);
        for (Plane plane : planes) {
            java.util.Map<BlockPos, BlockState> images = new java.util.LinkedHashMap<>();
            for (var e : out.entrySet())
                images.putIfAbsent(reflect(e.getKey(), plane), flip(e.getValue(), plane.axis()));
            for (var e : images.entrySet())
                out.putIfAbsent(e.getKey(), e.getValue());
        }
        return out;
    }

    public static BlockPos reflect(BlockPos v, Plane plane) {
        return switch (plane.axis()) {
            case X -> new BlockPos(plane.at() - v.getX() - 1, v.getY(), v.getZ());
            case Y -> new BlockPos(v.getX(), plane.at() - v.getY() - 1, v.getZ());
            case Z -> new BlockPos(v.getX(), v.getY(), plane.at() - v.getZ() - 1);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState flip(BlockState state, Direction.Axis axis) {
        for (Property<?> property : state.getProperties())
            if (state.getValue(property) instanceof Direction d && d.getAxis() == axis && property.getPossibleValues().contains(d.getOpposite()))
                state = state.setValue((Property) property, (Comparable) d.getOpposite());
        return state;
    }
}
