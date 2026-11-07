// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The loaded block entities of one kind in each level: runtime state only, never saved. A block entity adds itself on load and leaves on removal or unload. */
public final class BlockIndex<T extends BlockEntity> {

    private final WeakHashMap<Level, Set<T>> byLevel = new WeakHashMap<>();

    public void add(T be) {
        if (be.getLevel() != null && !be.getLevel().isClientSide)
            byLevel.computeIfAbsent(be.getLevel(), l -> new HashSet<>()).add(be);
    }

    public void remove(T be) {
        Set<T> set = be.getLevel() == null ? null : byLevel.get(be.getLevel());
        if (set != null)
            set.remove(be);
    }

    public Set<T> in(Level level) {
        Set<T> set = byLevel.get(level);
        return set == null ? Collections.emptySet() : set;
    }
}
