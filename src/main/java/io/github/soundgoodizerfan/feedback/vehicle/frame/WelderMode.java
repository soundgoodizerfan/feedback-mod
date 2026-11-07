// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a Welder's use does; changed by shift-scroll. Lives on the stack so the client sees it. */
public enum WelderMode {
    /** Place points, and select them by clicking; the gizmo moves the selection. */
    POINT,
    /** Select points by box: two corners, everything inside. The gizmo moves the selection. */
    SELECT,
    /** Join two points with an edge, or a tube if a block is in the off hand. */
    CONNECT,
    /** Choose three points to lay a plate in the off-hand block. */
    PLATE,
    /** Weld a point to the block it touches, or unweld it. */
    WELD,
    /**
     * Pull the selection out along a gizmo arrow: a copy of each point, joined to it (a tube of the
     * off-hand block if one is held), and edges between the copies where the originals had them.
     */
    EXTRUDE,
    /**
     * Take points, plates and tubes away. Its own mode, never a sneak-click in the others: every
     * playtester deleted a point they needed with a stray sneak (2026-10-16).
     */
    REMOVE;

    public static final Codec<WelderMode> CODEC = Codec.INT.xmap(WelderMode::of, WelderMode::ordinal);
    public static final StreamCodec<ByteBuf, WelderMode> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(WelderMode::of, WelderMode::ordinal);

    public static WelderMode of(int ordinal) {
        WelderMode[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }

    public WelderMode step(int direction) {
        return of(ordinal() + direction);
    }
}
