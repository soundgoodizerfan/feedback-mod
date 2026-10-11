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
package io.github.soundgoodizerfan.feedback.machine.pulley;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Select-then-connect, server-side -- the same shape {@code control.data.DataLinkManager} already
 * uses for the data connector, applied to a belt instead. No cable entity, no in-world belt block;
 * a belt is two pulleys that know each other's position, exactly the way a data link is two nodes
 * that know each other's reference.
 */
public final class BeltLinkManager {

    private static final BeltLinkManager INSTANCE = new BeltLinkManager();

    public static BeltLinkManager get() {
        return INSTANCE;
    }

    private final Map<UUID, GlobalPos> pending = new HashMap<>();

    private BeltLinkManager() {
    }

    /** Called server-side when a player right-clicks a pulley with a Belt in hand. */
    public void onPulleyInteract(Player player, Level level, PulleyBlockEntity pulley) {
        UUID id = player.getUUID();
        GlobalPos previous = pending.remove(id);
        GlobalPos here = GlobalPos.of(level.dimension(), pulley.getBlockPos());

        if (previous == null || previous.equals(here)) {
            pending.put(id, here);
            player.displayClientMessage(Component.translatable("feedback.belt.selected"), true);
            return;
        }

        // A selection made in another dimension would otherwise resolve at the same coordinates here.
        boolean linked = previous.dimension() == level.dimension() && level.isLoaded(previous.pos())
                && level.getBlockEntity(previous.pos()) instanceof PulleyBlockEntity from
                && from.linkBelt(pulley);
        player.displayClientMessage(Component.translatable(
                linked ? "feedback.belt.linked" : "feedback.belt.failed"), true);
    }

    /** Drops a player's half-made belt so it cannot outlive the session. */
    public void clear(Player player) {
        pending.remove(player.getUUID());
    }
}
