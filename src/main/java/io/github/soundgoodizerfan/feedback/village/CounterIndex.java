// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
