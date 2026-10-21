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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The player began a forging pattern (spec 4.1) in the Tool Table they have open: which menu and
 * which shape on its list. The menu notes when, and how hot the stock is, so it can later judge the
 * hits against the same heat clock the screen played and refuse a result sent faster than the
 * pattern could be played ({@link ToolTableMenu#beginForge}). A payload naming a menu that is not
 * the one open is ignored.
 */
public record ForgeBeginPayload(int container, int option) implements CustomPacketPayload {

    public static final Type<ForgeBeginPayload> TYPE = new Type<>(Feedback.id("forge_begin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeBeginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeBeginPayload::container,
            ByteBufCodecs.VAR_INT, ForgeBeginPayload::option,
            ForgeBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ForgeBeginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ToolTableMenu menu
                    && menu.containerId == payload.container())
                menu.beginForge(player, payload.option());
        });
    }
}
