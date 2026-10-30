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

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a dough was kneaded at the Prep Table ({@code docs/specs/feedback_food_spec.md} 6): absent means nobody did. A class, never a
 * measurement; {@link io.github.soundgoodizerfan.feedback.modular.Kneading} grades it. It rides the stack through baking, where
 * {@link Cooking#apply} scales the loaf's hunger and saturation by it.
 *
 * @param grade 0 crumbly, 1 good, 2 glossy, 3 tough
 */
public record FoodKnead(int grade) {

    public static final int CRUMBLY = 0, GOOD = 1, GLOSSY = 2, TOUGH = 3;

    public static final Codec<FoodKnead> CODEC = Codec.INT.xmap(FoodKnead::new, FoodKnead::grade);

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodKnead> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, FoodKnead::grade, FoodKnead::new);

    /** What this class does to the baked loaf's hunger and saturation. */
    public float scale() {
        return switch (grade) {
            case GLOSSY -> FTuning.KNEAD_GLOSSY_SCALE;
            case GOOD -> FTuning.KNEAD_GOOD_SCALE;
            case CRUMBLY -> FTuning.KNEAD_CRUMBLY_SCALE;
            default -> FTuning.KNEAD_TOUGH_SCALE;
        };
    }
}
