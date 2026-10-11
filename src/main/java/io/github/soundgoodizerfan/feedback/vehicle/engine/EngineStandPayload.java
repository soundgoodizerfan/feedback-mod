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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Engine Stand screen's edit, and whether to print it -- {@code ProgrammerCommitPayload}'s shape. */
public record EngineStandPayload(BlockPos pos, EngineDesign design, boolean print) implements CustomPacketPayload {

    public static final Type<EngineStandPayload> TYPE = new Type<>(Feedback.id("engine_stand"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineStandPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EngineStandPayload::pos,
            EngineDesign.STREAM_CODEC, EngineStandPayload::design,
            ByteBufCodecs.BOOL, EngineStandPayload::print,
            EngineStandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EngineStandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Menu check first, so a client cannot name an arbitrary position.
            if (context.player().containerMenu instanceof EngineStandMenu menu && menu.getPos().equals(payload.pos())
                    && context.player().level().getBlockEntity(payload.pos()) instanceof EngineStandBlockEntity stand)
                stand.commit(payload.design(), payload.print(), context.player());
        });
    }
}
