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

import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The server's end of the parry and roll lifecycle cues: what happened to a fighter that is not an impact
 * (that is {@link Feel}). The server decides, the sounds go out as ordinary sound packets, so tracking
 * players hear them with no payload of their own. A refused press is the client's cue
 * ({@code CombatKeys}); the client already knows the state. Figures in {@link FeelTuning}.
 */
public final class CombatCues {

    private CombatCues() {
    }

    /** A parry press accepted, or a roll begun: heard by everyone in range, the player included. */
    public static void everyone(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** The window closed on nothing: a dull cue and a puff at the hand, loud to the fighter, quiet to bystanders. */
    public static void whiff(ServerPlayer player) {
        SoundEvent sound = FSounds.PARRY_WHIFF.get();
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                FeelTuning.WHIFF_VOLUME * FeelTuning.WHIFF_BYSTANDER, FeelTuning.WHIFF_PITCH);
        player.playNotifySound(sound, SoundSource.PLAYERS, FeelTuning.WHIFF_VOLUME, FeelTuning.WHIFF_PITCH);
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(FeelTuning.WHIFF_PUFF_REACH)).subtract(0, 0.4, 0);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.POOF, hand.x, hand.y, hand.z,
                FeelTuning.WHIFF_PUFF_COUNT, 0.1, 0.05, 0.1, 0.01);
    }

    /** Free to act again: a tick for the fighter alone, if {@link FeelTuning#READY_CUE} is on. */
    public static void ready(LivingEntity who) {
        if (FeelTuning.READY_CUE && who instanceof ServerPlayer player)
            player.playNotifySound(FSounds.READY_TICK.get(), SoundSource.PLAYERS, FeelTuning.READY_VOLUME, FeelTuning.READY_PITCH);
    }
}
