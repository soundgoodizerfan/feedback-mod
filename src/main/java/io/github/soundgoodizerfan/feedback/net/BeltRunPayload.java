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
package io.github.soundgoodizerfan.feedback.net;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A belt run's cargo as the server holds it, for drawing: where each unit is at {@code time} and how
 * fast the free ones move. Sent when cargo changes or the speed does, never every tick; the client
 * extrapolates. {@code shape} (the run's cells and slopes) rides along only when new or every few
 * seconds; a run with an empty shape is gone.
 */
public record BeltRunPayload(long id, int facing, long[] shape, byte[] kinds, float speed, long time, List<Entry> cargo)
        implements CustomPacketPayload {

    /** One unit: the stack, its coordinate along the run and whether it is held still. */
    public record Entry(ItemStack stack, float pos, boolean pinned) {
    }

    public static final Type<BeltRunPayload> TYPE = new Type<>(Feedback.id("belt_run"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeltRunPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.id);
                buf.writeByte(p.facing);
                buf.writeBoolean(p.shape != null);
                if (p.shape != null) {
                    buf.writeVarInt(p.shape.length);
                    for (int i = 0; i < p.shape.length; i++) {
                        buf.writeLong(p.shape[i]);
                        buf.writeByte(p.kinds[i]);
                    }
                }
                buf.writeFloat(p.speed);
                buf.writeLong(p.time);
                buf.writeVarInt(p.cargo.size());
                for (Entry e : p.cargo) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.stack);
                    buf.writeFloat(e.pos);
                    buf.writeBoolean(e.pinned);
                }
            },
            buf -> {
                long id = buf.readLong();
                int facing = buf.readByte();
                long[] shape = null;
                byte[] kinds = null;
                if (buf.readBoolean()) {
                    int n = buf.readVarInt();
                    shape = new long[n];
                    kinds = new byte[n];
                    for (int i = 0; i < n; i++) {
                        shape[i] = buf.readLong();
                        kinds[i] = buf.readByte();
                    }
                }
                float speed = buf.readFloat();
                long time = buf.readLong();
                int count = buf.readVarInt();
                List<Entry> cargo = new ArrayList<>(count);
                for (int i = 0; i < count; i++)
                    cargo.add(new Entry(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readFloat(), buf.readBoolean()));
                return new BeltRunPayload(id, facing, shape, kinds, speed, time, cargo);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeltRunPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.ClientBeltRuns.receive(payload));
    }
}
