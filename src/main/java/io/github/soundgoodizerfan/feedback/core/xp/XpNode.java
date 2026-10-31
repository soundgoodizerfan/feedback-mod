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
package io.github.soundgoodizerfan.feedback.core.xp;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A block entity on an XP network (progression roadmap L2): a wire, a holder with a container seated, or a creative
 * source. Adjacent nodes join one {@link XpNetwork}; the network reads and writes the stores through this.
 */
public interface XpNode {

    /** The container store this node holds, or null if it holds none (a wire, an empty holder). */
    @Nullable
    default XpStore xpStore() {
        return null;
    }

    /** Replaces the held store with {@code store}, the same container after the network moved points. */
    default void xpSet(XpStore store) {
    }

    /** Points per tick this node moves per level of difference; a node that is not a wire does not limit the run. */
    default float conductance() {
        return Float.MAX_VALUE;
    }

    /** The share of what moves through this node that escapes as orbs. */
    default float leak() {
        return 0f;
    }

    /** A level this node holds the whole run to at no cost (a creative source), NaN for none. */
    default float pinnedLevel() {
        return Float.NaN;
    }

    @Nullable
    XpNetwork network();

    void network(@Nullable XpNetwork network);

    default BlockEntity entity() {
        return (BlockEntity) this;
    }
}
