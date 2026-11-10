// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A propeller as baked from its modular design ({@code ModularBake#propeller}): how many blades, how wide the
 * disc is, how coarse the pitch, the blade metal (which sets the tip speed it survives) and a mass
 * multiplier. Every figure the prop block has is derived from these and the material at the moment it is
 * needed -- nothing stored that could disagree.
 *
 * @param blades     blades on the hub.
 * @param diameterCm disc diameter in cm (a block is a metre).
 * @param pitch      0 fine, 1 medium, 2 coarse: fine pulls hard from a standstill and tops out slow, coarse is the reverse.
 * @param material   the blades' metal.
 * @param massMult   weight against a plain prop (the hub and the mix of metals).
 */
public record PropDesign(int blades, int diameterCm, int pitch, ResourceLocation material, float massMult) {

    public static final int MIN_BLADES = 2, MAX_BLADES = 6, MIN_CM = 80, MAX_CM = 600;

    public static final Codec<PropDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_BLADES, MAX_BLADES).fieldOf("blades").forGetter(PropDesign::blades),
            Codec.intRange(MIN_CM, MAX_CM).fieldOf("diameter").forGetter(PropDesign::diameterCm),
            Codec.intRange(0, 2).fieldOf("pitch").forGetter(PropDesign::pitch),
            ResourceLocation.CODEC.fieldOf("material").forGetter(PropDesign::material),
            Codec.FLOAT.optionalFieldOf("mass_mult", 1f).forGetter(PropDesign::massMult)).apply(i, PropDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PropDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PropDesign::blades,
            ByteBufCodecs.VAR_INT, PropDesign::diameterCm,
            ByteBufCodecs.VAR_INT, PropDesign::pitch,
            ResourceLocation.STREAM_CODEC, PropDesign::material,
            ByteBufCodecs.FLOAT, PropDesign::massMult,
            PropDesign::new);

    /** The settings pulled back into range, so a design is always buildable. */
    public PropDesign {
        blades = Mth.clamp(blades, MIN_BLADES, MAX_BLADES);
        diameterCm = Mth.clamp(diameterCm, MIN_CM, MAX_CM);
        pitch = Mth.clamp(pitch, 0, 2);
    }

    /** Disc diameter in blocks (metres). */
    public double diameter() {
        return diameterCm / 100.0;
    }
}
