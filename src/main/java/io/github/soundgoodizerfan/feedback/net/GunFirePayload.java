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
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client fired. It has already played the flash, sound and kick (spec 7: prediction); this
 * asks the server to validate and resolve. The aim is the view at the moment of the press.
 */
public record GunFirePayload(float yaw, float pitch, boolean aimed) implements CustomPacketPayload {

    public static final Type<GunFirePayload> TYPE = new Type<>(Feedback.id("gun_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunFirePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GunFirePayload::yaw,
            ByteBufCodecs.FLOAT, GunFirePayload::pitch,
            ByteBufCodecs.BOOL, GunFirePayload::aimed,
            GunFirePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunFirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.level() instanceof ServerLevel level) || player.isSpectator()
                    || !Float.isFinite(payload.yaw()) || !Float.isFinite(payload.pitch()))
                return;
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem)
                GunShots.trigger(level, player, stack, payload.yaw(), payload.pitch(), System.currentTimeMillis(), payload.aimed());
        });
    }
}
