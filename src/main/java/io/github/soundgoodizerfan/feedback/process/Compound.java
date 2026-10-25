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
 */
public record Compound(ResourceLocation effect, int amplifier, int ticksPerStrength, Optional<Crash> crash) {

    /** A weak effect after a fast dose wears off. */
    public record Crash(ResourceLocation effect, int ticks) {
        static final Codec<Crash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Crash::effect),
                Codec.intRange(1, 6000).fieldOf("ticks").forGetter(Crash::ticks)
        ).apply(instance, Crash::new));
    }

    public static final Codec<Compound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Compound::effect),
            Codec.intRange(0, 4).optionalFieldOf("amplifier", 0).forGetter(Compound::amplifier),
            Codec.intRange(20, 36000).fieldOf("ticks_per_strength").forGetter(Compound::ticksPerStrength),
            Crash.CODEC.optionalFieldOf("crash").forGetter(Compound::crash)
    ).apply(instance, Compound::new));
}
