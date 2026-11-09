// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Fires the code-awarded advancements: each names its path as the {@code event} of a {@link EventTrigger} criterion. */
public final class Awards {
    private static final double NEAR = 24;

    private Awards() {
    }

    /** Fire event {@code path} at {@code player}; the advancement that names it is awarded. */
    public static void give(Player player, String path) {
        EventTrigger.fire(player, path);
    }

    /** Machines have no owner: award every player within a screen's reach of {@code pos}. */
    public static void near(Level level, BlockPos pos, String path) {
        if (level instanceof ServerLevel server)
            for (ServerPlayer p : server.players())
                if (p.blockPosition().distSqr(pos) <= NEAR * NEAR)
                    give(p, path);
    }
}
