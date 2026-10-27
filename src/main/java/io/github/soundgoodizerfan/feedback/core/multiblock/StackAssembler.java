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
package io.github.soundgoodizerfan.feedback.core.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Mode B: flood a connected group of parts sharing one {@link RoleRule} and judge it against the rule. No controller. */
final class StackAssembler {

    private StackAssembler() {
    }

    /** Builds the structure around {@code seed}, claiming every part in the group. */
    static Structure assemble(Level level, Part seed, Function<net.minecraft.core.BlockPos, Part> partAt, Set<Part> seen) {
        RoleRule rule = seed.rule();
        List<Part> group = new ArrayList<>();
        ArrayDeque<Part> queue = new ArrayDeque<>();
        seen.add(seed);
        queue.add(seed);
        while (!queue.isEmpty()) {
            Part at = queue.poll();
            group.add(at);
            for (Direction d : Direction.values()) {
                if (!at.connects(d))
                    continue;
                Part n = partAt.apply(at.pos().relative(d));
                if (n != null && n.rule() == rule && n.connects(d.getOpposite()) && seen.add(n))
                    queue.add(n);
            }
        }
        Direction up = rule.up;
        // shortcut: ordered by height along `up` only, so a branching tree is judged as a flat sorted list; upgrade if a tree-shaped stack appears.
        group.sort(Comparator.<Part>comparingInt(p -> p.pos().getX() * up.getStepX() + p.pos().getY() * up.getStepY()
                + p.pos().getZ() * up.getStepZ()).thenComparingLong(p -> p.pos().asLong()));

        Structure s = rule.factory.get();
        s.members.addAll(group);
        s.flaws = rule.check(group.stream().map(Part::role).toList());
        s.formed = s.flaws.isEmpty();
        return s;
    }
}
