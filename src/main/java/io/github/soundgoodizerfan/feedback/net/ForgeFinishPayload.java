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

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The blows struck, in ticks from the start of the pattern ({@link ForgeBeginPayload}), for the
 * Tool Table the player has open. The menu judges them against the pattern and the stock's own heat,
 * makes the part with the flaws the playing earned and hands it over ({@link ToolTableMenu#finishForge}):
 * it checks the table is still stocked, the hammer is in, and the result did not come faster than
 * the pattern runs. One that sends perfect hits is not caught -- the part is its own player's to
 * spoil, and nothing is lost to anyone else.
 */
public record ForgeFinishPayload(int container, int option, List<Integer> ticks, List<Boolean> heavy) implements CustomPacketPayload {

    public static final Type<ForgeFinishPayload> TYPE = new Type<>(Feedback.id("forge_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeFinishPayload::container,
            ByteBufCodecs.VAR_INT, ForgeFinishPayload::option,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), ForgeFinishPayload::ticks,
            ByteBufCodecs.BOOL.apply(ByteBufCodecs.list(256)), ForgeFinishPayload::heavy,
            ForgeFinishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ForgeFinishPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ToolTableMenu menu
                    && menu.containerId == payload.container())
                menu.finishForge(player, payload.option(), ForgeRhythm.hits(payload.ticks(), payload.heavy()));
        });
    }
}
