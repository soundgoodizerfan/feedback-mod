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
import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What the player is doing at the pot: stirring at this pace (0 to 100) and whether they skimmed since
 * the last packet. Sent a few times a second while the screen is open; the pot is live, so the server
 * applies it as it comes and answers with how the pot is ({@link PotStatePayload}).
 */
public record PotInputPayload(BlockPos pos, int stir, boolean skim) implements CustomPacketPayload {

    public static final Type<PotInputPayload> TYPE = new Type<>(Feedback.id("pot_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotInputPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PotInputPayload::pos,
            ByteBufCodecs.VAR_INT, PotInputPayload::stir,
            ByteBufCodecs.BOOL, PotInputPayload::skim,
            PotInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PotInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.distanceToSqr(payload.pos().getCenter()) > 64
                    || !(player.level().getBlockEntity(payload.pos()) instanceof StockpotBlockEntity pot))
                return;
            if (!io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlock.acceptInput(player, payload.pos()))
                return;
            pot.stir(Math.max(0, Math.min(100, payload.stir())) / 100f);
            if (payload.skim())
                pot.skim();
            PacketDistributor.sendToPlayer(player, new PotStatePayload(payload.pos(), false, pot.scum(), pot.mixed(), pot.charred(), pot.water()));
        });
    }
}
