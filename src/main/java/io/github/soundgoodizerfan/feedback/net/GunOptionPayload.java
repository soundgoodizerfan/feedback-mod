// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.SmartFuse;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client used the "Gun option" key: set switch {@code index} of the held weapon to {@code state}, or step it to its next state when
 * {@code state} is negative (the server owns the state). Sneaking, a tap on a launcher with a fuse control cycles its fuse mode instead.
 */
public record GunOptionPayload(boolean offHand, int index, int state) implements CustomPacketPayload {

    public static final Type<GunOptionPayload> TYPE = new Type<>(Feedback.id("gun_option"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunOptionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GunOptionPayload::offHand, ByteBufCodecs.VAR_INT, GunOptionPayload::index,
            ByteBufCodecs.VAR_INT, GunOptionPayload::state, GunOptionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunOptionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem gun) || !gun.mechanism().built(stack))
                return;
            if (payload.state() < 0 && player.isShiftKeyDown() && SmartFuse.fitted(stack, player.level())) {
                player.displayClientMessage(Component.translatable("message.feedback.fuse_mode", Component.translatable(SmartFuse.next(stack).langKey())), true);
                return;
            }
            var options = gun.mechanism().switches(stack);
            if (payload.index() < 0 || payload.index() >= options.size())
                return;
            var option = options.get(payload.index());
            int to = payload.state() < 0 ? option.next() : payload.state();
            if (to >= option.states().size())
                return;
            gun.mechanism().setSwitch(stack, option.id(), to, player.level().getGameTime());
            player.displayClientMessage(Component.translatable("message.feedback.option." + option.id(),
                    Component.translatable("message.feedback.state." + option.states().get(to))), true);
        });
    }
}
