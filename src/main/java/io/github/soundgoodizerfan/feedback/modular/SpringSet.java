// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * What an overloaded spring does to a spring-driven tool. A spring worked past its rating takes a permanent set (it stores less
 * from then on); one worked far past it snaps, which ruins part of the tool and hurts the hand on it. Either way it is misuse that
 * wears it, never carrying it or leaving it cocked. The set rides on the stack as custom data.
 */
public final class SpringSet {

    private static final String SET = "SpringSet";
    private static final float MAX_SET = 0.9f;
    /** Load over rating below which nothing happens, and above which the spring snaps; between is a set. */
    private static final float SNAP_RATIO = 1.6f;

    public enum Outcome {
        OK, SET, SNAP
    }

    private SpringSet() {
    }

    /** The share of its stored energy the spring has lost to sets, 0 to 0.9. */
    public static float set(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0f : data.copyTag().getFloat(SET);
    }

    /** Works the spring at {@code ratio} times its rating and says what came of it. */
    public static Outcome overload(ItemStack stack, float ratio, LivingEntity wielder) {
        if (ratio <= 1f)
            return Outcome.OK;
        if (ratio <= SNAP_RATIO) {
            float add = 0.1f * (ratio - 1f) / (SNAP_RATIO - 1f) + 0.02f;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(SET, Math.min(MAX_SET, tag.getFloat(SET) + add)));
            return Outcome.SET;
        }
        stack.hurtAndBreak(Math.max(1, stack.getMaxDamage() / 4), wielder, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        wielder.hurt(wielder.damageSources().generic(), 2f);
        return Outcome.SNAP;
    }
}
