// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Programmer inside a Computer: the same editing logic, held by the Computer block entity and never placed in the world on its own.
 * It is electric (takes every medium), is valid while its host is, and tells its host when it changes.
 */
public class EmbeddedProgrammer extends ProgrammerBlockEntity {

    private final BlockEntity host;
    private java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> closed = p -> { };

    /** Told, on the server, when a player's editor menu closes: the Computer puts the medium back in its bay and reopens the desktop. */
    public void onEditorClosed(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> closed) {
        this.closed = closed;
    }

    void editorClosed(net.minecraft.server.level.ServerPlayer player) {
        closed.accept(player);
    }

    public EmbeddedProgrammer(BlockEntity host) {
        super(host.getBlockPos(), host.getBlockState());
        this.host = host;
    }

    @Override
    protected boolean electric() {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(host, player);
    }

    @Override
    public void sync() {
        host.setChanged();
        if (host.getLevel() != null && !host.getLevel().isClientSide)
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
    }
}
