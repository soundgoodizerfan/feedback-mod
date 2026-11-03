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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
    public record Entry(ResourceLocation type, int level, int pay, List<Axis> axes) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(Entry::type),
                Codec.INT.optionalFieldOf("level", 1).forGetter(Entry::level),
                Codec.INT.fieldOf("pay").forGetter(Entry::pay),
                Axis.CODEC.listOf().fieldOf("axes").forGetter(Entry::axes)).apply(i, Entry::new));
    }

    public static final Codec<CommissionProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("profession").forGetter(CommissionProfile::profession),
            Entry.CODEC.listOf().fieldOf("orders").forGetter(CommissionProfile::orders)).apply(i, CommissionProfile::new));
}
