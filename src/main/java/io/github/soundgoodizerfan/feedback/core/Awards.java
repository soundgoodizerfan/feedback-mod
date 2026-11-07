// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Grants the code-awarded (joke) advancements, whose criterion is {@code minecraft:impossible}. */
public final class Awards {
    private static final double NEAR = 24;

    private Awards() {
    }

    /** Award {@code feedback:<path>} to {@code player}; a no-op if it is missing or already held. */
    public static void give(Player player, String path) {
        if (!(player instanceof ServerPlayer sp))
            return;
        var holder = sp.server.getAdvancements().get(Feedback.id(path));
        if (holder != null)
            sp.getAdvancements().award(holder, "got");
    }

    /** Machines have no owner: award every player within a screen's reach of {@code pos}. */
    public static void near(Level level, BlockPos pos, String path) {
        if (level instanceof ServerLevel server)
            for (ServerPlayer p : server.players())
                if (p.blockPosition().distSqr(pos) <= NEAR * NEAR)
                    give(p, path);
    }
}
