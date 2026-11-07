// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.resources.ResourceLocation;

/**
 * What one profession commissions: for each modular type, the figures an order may ask about and
 * the span each is drawn from. An order picks a target inside a span, so what is asked is always
 * something a designer can reach with some choice of parts.
 *
 * @param profession a villager profession id
 */
public record CommissionProfile(ResourceLocation profession, List<Entry> orders) {

    /** One figure an order may ask about, and the span its target is drawn from. */
    public record Axis(Fig fig, float lo, float hi) {

        private static final Codec<Fig> FIG = Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Fig.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown figure " + name);
            }
        }, fig -> fig.name().toLowerCase(Locale.ROOT));

        public static final Codec<Axis> CODEC = RecordCodecBuilder.create(i -> i.group(
                FIG.fieldOf("fig").forGetter(Axis::fig),
                Codec.FLOAT.fieldOf("lo").forGetter(Axis::lo),
                Codec.FLOAT.fieldOf("hi").forGetter(Axis::hi)).apply(i, Axis::new));

        public float range() {
            return Math.max(1e-3f, hi - lo);
        }
    }

    /**
     * @param type  a modular type id
     * @param level the villager trade level from which the type is ordered
     * @param pay   emeralds for a Good delivery
     */
    public record Entry(ResourceLocation type, int level, int pay, List<Axis> axes, java.util.Map<String, List<Axis>> leans) {

        /** What a villager of this type (a biome's villagers: {@code taiga}, {@code desert}) leans toward on a dish: each axis' span is how far it is flexible, its middle what it likes. */
        public List<Axis> leanFor(String villagerType) {
            return leans.getOrDefault(villagerType, List.of());
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(Entry::type),
                Codec.INT.optionalFieldOf("level", 1).forGetter(Entry::level),
                Codec.INT.fieldOf("pay").forGetter(Entry::pay),
                Axis.CODEC.listOf().optionalFieldOf("axes", List.of()).forGetter(Entry::axes),
                Codec.unboundedMap(Codec.STRING, Axis.CODEC.listOf()).optionalFieldOf("leans", java.util.Map.of()).forGetter(Entry::leans)).apply(i, Entry::new));
    }

    public static final Codec<CommissionProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("profession").forGetter(CommissionProfile::profession),
            Entry.CODEC.listOf().fieldOf("orders").forGetter(CommissionProfile::orders)).apply(i, CommissionProfile::new));
}
