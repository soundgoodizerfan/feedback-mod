// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a worn suit remembers between ticks, on its chestplate: the heat the servos have left in it (Tu, 0 meaning it has not been warmed and sits at ambient), which abilities are on (a bit each,
 * {@link Ability#bit}) and whether the heat has cut them out. The battery is the vanilla container component beside it.
 */
public record SuitState(float heat, int toggles, boolean cut) {

    public static final SuitState NEW = new SuitState(0f, 0, false);

    public static final Codec<SuitState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("heat").forGetter(SuitState::heat),
            Codec.INT.fieldOf("toggles").forGetter(SuitState::toggles),
            Codec.BOOL.optionalFieldOf("cut", false).forGetter(SuitState::cut)
    ).apply(i, SuitState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SuitState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SuitState::heat,
            ByteBufCodecs.VAR_INT, SuitState::toggles,
            ByteBufCodecs.BOOL, SuitState::cut,
            SuitState::new);

    public boolean on(Ability a) {
        return (toggles & a.bit()) != 0;
    }

    public SuitState with(Ability a, boolean on) {
        return new SuitState(heat, on ? toggles | a.bit() : toggles & ~a.bit(), cut);
    }

    public SuitState withHeat(float h, boolean c) {
        return new SuitState(h, toggles, c);
    }
}
