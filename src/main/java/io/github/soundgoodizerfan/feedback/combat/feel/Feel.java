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
package io.github.soundgoodizerfan.feedback.combat.feel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One impact's feel, every figure the client plays: a heavy parry and a roll-cancel are the same
 * code with different numbers (implementation plan 2.6). Pure view and audio; it changes nothing in
 * the world, and the server never waits on it.
 *
 * @param flash       full-screen flash strength, 0 to 1
 * @param rollDegrees camera roll kick at the peak, degrees
 * @param fovPop      fraction of the field of view removed at the peak
 * @param heavy       sound band: false light, true heavy
 * @param sparks      spark particles thrown from the impact
 */
public record Feel(float flash, float rollDegrees, float fovPop, boolean heavy, int sparks) {

    public static final StreamCodec<RegistryFriendlyByteBuf, Feel> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Feel::flash,
            ByteBufCodecs.FLOAT, Feel::rollDegrees,
            ByteBufCodecs.FLOAT, Feel::fovPop,
            ByteBufCodecs.BOOL, Feel::heavy,
            ByteBufCodecs.VAR_INT, Feel::sparks,
            Feel::new);

    /** This feel with every figure held to its tuning ceiling, so a payload cannot ask for the unreasonable. */
    public Feel clamped() {
        return new Feel(Mth.clamp(flash, 0f, 1f),
                Mth.clamp(rollDegrees, 0f, FeelTuning.MAX_ROLL_DEG), Mth.clamp(fovPop, 0f, FeelTuning.MAX_FOV_POP),
                heavy, Mth.clamp(sparks, 0, FeelTuning.MAX_SPARKS));
    }

    /**
     * Plays this feel at {@code at}: the whole of it for {@code subject}, flash, sparks and sound only
     * for everyone else tracking them.
     */
    public void fire(ServerPlayer subject, Vec3 at) {
        Feel f = clamped();
        PacketDistributor.sendToPlayer(subject, new FeelPayload(f, at, true));
        PacketDistributor.sendToPlayersTrackingEntity(subject, new FeelPayload(f, at, false));
    }
}
