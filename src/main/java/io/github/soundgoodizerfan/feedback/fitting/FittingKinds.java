// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;

/**
 * What kind a fitting is, as an id the look tables ({@link FittingLooks}) key on. The kind is a property of the fitting's class: the
 * class's simple name without its {@code Fitting} suffix, in snake case ({@code TemperatureSensorFitting} is
 * {@code feedback:temperature_sensor}). Worked out once per class, so reading it every frame costs a lookup and allocates nothing. Never an
 * item id: the item a fitting drops as is not what it is.
 */
public final class FittingKinds {

    private static final ClassValue<ResourceLocation> KINDS = new ClassValue<>() {
        @Override
        protected ResourceLocation computeValue(Class<?> type) {
            String name = type.getSimpleName();
            if (name.endsWith("Fitting") && name.length() > "Fitting".length())
                name = name.substring(0, name.length() - "Fitting".length());
            StringBuilder snake = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isUpperCase(c) && i > 0)
                    snake.append('_');
                snake.append(Character.toLowerCase(c));
            }
            return Feedback.id(snake.toString());
        }
    };

    private FittingKinds() {
    }

    /** The kind of {@code fitting}: {@code feedback:<class name in snake case, without Fitting>}. */
    public static ResourceLocation of(Fitting fitting) {
        return KINDS.get(fitting.getClass());
    }
}
