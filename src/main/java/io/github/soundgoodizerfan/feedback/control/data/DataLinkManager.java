// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Select-then-connect, server-side -- the same shape as MrCrayfish's Furniture Mod: Refurbished's
 * {@code LinkManager} (MIT): the first node the player interacts with is remembered per player,
 * the second one completes the link. No cable item, no in-world entity; the pending selection is
 * pure server memory and never persisted, which is correct -- it isn't world state, it's "what
 * was I about to do."
 * <p>
 * Left behind from the read: link-length limits and a "powerable zone" -- both are about power
 * delivery, which a data link doesn't do. {@link DataNode#getMaxNodeLinks()} is this mod's only
 * cap, and it is per-node fan-out, not link distance.
 */
public final class DataLinkManager {

    private static final DataLinkManager INSTANCE = new DataLinkManager();

    public static DataLinkManager get() {
        return INSTANCE;
    }

    /**
     * A {@link DataNodeRef} has no dimension -- every link is within one level by construction --
     * so a selection remembers the level it was made in and is only handed out back in that
     * level. Otherwise a node picked in the Nether would resolve at the same coordinates here.
     */
    private record Pending(ResourceKey<Level> dimension, DataNodeRef ref) {
    }

    private final Map<UUID, Pending> pending = new HashMap<>();

    private DataLinkManager() {
    }

    /** Called server-side when a player right-clicks a data node with the connector. */
    public void onNodeInteract(Player player, DataNode node) {
        UUID id = player.getUUID();
        Level level = node.getNodeLevel();
        DataNodeRef previous = Optional.ofNullable(pending.remove(id))
                .filter(p -> p.dimension() == level.dimension()).map(Pending::ref).orElse(null);

        if (previous == null || previous.equals(node.getNodeRef())) {
            pending.put(id, new Pending(level.dimension(), node.getNodeRef()));
            player.displayClientMessage(Component.translatable("feedback.data_connector.selected"), true);
            return;
        }

        Optional<DataNode> from = DataNode.resolve(level, previous);
        boolean linked = from.isPresent() && from.get().linkTo(node);
        player.displayClientMessage(Component.translatable(linked
                ? "feedback.data_connector.linked" : "feedback.data_connector.failed"), true);
    }

    /** Drops a player's half-made link -- called on logout so it can't outlive the session. */
    public void clear(Player player) {
        pending.remove(player.getUUID());
    }

    /**
     * Pops whatever this player last selected with the connector, for a Programmer card's "Link"
     * button -- reusing the same one-click selection {@link #onNodeInteract} already records,
     * without ever completing a real {@code DataNode#linkTo} between two world blocks. A card's
     * wiring lives on the printed program, not as a persistent link on either endpoint.
     */
    public Optional<DataNodeRef> takePending(Player player) {
        Optional<DataNodeRef> found = peekPending(player);
        pending.remove(player.getUUID());
        return found;
    }

    /**
     * Reads a player's pending selection without consuming it -- lets a caller check whether it
     * fits (e.g. a Sensor card checking for a {@code SensorFitting}) before deciding whether to
     * {@link #takePending} it at all.
     */
    public Optional<DataNodeRef> peekPending(Player player) {
        return Optional.ofNullable(pending.get(player.getUUID()))
                .filter(p -> p.dimension() == player.level().dimension()).map(Pending::ref);
    }
}
