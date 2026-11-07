// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * One thing on a brewing map ({@code data/<namespace>/potion_effect/*.json}, potions spec 3.2 and 12.3): an effect node, or a
 * hazard or helper. The map is data keyed by {@code base}, so a second base is more rows. Modded effects join by a row.
 *
 * @param kind        what it is.
 * @param base        the liquid whose map it sits on ({@code water} is the only one).
 * @param x           position on the plane.
 * @param y           position on the plane.
 * @param radius      how far from the position it reaches.
 * @param effect      an {@link Kind#EFFECT}'s mob effect and figures.
 * @param above       a {@link Kind#SCORCH}'s temperature: harmless below it, drains potency over it.
 * @param destination a {@link Kind#VORTEX}'s landing point for a marker that reaches its centre.
 */
public record PotionFeature(Kind kind, String base, float x, float y, float radius, Optional<Effect> effect, float above, Optional<List<Float>> destination) {

    /** BOUNDARY is the map's edge: radius is how far from the centre the wall stands. LANDMARK is a harmless fixed mark to steer by in fog. */
    public enum Kind { EFFECT, PRECIPITATE, SCORCH, VORTEX, BOUNDARY, LANDMARK }

    /**
     * @param effect       the mob effect.
     * @param baseDuration ticks of a centred potion.
     * @param maxAmplifier the level (0 is I) of a potion landed on the node's centre; the x offset scales from there down to level 0.
     * @param family       the plant family (a compound class id) the node leans toward, for a dud draught's hint; empty for none.
     * @param imbueHits    multiplies the dip's hits per soak (1 for most; a coat that clings longer sets more).
     */
    public record Effect(ResourceLocation effect, int baseDuration, int maxAmplifier, boolean imbue, int imbueHits, String family) {
        static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Effect::effect),
                Codec.intRange(20, 72000).fieldOf("base_duration").forGetter(Effect::baseDuration),
                Codec.intRange(0, 4).optionalFieldOf("max_amplifier", 0).forGetter(Effect::maxAmplifier),
                Codec.BOOL.optionalFieldOf("imbue", false).forGetter(Effect::imbue),
                Codec.intRange(1, 8).optionalFieldOf("imbue_hits", 1).forGetter(Effect::imbueHits),
                Codec.STRING.optionalFieldOf("family", "").forGetter(Effect::family)
        ).apply(i, Effect::new));
    }

    private static final Codec<Kind> KIND = Codec.STRING.xmap(s -> Kind.valueOf(s.toUpperCase(java.util.Locale.ROOT)), k -> k.name().toLowerCase(java.util.Locale.ROOT));

    public static final Codec<PotionFeature> CODEC = RecordCodecBuilder.create(i -> i.group(
            KIND.optionalFieldOf("kind", Kind.EFFECT).forGetter(PotionFeature::kind),
            Codec.STRING.optionalFieldOf("base", "water").forGetter(PotionFeature::base),
            Codec.FLOAT.listOf(2, 2).fieldOf("position").forGetter(f -> List.of(f.x, f.y)),
            Codec.floatRange(0.01f, 4f).fieldOf("radius").forGetter(PotionFeature::radius),
            Effect.CODEC.optionalFieldOf("effect").forGetter(PotionFeature::effect),
            Codec.FLOAT.optionalFieldOf("above", 90f).forGetter(PotionFeature::above),
            Codec.FLOAT.listOf(2, 2).optionalFieldOf("destination").forGetter(PotionFeature::destination)
    ).apply(i, (kind, base, pos, r, effect, above, dest) -> new PotionFeature(kind, base, pos.get(0), pos.get(1), r, effect, above, dest)));

    public float distance(float px, float py) {
        return (float) Math.hypot(px - x, py - y);
    }
}
