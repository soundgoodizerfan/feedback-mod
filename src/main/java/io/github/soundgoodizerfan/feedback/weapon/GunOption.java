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
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * One part a gun can have fitted, loaded from {@code data/<namespace>/gun_option/*.json}
 * ({@code docs/feedback_guns_handoff_options.md} section 4). The schema is the ownership rule
 * ({@code docs/feedback_guns_spec.md} 13.1): every field is a figure the <em>gun</em> owns, and
 * none names a round figure, so an option cannot reach a charge, a projectile or a case. A new
 * figure channel is a code change on purpose: the ownership column is decided when it is added.
 * Each effect is a multiplier on a figure the bake computes, one at 1 changing nothing.
 *
 * @param id       the file's id; set by the loader, so a file does not write it.
 * @param form     the part form it is machined as: what an assembly or a refit looks for.
 * @param slot     options in one slot exclude each other: one muzzle device, one vent. The Stand
 *                 draws a row per slot.
 * @param part     the Stand part whose rows it appears under.
 * @param metalMb  the metal in it, in mB: what it weighs and costs.
 * @param reliefAt fraction of the chamber's rating at which it opens: the pressure the chamber
 *                 sees never passes it.
 * @param bleed    share of the shot's work that goes out of it.
 * @param gasCut   share of the gas's part of the recoil that it throws away.
 * @param blast    multiplier on the gas's part of the report.
 * @param heat     multiplier on the heat a shot leaves in the gun.
 * @param shed     multiplier on how fast the barrel sheds heat.
 * @param eddy     share of a coupled bullet's muzzle energy drained after it leaves the barrel, times its
 *                 metal's ampacity (copper = 1); the recoil is the speed before the drag.
 * @param trait    a trait the fitting adds to the gun, by id in {@code ModularTraits} (a selector switch giving
 *                 select fire, a bipod giving braced); empty for a fitting that only changes figures. A fitting
 *                 is a tip-like optional part of the gun: it may add a trait, change figures, or both
 *                 ({@code docs/feedback_modular_items_spec.md} 8.4).
 * @param params   the numbers that trait reads, by name.
 */
public record GunOption(ResourceLocation id, String form, String slot, String part, float metalMb,
                        Optional<Float> reliefAt, float bleed, float gasCut, float blast, float heat, float shed,
                        float eddy, String trait, java.util.Map<String, Float> params) {

    public static final Codec<GunOption> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(GunOption::id),
            Codec.STRING.fieldOf("form").forGetter(GunOption::form),
            Codec.STRING.fieldOf("slot").forGetter(GunOption::slot),
            Codec.STRING.fieldOf("part").forGetter(GunOption::part),
            Codec.floatRange(0, 100).fieldOf("metal_mb").forGetter(GunOption::metalMb),
            Codec.floatRange(0, 1).optionalFieldOf("relief_at").forGetter(GunOption::reliefAt),
            Codec.floatRange(0, 1).optionalFieldOf("bleed", 0f).forGetter(GunOption::bleed),
            Codec.floatRange(0, 1).optionalFieldOf("gas_cut", 0f).forGetter(GunOption::gasCut),
            Codec.floatRange(0, 100).optionalFieldOf("blast", 1f).forGetter(GunOption::blast),
            Codec.floatRange(0, 100).optionalFieldOf("heat", 1f).forGetter(GunOption::heat),
            Codec.floatRange(0, 100).optionalFieldOf("shed", 1f).forGetter(GunOption::shed),
            Codec.floatRange(0, 1).optionalFieldOf("eddy", 0f).forGetter(GunOption::eddy),
            Codec.STRING.optionalFieldOf("trait", "").forGetter(GunOption::trait),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", java.util.Map.of()).forGetter(GunOption::params)
    ).apply(i, GunOption::new));

    private static final StreamCodec<io.netty.buffer.ByteBuf, java.util.Map<String, Float>> PARAMS =
            ByteBufCodecs.<io.netty.buffer.ByteBuf, String, Float, java.util.Map<String, Float>>map(
                    java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT);

    /** Written out by hand: fourteen fields is past what {@code composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GunOption> STREAM_CODEC = StreamCodec.of(
            (buf, o) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, o.id);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.form);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.slot);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.part);
                ByteBufCodecs.FLOAT.encode(buf, o.metalMb);
                ByteBufCodecs.optional(ByteBufCodecs.FLOAT).encode(buf, o.reliefAt);
                ByteBufCodecs.FLOAT.encode(buf, o.bleed);
                ByteBufCodecs.FLOAT.encode(buf, o.gasCut);
                ByteBufCodecs.FLOAT.encode(buf, o.blast);
                ByteBufCodecs.FLOAT.encode(buf, o.heat);
                ByteBufCodecs.FLOAT.encode(buf, o.shed);
                ByteBufCodecs.FLOAT.encode(buf, o.eddy);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.trait);
                PARAMS.encode(buf, o.params);
            },
            buf -> new GunOption(ResourceLocation.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.optional(ByteBufCodecs.FLOAT).decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    PARAMS.decode(buf)));

    public String langKey() {
        return "gun.feedback.fitting." + id.getPath();
    }
}
