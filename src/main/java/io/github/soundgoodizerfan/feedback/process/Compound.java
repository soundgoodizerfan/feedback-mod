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
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * What a food's named active does when eaten (food spec 7), as data: a compound maps to a mob effect by
 * property, never by item, so a modded food or a modded effect joins by writing a file. Strength comes
 * from the food; this says what a unit of it buys.
 *
 * @param effect          the effect it gives.
 * @param amplifier       the effect's level, 0 is I.
 * @param ticksPerStrength how long a full-strength dose lasts, before fat and fibre stretch it.
 * @param crash           what follows when it ends, if the dose came on fast: the overrun of a buff.
 * @param direction       where it pulls the potion marker on the brewing map, in degrees (0 is +x, 90 is +y); absent, it does not steer
 *                        (potions spec 3.1). Classes come in opposing pairs, so adding one can cancel another.
 * @param gain            how long a full-strength dose's vector is, against 1 for a main axis; a short one is for fine correction.
 * @param taste           three figures (sweet, bitter, pungent) a potion's taste is averaged from (potions spec 7); empty is tasteless.
 * @param color           the class's colour ("#rrggbb" in data), for the brewing compass and a dud draught's tint; absent for a stabiliser.
 * @param pull            a stabiliser: it tightens the marker toward the nearest effect node along one axis, never rescues a wild miss.
 */
public record Compound(Optional<ResourceLocation> effect, int amplifier, int ticksPerStrength, Optional<Crash> crash,
                       Optional<Float> direction, float gain, List<Float> taste, Optional<Pull> pull, Optional<Integer> color) {

    /** @param axis true for the x axis (strength), false for y (duration) */
    public record Pull(boolean axisX, float amount) {
        static final Codec<Pull> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(a -> a.equals("x"), x -> x ? "x" : "y").fieldOf("axis").forGetter(Pull::axisX),
                Codec.floatRange(0f, 1f).fieldOf("amount").forGetter(Pull::amount)
        ).apply(instance, Pull::new));
    }

    /** A weak effect after a fast dose wears off. */
    public record Crash(ResourceLocation effect, int ticks) {
        static final Codec<Crash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Crash::effect),
                Codec.intRange(1, 6000).fieldOf("ticks").forGetter(Crash::ticks)
        ).apply(instance, Crash::new));
    }

    public static final Codec<Compound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(Compound::effect),
            Codec.intRange(0, 4).optionalFieldOf("amplifier", 0).forGetter(Compound::amplifier),
            Codec.intRange(20, 36000).optionalFieldOf("ticks_per_strength", 600).forGetter(Compound::ticksPerStrength),
            Crash.CODEC.optionalFieldOf("crash").forGetter(Compound::crash),
            Codec.FLOAT.optionalFieldOf("direction").forGetter(Compound::direction),
            Codec.floatRange(0f, 2f).optionalFieldOf("gain", 1f).forGetter(Compound::gain),
            Codec.FLOAT.listOf(3, 3).optionalFieldOf("taste", List.of()).forGetter(Compound::taste),
            Pull.CODEC.optionalFieldOf("pull").forGetter(Compound::pull),
            Material.HEX.optionalFieldOf("color").forGetter(Compound::color)
    ).apply(instance, Compound::new));
}
