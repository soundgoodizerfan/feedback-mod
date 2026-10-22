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
package io.github.soundgoodizerfan.feedback.combat.state;

import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;

/**
 * A fighter's combat state, an attachment on any {@link LivingEntity} (the mod's first; see
 * {@link FCombat}). Synced to every client tracking the entity, because the pose and the feel layer
 * read it there; never persisted, so a reload or a respawn starts {@link #IDLE}.
 *
 * <p>Immutable: change it by {@link #set}, which is what makes the attachment sync. Holds state only;
 * the rules that start a phase are in {@code combat/parry} and {@code combat/roll}, the clock that
 * ends one is {@link CombatTicker}.
 *
 * @param phase      what the fighter is committed to
 * @param ticksLeft  ticks until the phase ends. In {@link CombatPhase#IDLE} it is the roll cooldown still to run
 * @param then       ticks of the phase that follows (a parry's recovery, a roll's recovery)
 * @param iFrames    ticks of invulnerability left, independent of phase (a roll's window sits inside it)
 */
public record CombatState(CombatPhase phase, int ticksLeft, int then, int iFrames) {

    public static final CombatState IDLE = new CombatState(CombatPhase.IDLE, 0, 0, 0);

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> CombatPhase.values()[i], CombatPhase::ordinal), CombatState::phase,
            ByteBufCodecs.VAR_INT, CombatState::ticksLeft,
            ByteBufCodecs.VAR_INT, CombatState::then,
            ByteBufCodecs.VAR_INT, CombatState::iFrames,
            CombatState::new);

    /** Free to start a parry or a roll: idle, and no roll cooldown running. */
    public boolean ready() {
        return phase == CombatPhase.IDLE && ticksLeft == 0;
    }

    public static CombatState of(LivingEntity entity) {
        return entity.getData(FCombat.STATE);
    }

    public static void set(LivingEntity entity, CombatState state) {
        entity.setData(FCombat.STATE, state);
    }
}
