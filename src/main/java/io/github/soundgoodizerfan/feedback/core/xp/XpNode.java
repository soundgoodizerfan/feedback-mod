// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /** Other nodes this one joins without touching them (a sculk relay's partners in range); none for a plain node. */
    default java.util.List<XpNode> bridges() {
        return java.util.List.of();
    }

    @Nullable
    XpNetwork network();

    void network(@Nullable XpNetwork network);

    default BlockEntity entity() {
        return (BlockEntity) this;
    }
}
