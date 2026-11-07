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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
