// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.CameraItem;
import io.github.soundgoodizerfan.feedback.item.Photo;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the picture a camera just took. The server checks the person holds a camera and has not just taken one, then hands over a photograph carrying it. */
public record PhotoPayload(Photo photo) implements CustomPacketPayload {

    public static final Type<PhotoPayload> TYPE = new Type<>(Feedback.id("photo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhotoPayload> STREAM_CODEC =
            Photo.STREAM_CODEC.map(PhotoPayload::new, PhotoPayload::photo);

    /** Ticks between one person's photographs. */
    private static final int COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PhotoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !payload.photo().valid())
                return;
            boolean camera = false;
            for (InteractionHand hand : InteractionHand.values())
                camera |= player.getItemInHand(hand).getItem() instanceof CameraItem;
            long now = player.level().getGameTime();
            if (!camera || now - LAST.getOrDefault(player.getUUID(), -1000L) < COOLDOWN_TICKS)
                return;
            LAST.put(player.getUUID(), now);
            ItemStack photo = new ItemStack(FModular.PHOTOGRAPH.get());
            photo.set(FWeapons.PHOTO.get(), payload.photo());
            player.getInventory().placeItemBackInInventory(photo);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
        });
    }
}
