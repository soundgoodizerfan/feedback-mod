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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.GunStandMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Gun Stand screen's edit, and whether to print it -- {@code EngineStandPayload}'s shape. */
public record GunStandPayload(BlockPos pos, GunDesign design, String name, boolean print) implements CustomPacketPayload {

    public static final Type<GunStandPayload> TYPE = new Type<>(Feedback.id("gun_stand"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunStandPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GunStandPayload::pos,
            GunDesign.STREAM_CODEC, GunStandPayload::design,
            ByteBufCodecs.stringUtf8(GunStandBlockEntity.NAME_MAX), GunStandPayload::name,
            ByteBufCodecs.BOOL, GunStandPayload::print,
            GunStandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunStandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Menu check first, so a client cannot name an arbitrary position.
            if (context.player().containerMenu instanceof GunStandMenu menu && menu.getPos().equals(payload.pos())
                    && context.player().level().getBlockEntity(payload.pos()) instanceof GunStandBlockEntity stand)
                stand.commit(payload.design(), payload.name(), payload.print(), context.player());
        });
    }
}
