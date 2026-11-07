// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    public static final int SLOTS = 5;

    public static final CastState EMPTY = new CastState(List.of("", "", "", "", ""), Stamina.MAX, List.of(0, 0, 0, 0, 0));

    public static final Codec<CastState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("slots").forGetter(CastState::slots),
            Codec.INT.fieldOf("stamina").forGetter(CastState::stamina),
            Codec.INT.listOf().optionalFieldOf("waits", List.of(0, 0, 0, 0, 0)).forGetter(CastState::waits)).apply(i, CastState::new));

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
