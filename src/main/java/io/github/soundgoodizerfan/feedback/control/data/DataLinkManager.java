// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.item.DataConnectorMode;

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
    public void onNodeInteract(Player player, DataNode node, DataConnectorMode mode) {
        if (mode == DataConnectorMode.CLEAR) {
            pending.remove(player.getUUID());
            tell(player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload.cleared());
            node.pruneNodeLinks();
            boolean had = !node.getNodeLinks().isEmpty();
            node.unlinkAll();
            player.displayClientMessage(Component.translatable(had
                    ? "feedback.data_connector.cleared" : "feedback.data_connector.no_links"), true);
            return;
        }
        UUID id = player.getUUID();
        Level level = node.getNodeLevel();
        DataNodeRef previous = Optional.ofNullable(pending.remove(id))
                .filter(p -> p.dimension() == level.dimension()).map(Pending::ref).orElse(null);

        if (previous == null || previous.equals(node.getNodeRef())) {
            pending.put(id, new Pending(level.dimension(), node.getNodeRef()));
            tell(player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload.of(node.getNodeRef()));
            return;
        }

        tell(player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload.cleared());
        Optional<DataNode> from = DataNode.resolve(level, previous);
        if (from.isEmpty()) {
            player.displayClientMessage(Component.translatable("feedback.data_connector.failed"), true);
            return;
        }
        DataNode a = from.get();
        a.pruneNodeLinks();
        node.pruneNodeLinks();
        if (mode == DataConnectorMode.UNLINK) {
            boolean joined = a.getNodeLinks().contains(node.getNodeRef());
            if (joined) {
                a.unlink(node.getNodeRef());
                node.unlink(a.getNodeRef());
            }
            player.displayClientMessage(Component.translatable(joined
                    ? "feedback.data_connector.unlinked" : "feedback.data_connector.not_linked"), true);
            return;
        }
        if (!a.canLinkTo(node)) {
            player.displayClientMessage(Component.translatable("feedback.data_connector.failed"), true);
            return;
        }
        int blocks = (int) Math.ceil(Math.sqrt(a.getNodePos().distSqr(node.getNodePos())));
        DataWires.Paid paid = DataWires.pay(player, blocks);
        if (!paid.enough()) {
            player.displayClientMessage(Component.translatable(paid.inRange()
                    ? "feedback.data_connector.wire" : "feedback.data_connector.range", paid.pieces()), true);
            return;
        }
        boolean linked = a.linkTo(node);
        if (linked)
            DataWires.record(level, a.getNodeRef(), node.getNodeRef(), paid.grade());
        player.displayClientMessage(Component.translatable(linked
                ? "feedback.data_connector.linked" : "feedback.data_connector.failed"), true);
    }

    /** Drops a player's half-made link -- called on logout so it can't outlive the session. */
    public void clear(Player player) {
        pending.remove(player.getUUID());
    }

    /** Tells the player's client what is selected, so it can stretch a wire to the aim. */
    private static void tell(Player player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload payload) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload);
        // The chip by the crosshair; DataSelection already stretches the wire, so no line here.
        if (payload.none())
            io.github.soundgoodizerfan.feedback.net.AimPayloads.linkDone(player);
        else
            io.github.soundgoodizerfan.feedback.net.AimPayloads.linkFrom(player, null, player.level().getBlockState(payload.pos()).getBlock().getName());
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
        tell(player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload.cleared());
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
