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
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client asked to switch the held gun's fire mode: semi, burst, auto, round again. */
public record GunModePayload() implements CustomPacketPayload {

    public static final Type<GunModePayload> TYPE = new Type<>(Feedback.id("gun_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunModePayload> STREAM_CODEC =
            StreamCodec.unit(new GunModePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var stack = context.player().getMainHandItem();
            if (stack.getItem() instanceof GunItem)
                stack.set(FWeapons.FIRE_MODE.get(), stack.getOrDefault(FWeapons.FIRE_MODE.get(), FireMode.AUTO).next());
        });
    }
}
