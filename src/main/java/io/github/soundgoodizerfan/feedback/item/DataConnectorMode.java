// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a Data Connector's use does; changed by shift-scroll. Lives on the stack so the client sees it. */
public enum DataConnectorMode {
    /** Select one node, then another, to join them with wire. */
    LINK,
    /** Select two linked nodes in turn to cut the wire between them. */
    UNLINK,
    /** Use on a node to cut every wire it carries. */
    CLEAR;

    public static final Codec<DataConnectorMode> CODEC = Codec.INT.xmap(DataConnectorMode::of, DataConnectorMode::ordinal);
    public static final StreamCodec<ByteBuf, DataConnectorMode> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DataConnectorMode::of, DataConnectorMode::ordinal);

    public static DataConnectorMode of(int ordinal) {
        DataConnectorMode[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }

    public DataConnectorMode step(int direction) {
        return of(ordinal() + direction);
    }
}
