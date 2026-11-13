// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a dough was kneaded at the Prep Table ({@code docs/specs/feedback_food_spec.md} 6): absent means nobody did. A class, never a
 * measurement; {@link io.github.soundgoodizerfan.feedback.modular.prep.Kneading} grades it. It rides the stack through baking, where
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
            case GLOSSY -> ProcessTuning.KNEAD_GLOSSY_SCALE;
            case GOOD -> ProcessTuning.KNEAD_GOOD_SCALE;
            case CRUMBLY -> ProcessTuning.KNEAD_CRUMBLY_SCALE;
            default -> ProcessTuning.KNEAD_TOUGH_SCALE;
        };
    }
}
