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
package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * A fighter's cast loadout and stamina: the techniques slotted to the cast keys (empty string: a free slot) and the
 * stamina left, in whole points, and per slot what its technique waits on ({@link Rearms#wait}: 0 ready). The attachment {@link FCombat#CAST}: persisted, kept on death, synced to the owner.
 * Stamina is the one meter in the combat kit (user ruling 2026-10-24); the exact fraction lives server-side in
 * {@link Stamina}, this carries what the bar shows.
 */
public record CastState(List<String> slots, int stamina, List<Integer> waits) {

    public static final int SLOTS = 3;

    public static final CastState EMPTY = new CastState(List.of("", "", ""), Stamina.MAX, List.of(0, 0, 0));

    public static final Codec<CastState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("slots").forGetter(CastState::slots),
            Codec.INT.fieldOf("stamina").forGetter(CastState::stamina),
            Codec.INT.listOf().optionalFieldOf("waits", List.of(0, 0, 0)).forGetter(CastState::waits)).apply(i, CastState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CastState::slots,
            ByteBufCodecs.VAR_INT, CastState::stamina,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CastState::waits,
            CastState::new);

    public static CastState of(Player p) {
        CastState s = p.getData(FCombat.CAST);
        return s.slots.size() == SLOTS ? s : EMPTY;
    }

    public static void set(Player p, CastState s) {
        p.setData(FCombat.CAST, s);
    }

    public CastState withStamina(int points) {
        return new CastState(slots, points, waits);
    }

    public CastState withWaits(List<Integer> next) {
        return new CastState(slots, stamina, next);
    }

    /** {@code tech} in slot {@code slot}; the tech leaves any other slot it held. An empty tech clears the slot. */
    public CastState slotted(int slot, String tech) {
        List<String> next = new ArrayList<>(slots);
        for (int i = 0; i < next.size(); i++)
            if (!tech.isEmpty() && next.get(i).equals(tech))
                next.set(i, "");
        next.set(slot, tech);
        return new CastState(next, stamina, waits);
    }
}
