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
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * One vehicle as the player sees it, though Sable holds it as several bodies: a frame's body, the
 * body of each part swinging on its bearings, and every other frame joined to it by a
 * frame-to-frame joint, with theirs. The Lift and the Lift item act on all of it -- before, they
 * took the one body clicked, and an arm was picked up alone or left floating (playtest 2026-10-16).
 */
public final class Superobject {

    private Superobject() {
    }

    /** Every body joined to {@code start}, {@code start} included. */
    public static Set<ServerSubLevel> of(ServerLevel level, ServerSubLevel start) {
        var container = ServerSubLevelContainer.getContainer(level);
        List<FrameBlockEntity> frames = FrameBlockEntity.loadedOn(level);
        Set<ServerSubLevel> found = new LinkedHashSet<>();
        ArrayDeque<ServerSubLevel> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            ServerSubLevel body = todo.pop();
            if (!found.add(body))
                continue;
            for (FrameBlockEntity f : frames) {
                boolean in = SableCompanion.INSTANCE.getContaining(f) == body
                        || f.groupBodies().values().stream().anyMatch(g -> g.body().equals(body.getUniqueId()));
                if (!in)
                    continue;
                add(todo, SableCompanion.INSTANCE.getContaining(f));
                f.groupBodies().values().forEach(g -> add(todo, container.getSubLevel(g.body())));
                for (FrameBlockEntity far : f.partners()) {
                    add(todo, SableCompanion.INSTANCE.getContaining(far));
                    far.groupBodies().values().forEach(g -> add(todo, container.getSubLevel(g.body())));
                }
            }
        }
        return found;
    }

    /** The bodies of {@code all} that are not some frame's swinging part: the ones holding real blocks. */
    public static List<ServerSubLevel> roots(ServerLevel level, Set<ServerSubLevel> all) {
        Set<java.util.UUID> parts = new java.util.HashSet<>();
        for (FrameBlockEntity f : FrameBlockEntity.loadedOn(level))
            f.groupBodies().values().forEach(g -> parts.add(g.body()));
        return all.stream().filter(b -> !parts.contains(b.getUniqueId())).toList();
    }

    private static void add(ArrayDeque<ServerSubLevel> todo, Object body) {
        if (body instanceof ServerSubLevel s && !s.isRemoved())
            todo.add(s);
    }
}
