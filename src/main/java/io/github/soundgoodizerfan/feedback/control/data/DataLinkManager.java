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
    public void onNodeInteract(Player player, DataNode node, DataConnectorMode mode, net.minecraft.world.InteractionHand hand) {
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
        if (mode == DataConnectorMode.PAINT) {
            boolean painted = a.getNodeLinks().contains(node.getNodeRef()) && colour(a, node, colorIn(player, hand));
            player.displayClientMessage(Component.translatable(painted
                    ? "feedback.data_connector.painted" : "feedback.data_connector.not_painted"), true);
            return;
        }
        if (!a.canLinkTo(node)) {
            player.displayClientMessage(Component.translatable("feedback.data_connector.failed"), true);
            return;
        }
        int blocks = (int) Math.ceil(Math.sqrt(a.getNodePos().distSqr(node.getNodePos())));
        // Two optical terminals are joined by fibre (never wire), paid at one piece a block from the glass carried; any other pair by wire.
        if (a instanceof OpticalEnd ea && node instanceof OpticalEnd eb) {
            linkFibre(player, hand, level, ea, eb, blocks);
            return;
        }
        DataWires.Paid paid = DataWires.pay(player, blocks, Math.max(DataWires.gainOf(a), DataWires.gainOf(node)));
        if (!paid.enough()) {
            player.displayClientMessage(Component.translatable(paid.inRange()
                    ? "feedback.data_connector.wire" : "feedback.data_connector.range", paid.pieces()), true);
            return;
        }
        boolean linked = a.linkTo(node);
        if (linked) {
            DataWires.record(level, a.getNodeRef(), node.getNodeRef(), paid.grade(), paid.critical());
            colour(a, node, colorIn(player, hand));
        }
        player.displayClientMessage(Component.translatable(linked
                ? "feedback.data_connector.linked" : "feedback.data_connector.failed"), true);
    }

    /** Joins two optical terminals with the fibre the player carries; says why when it cannot (no crystal in an end, no fibre, a fibre too lossy even at its best). */
    private void linkFibre(Player player, net.minecraft.world.InteractionHand hand, Level level, OpticalEnd a, OpticalEnd b, int blocks) {
        float share = Math.min(a.budgetShare(), b.budgetShare());
        if (share <= 0f) {
            player.displayClientMessage(Component.translatable("feedback.data_connector.fibre_no_crystal"), true);
            return;
        }
        DataWires.PaidFibre paid = DataWires.payFibre(player, blocks, DataWires.budgetDb(a, b));
        if (!paid.enough()) {
            player.displayClientMessage(Component.translatable(paid.dead()
                    ? "feedback.data_connector.fibre_dead" : "feedback.data_connector.fibre", paid.pieces()), true);
            return;
        }
        boolean linked = a.linkTo(b);
        if (linked) {
            DataWires.recordFibre(level, a.getNodeRef(), b.getNodeRef(), paid.lossDb());
            colour(a, b, colorIn(player, hand));
        }
        player.displayClientMessage(Component.translatable(linked
                ? "feedback.data_connector.linked" : "feedback.data_connector.failed"), true);
    }

    /** The color of the dye in the hand not holding the connector, which a link is made in; {@link DataRequest#ANY} with none. */
    private static int colorIn(Player player, net.minecraft.world.InteractionHand hand) {
        return player.getItemInHand(net.minecraft.world.InteractionHand.values()[1 - hand.ordinal()]).getItem() instanceof net.minecraft.world.item.DyeItem dye ? dye.getDyeColor().getId() : DataRequest.ANY;
    }

    /** Sets the link color on whichever end is a Controller or Computer; false when neither is. */
    private static boolean colour(DataNode a, DataNode b, int color) {
        boolean any = false;
        if (a.getNodeOwner() instanceof io.github.soundgoodizerfan.feedback.control.controller.ControllerHost host) {
            host.core().setColor(b.getNodeRef(), color);
            any = true;
        }
        if (b.getNodeOwner() instanceof io.github.soundgoodizerfan.feedback.control.controller.ControllerHost host) {
            host.core().setColor(a.getNodeRef(), color);
            any = true;
        }
        return any;
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

    /** Takes the node this player last selected with the connector, for a use that pairs it with something other than a node (a mast and its robot). */
    public Optional<DataNodeRef> takePending(Player player) {
        Optional<DataNodeRef> found = peekPending(player);
        pending.remove(player.getUUID());
        tell(player, io.github.soundgoodizerfan.feedback.net.DataSelectionPayload.cleared());
        return found;
    }

    /** What this player has selected, without consuming it. */
    public Optional<DataNodeRef> peekPending(Player player) {
        return Optional.ofNullable(pending.get(player.getUUID()))
                .filter(p -> p.dimension() == player.level().dimension()).map(Pending::ref);
    }
}
