// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /** The pulley this player selected, forgotten as it is read: a pulley voxel's click ({@code MicroBelts}) takes it to link to. */
    public GlobalPos takePending(Player player) {
        return pending.remove(player.getUUID());
    }

    /** Called server-side when a player right-clicks a pulley with a Belt in hand. */
    public void onPulleyInteract(Player player, Level level, PulleyBlockEntity pulley) {
        UUID id = player.getUUID();
        // A pulley voxel selected first: this one is its other end.
        var micro = io.github.soundgoodizerfan.feedback.vehicle.micro.MicroBelts.takePending(player);
        if (micro != null) {
            boolean ok = micro.cell().dimension() == level.dimension() && level.isLoaded(micro.cell().pos())
                    && level.getBlockEntity(micro.cell().pos()) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell
                    && pulley.linkMicro(cell, micro.index());
            io.github.soundgoodizerfan.feedback.net.AimPayloads.linkDone(player);
            player.displayClientMessage(Component.translatable(ok ? "feedback.belt.linked" : "feedback.belt.failed"), true);
            return;
        }

        GlobalPos previous = pending.remove(id);
        GlobalPos here = GlobalPos.of(level.dimension(), pulley.getBlockPos());

        if (previous == null || previous.equals(here)) {
            pending.put(id, here);
            io.github.soundgoodizerfan.feedback.net.AimPayloads.linkFrom(player, pulley.getBlockPos(), level.getBlockState(pulley.getBlockPos()).getBlock().getName());
            return;
        }

        // A selection made in another dimension would otherwise resolve at the same coordinates here.
        boolean linked = previous.dimension() == level.dimension() && level.isLoaded(previous.pos())
                && level.getBlockEntity(previous.pos()) instanceof PulleyBlockEntity from
                && from.linkBelt(pulley);
        io.github.soundgoodizerfan.feedback.net.AimPayloads.linkDone(player);
        player.displayClientMessage(Component.translatable(
                linked ? "feedback.belt.linked" : "feedback.belt.failed"), true);
    }

    /** Drops a player's half-made belt so it cannot outlive the session. */
    public void clear(Player player) {
        pending.remove(player.getUUID());
    }
}
