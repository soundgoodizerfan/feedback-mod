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
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Reloading Press screen: the die set's bore, and whether the lever was pulled. */
public record PressPayload(BlockPos pos, int bore, boolean seat) implements CustomPacketPayload {

    public static final Type<PressPayload> TYPE = new Type<>(Feedback.id("reloading_press"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PressPayload::pos,
            ByteBufCodecs.VAR_INT, PressPayload::bore,
            ByteBufCodecs.BOOL, PressPayload::seat,
            PressPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ReloadingPressMenu menu && menu.getPos().equals(payload.pos())
                    && context.player().level().getBlockEntity(payload.pos()) instanceof ReloadingPressBlockEntity press)
                press.commit(payload.bore(), payload.seat(), context.player());
        });
    }
}
