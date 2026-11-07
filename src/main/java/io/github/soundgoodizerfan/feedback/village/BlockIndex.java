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
