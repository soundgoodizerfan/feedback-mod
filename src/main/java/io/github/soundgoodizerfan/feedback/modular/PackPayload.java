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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A pack key was pressed: the server opens the worn pack ({@link ModularBackpack#open}), or its crafting pad (action {@link #PAD}). */
public record PackPayload(byte action) implements CustomPacketPayload {

    public static final byte OPEN = 0, PAD = 1, DROP = 2, SLEEP = 3;

    public static final Type<PackPayload> TYPE = new Type<>(Feedback.id("open_pack"));

    public static final StreamCodec<ByteBuf, PackPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new PackPayload(buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer p = (ServerPlayer) context.player();
            switch (payload.action()) {
                case PAD -> ModularBackpack.openPad(p);
                case DROP -> ModularBackpack.release(p);
                case SLEEP -> ModularBackpack.sleep(p);
                default -> ModularBackpack.open(p);
            }
        });
    }
}
