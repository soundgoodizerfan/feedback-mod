// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * One part a gun can have fitted, loaded from {@code data/<namespace>/gun_option/*.json}. The schema is the ownership rule
 * ({@code docs/specs/feedback_guns_spec.md} 13.1): a fitting reaches only figures the <em>gun</em> owns, never a charge,
 * a projectile or a case. The file holds the fitting's geometry; what it does is physics ({@link FittingPhysics}), read from
 * the metal it is machined in, so no file states a multiplier.
 *
 * @param id      the file's id; set by the loader, so a file does not write it.
 * @param form    the part form it is machined as: what an assembly or a refit looks for.
 * @param slot    options in one slot exclude each other: one muzzle device, one vent. The Stand draws a row per slot.
 * @param part    the Stand part whose rows it appears under.
 * @param metalMb the metal in it, in mB: what it weighs and costs.
 * @param trait   a trait the fitting adds to the gun, by id in {@code ModularTraits} (a selector switch giving select fire, a
 *                bipod giving braced); empty for a fitting that only changes figures.
 * @param params  the geometry, by name: {@code port_area} (a brake's or vent's ports against a reference), {@code fin_area}
 *                (a shroud's), {@code eddy} (an eddy sleeve's coupling), and what the fitting's trait reads.
 */
public record GunOption(ResourceLocation id, String form, String slot, String part, float metalMb, String trait,
                        java.util.Map<String, Float> params) {

    public static final Codec<GunOption> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GunOption::id),
            Codec.STRING.fieldOf("form").forGetter(GunOption::form),
            Codec.STRING.fieldOf("slot").forGetter(GunOption::slot),
            Codec.STRING.fieldOf("part").forGetter(GunOption::part),
            Codec.floatRange(0, 100).fieldOf("metal_mb").forGetter(GunOption::metalMb),
            Codec.STRING.optionalFieldOf("trait", "").forGetter(GunOption::trait),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", java.util.Map.of()).forGetter(GunOption::params)
    ).apply(i, GunOption::new));

    private static final StreamCodec<io.netty.buffer.ByteBuf, java.util.Map<String, Float>> PARAMS =
            ByteBufCodecs.<io.netty.buffer.ByteBuf, String, Float, java.util.Map<String, Float>>map(
                    java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT);

    public static final StreamCodec<RegistryFriendlyByteBuf, GunOption> STREAM_CODEC = StreamCodec.of(
            (buf, o) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, o.id);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.form);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.slot);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.part);
                ByteBufCodecs.FLOAT.encode(buf, o.metalMb);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.trait);
                PARAMS.encode(buf, o.params);
            },
            buf -> new GunOption(ResourceLocation.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    PARAMS.decode(buf)));

    public String langKey() {
        return "gun.feedback.fitting." + id.getPath();
    }

    /** A geometry figure by name, {@code fallback} if the file does not give it. */
    public float param(String name, float fallback) {
        return params.getOrDefault(name, fallback);
    }
}
