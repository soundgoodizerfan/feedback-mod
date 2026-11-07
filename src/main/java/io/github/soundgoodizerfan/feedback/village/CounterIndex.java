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
package io.github.soundgoodizerfan.feedback.village;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.world.level.Level;

/**
 * The loaded Counters of each level, runtime state only and never saved. It lets a Counter see whether a nearer stocked Counter serves the
 * same villager, so competition between shops is not a rule but falls out of "nearest counter with stock wins".
 */
public final class CounterIndex {

    private static final WeakHashMap<Level, Set<CounterBlockEntity>> BY_LEVEL = new WeakHashMap<>();

    private CounterIndex() {
    }

    static void add(CounterBlockEntity c) {
        if (c.getLevel() != null && !c.getLevel().isClientSide)
            BY_LEVEL.computeIfAbsent(c.getLevel(), l -> new HashSet<>()).add(c);
    }

    static void remove(CounterBlockEntity c) {
        Set<CounterBlockEntity> set = c.getLevel() == null ? null : BY_LEVEL.get(c.getLevel());
        if (set != null)
            set.remove(c);
    }

    /** The loaded Counters of {@code level}. */
    public static Set<CounterBlockEntity> in(Level level) {
        Set<CounterBlockEntity> set = BY_LEVEL.get(level);
        return set == null ? Collections.emptySet() : set;
    }
}
