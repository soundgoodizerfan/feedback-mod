// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What the gizmo does on the server: moves the Welder's selection, or extrudes it, a whole number
 * of 1/16 cells along one axis -- and the same at every mirror image.
 */
public final class FrameEdit {

    private FrameEdit() {
    }

    /** Moves or extrudes the selection held in {@code welder} on {@code be}; false, and a message, if it cannot. */
    public static boolean apply(Player player, ItemStack welder, FrameBlockEntity be, int axis, int steps, boolean extrude) {
        WelderSelected chosen = WelderItem.selected(welder, be);
        if (chosen == null || steps == 0 || axis < 0 || axis > 2)
            return false;
        int[] d = {axis == 0 ? steps : 0, axis == 1 ? steps : 0, axis == 2 ? steps : 0};
        Frame frame = be.frame();
        List<Integer> copies = extrude ? extrude(player, frame, chosen, d) : null;
        if (extrude ? copies == null : !move(frame, chosen.vertices(), d)) {
            player.displayClientMessage(Component.translatable("message.feedback.welder.cannot_move"), true);
            return false;
        }
        if (extrude)
            welder.set(FVehicles.WELDER_SELECTED.get(), new WelderSelected(chosen.frame(), copies, chosen.clicked()));
        be.changed();
        return true;
    }

    private static Frame.Vertex shifted(Frame.Vertex v, int[] d) {
        return new Frame.Vertex(v.x() + d[0], v.y() + d[1], v.z() + d[2]);
    }

    /**
     * Every selected point moves by {@code d}, and every mirror image of one by the image of
     * {@code d}; a point on a plane, or an image that is itself selected, moves with the selection.
     * All or nothing: refused if any lands out of range, on a point that is not moving, or on
     * another that is moving (a selection and its image crossing a plane).
     */
    private static boolean move(Frame frame, List<Integer> selected, int[] d) {
        Map<Integer, Frame.Vertex> to = new LinkedHashMap<>();
        for (int v : selected)
            to.put(v, shifted(frame.vertices().get(v), d));
        for (Symmetry.Image img : Symmetry.images(frame)) {
            int[] md = img.apply(d[0], d[1], d[2]);
            for (int v : selected) {
                int m = img.of(frame, v, false);
                if (m >= 0 && !to.containsKey(m))
                    to.put(m, shifted(frame.vertices().get(m), md));
            }
        }
        Set<Frame.Vertex> landing = new HashSet<>();
        for (Map.Entry<Integer, Frame.Vertex> e : to.entrySet()) {
            if (!e.getValue().inRange() || !landing.add(e.getValue()))
                return false;
            int there = frame.idAt(e.getValue());
            if (there >= 0 && !to.containsKey(there))
                return false;
        }
        frame.vertices().putAll(to);
        return true;
    }

    /**
     * A copy of every selected point at {@code d}, joined to its original (a tube of the off-hand
     * block if one is held); where two selected points are joined, their copies are too, in the
     * same tube. The copies become the selection, so the next pull carries on from them.
     */
    @Nullable
    private static List<Integer> extrude(Player player, Frame frame, WelderSelected chosen, int[] d) {
        Map<Integer, Integer> copy = new HashMap<>();
        for (int v : chosen.vertices())
            if (!shifted(frame.vertices().get(v), d).inRange())
                return null;
        for (int v : chosen.vertices())
            copy.put(v, frame.add(shifted(frame.vertices().get(v), d)));
        List<long[]> joined = new ArrayList<>();
        for (int v : chosen.vertices()) {
            // connect toggles: a copy that landed on a point already joined to its original keeps that edge.
            if (!frame.hasEdge(v, copy.get(v)))
                WelderItem.connect(frame, v, copy.get(v), player);
            joined.add(new long[]{v, copy.get(v)});
        }
        for (long key : List.copyOf(frame.edgeKeys())) {
            int a = Frame.edgeA(key), b = Frame.edgeB(key);
            if (!copy.containsKey(a) || !copy.containsKey(b))
                continue;
            int ca = copy.get(a), cb = copy.get(b);
            if (frame.hasEdge(ca, cb))
                continue;
            frame.toggleEdge(ca, cb);
            Frame.Tube t = frame.tube(a, b);
            if (t != null && frame.setTube(ca, cb, t.material()))
                for (int i = 0; i < 4 && frame.tube(ca, cb).size() != t.size(); i++)
                    frame.thicken(ca, cb);
            joined.add(new long[]{ca, cb});
        }
        int made = 0;
        for (long[] e : joined)
            made += Symmetry.copyEdge(frame, (int) e[0], (int) e[1]);
        if (made > 0 && !player.getAbilities().instabuild && player.getOffhandItem().getItem() instanceof BlockItem)
            player.getOffhandItem().shrink(Math.min(made, player.getOffhandItem().getCount()));
        return List.copyOf(copy.values());
    }
}
