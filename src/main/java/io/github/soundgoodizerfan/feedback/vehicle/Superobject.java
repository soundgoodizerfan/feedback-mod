// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.machine.bearing.Joint;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroBearings;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * One vehicle as the player sees it, though Sable holds it as several bodies: a body, and every body
 * hung on it, or on those, by a micro or full-size joint ({@link Joint}). The Lift and the Lift item
 * act on all of it -- before, they took the one body clicked, and an arm was picked up alone or left
 * floating (playtest 2026-10-16).
 */
public final class Superobject {

    private Superobject() {
    }

    /** A joint in a body's plot and the body it holds: {@code index} is the voxel in a micro cell, -1 for a full-size bearing. */
    public record Hold(BlockPos pos, int index, Joint joint, ServerSubLevel child) {
    }

    /** Every joint standing in {@code body}'s plot whose held body is loaded. */
    public static List<Hold> holds(ServerLevel level, ServerSubLevel body) {
        List<Hold> found = new ArrayList<>();
        var container = ServerSubLevelContainer.getContainer(level);
        var box = body.getPlot().getBoundingBox();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var be = level.getBlockEntity(p);
            if (be instanceof MicroCellBlockEntity cell && cell.holdsBearing()) {
                for (var e : cell.bearings().int2ObjectEntrySet())
                    if (container.getSubLevel(e.getValue().bodyId()) instanceof ServerSubLevel child && !child.isRemoved())
                        found.add(new Hold(p.immutable(), e.getIntKey(), e.getValue(), child));
            } else if (be instanceof RotaryBearingBlockEntity b && b.joint() != null
                    && container.getSubLevel(b.joint().bodyId()) instanceof ServerSubLevel child && !child.isRemoved())
                found.add(new Hold(p.immutable(), -1, b.joint(), child));
        }
        return found;
    }

    /** Every body joined to {@code start}, {@code start} included. */
    public static Set<ServerSubLevel> of(ServerLevel level, ServerSubLevel start) {
        var container = ServerSubLevelContainer.getContainer(level);
        // Joint edges both ways, once: parent to child, and child back to parent.
        Map<UUID, List<ServerSubLevel>> linked = new HashMap<>();
        for (ServerSubLevel s : container.getAllSubLevels())
            if (!s.isRemoved())
                for (Hold h : holds(level, s)) {
                    linked.computeIfAbsent(s.getUniqueId(), k -> new ArrayList<>()).add(h.child());
                    linked.computeIfAbsent(h.child().getUniqueId(), k -> new ArrayList<>()).add(s);
                }
        Set<ServerSubLevel> found = new LinkedHashSet<>();
        ArrayDeque<ServerSubLevel> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            ServerSubLevel body = todo.pop();
            if (!found.add(body))
                continue;
            linked.getOrDefault(body.getUniqueId(), List.of()).forEach(b -> add(todo, b));
        }
        return found;
    }

    /**
     * The bodies of {@code all} that no joint holds: the ones holding real blocks.
     */
    public static List<ServerSubLevel> roots(ServerLevel level, Set<ServerSubLevel> all) {
        Set<UUID> parts = new java.util.HashSet<>();
        for (ServerSubLevel s : all)
            for (Hold h : holds(level, s))
                parts.add(h.child().getUniqueId());
        return all.stream().filter(b -> !parts.contains(b.getUniqueId())).toList();
    }

    /**
     * Joint loops among {@code bodies}: the joints beyond the one-per-body a tree needs. A loop is
     * reported by the Lift, never refused (vehicles v2 spec).
     */
    public static int loops(ServerLevel level, Set<ServerSubLevel> bodies) {
        int joints = 0;
        for (ServerSubLevel s : bodies)
            joints += holds(level, s).size();
        return Math.max(0, joints - Math.max(0, bodies.size() - 1));
    }

    /**
     * Puts back every body hung on {@code root} by a joint, the outermost first, each at the nearest
     * quarter turn as a bearing put-back does. Returns null when none is left, or the message key of
     * the one that would not go ({@code blocked}, {@code not_loaded}, {@code loop}).
     */
    @Nullable
    public static String unwind(ServerLevel level, ServerSubLevel root) {
        int last = Integer.MAX_VALUE;
        for (int guard = 0; guard < 4096; guard++) {
            // The joints below the root only (never up to a parent, never across a frame).
            List<Hold> below = new ArrayList<>();
            Set<ServerSubLevel> seen = new java.util.HashSet<>(List.of(root));
            ArrayDeque<ServerSubLevel> todo = new ArrayDeque<>(List.of(root));
            while (!todo.isEmpty())
                for (Hold h : holds(level, todo.pop())) {
                    below.add(h);
                    if (seen.add(h.child()))
                        todo.add(h.child());
                }
            if (below.isEmpty())
                return null;
            // A pass that put nothing back would repeat for ever.
            if (below.size() >= last)
                return "blocked";
            last = below.size();
            Hold leaf = null;
            for (Hold h : below)
                if (holds(level, h.child()).isEmpty()) {
                    leaf = h;
                    break;
                }
            if (leaf == null)
                return "loop";
            String result = leaf.index() < 0
                    ? (level.getBlockEntity(leaf.pos()) instanceof RotaryBearingBlockEntity b ? b.toggle(level) : "not_loaded")
                    : MicroBearings.toggle(level, leaf.pos(), leaf.index());
            if (!result.equals("disassembled"))
                return result;
        }
        return "loop";
    }

    private static void add(ArrayDeque<ServerSubLevel> todo, Object body) {
        if (body instanceof ServerSubLevel s && !s.isRemoved())
            todo.add(s);
    }
}
