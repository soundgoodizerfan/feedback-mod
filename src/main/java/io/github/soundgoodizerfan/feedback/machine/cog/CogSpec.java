// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cog;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a cut cog carries on its item and in its block entity: the metal it was cut from and its tooth
 * count. A cog item without one is the plain wooden cog (no metal; the tooth count its block defaults to).
 */
public record CogSpec(ResourceLocation metal, int teeth) {

    /** The tooth counts a Gear Cutter offers, one shared pitch: every count meshes with every other. */
    public static final List<Integer> OPTIONS = List.of(8, 12, 16, 24, 32);

    /** Up to and including this many teeth a cog has the small footprint; past it, the large one. */
    public static final int SMALL_MAX_TEETH = 12;

    public static final Codec<CogSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("metal").forGetter(CogSpec::metal),
            Codec.INT.fieldOf("teeth").forGetter(CogSpec::teeth)).apply(i, CogSpec::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CogSpec> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CogSpec::metal,
            ByteBufCodecs.VAR_INT, CogSpec::teeth,
            CogSpec::new);

    public static boolean isLarge(int teeth) {
        return teeth > SMALL_MAX_TEETH;
    }

    public static boolean valid(int teeth) {
        return OPTIONS.contains(teeth);
    }
}
