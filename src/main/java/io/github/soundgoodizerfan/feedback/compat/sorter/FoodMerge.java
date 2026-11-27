// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.sorter;

import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * When Inventory Sorter's sort treats two food stacks as one: the same item, every component alike except the ones a food's
 * own history writes ({@link FoodState}, the stamped temperature pair, the name and nutrition a state rewrites), and the
 * states within {@link ProcessTuning#SORT_MERGE_FRACTION} on each fraction, a minute on the stamp and
 * {@link ProcessTuning#SORT_MERGE_ENV_TU} on the surroundings. A merge takes the worse of each field, so no stack gets
 * fresher by being sorted ({@code design_digest}: item history never costs stacking; the stack takes the worst value).
 * The temperature pair stays the kept stack's own.
 */
public final class FoodMerge {

    private FoodMerge() {
    }

    /** Whether the sort may count these two stacks as one; false for anything that is not a food with a state. */
    public static boolean near(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem() || a.isEmpty() || b.isEmpty())
            return false;
        FoodState x = a.get(FDataComponents.FOOD_STATE.get());
        FoodState y = b.get(FDataComponents.FOOD_STATE.get());
        if (x == null || y == null || !close(x, y))
            return false;
        return ItemStack.isSameItemSameComponents(bare(a), bare(b));
    }

    /** Both stacks' state set to the worse of the two, so whichever the sort keeps carries it. */
    public static void worsen(ItemStack a, ItemStack b) {
        FoodState x = a.get(FDataComponents.FOOD_STATE.get());
        FoodState y = b.get(FDataComponents.FOOD_STATE.get());
        if (x == null || y == null)
            return;
        FoodState worst = new FoodState(Math.min(x.cook(), y.cook()), Math.max(x.moisture(), y.moisture()), Math.max(x.pathogen(), y.pathogen()),
                Math.max(x.toxin(), y.toxin()), Math.max(x.charred(), y.charred()), Math.max(x.rot(), y.rot()), Math.min(x.stamp(), y.stamp()),
                Math.max(x.envTu(), y.envTu()), Math.min(x.smoke(), y.smoke()), Math.min(x.salt(), y.salt()));
        a.set(FDataComponents.FOOD_STATE.get(), worst);
        b.set(FDataComponents.FOOD_STATE.get(), worst);
    }

    private static boolean close(FoodState x, FoodState y) {
        float f = ProcessTuning.SORT_MERGE_FRACTION;
        return Math.abs(x.cook() - y.cook()) <= f && Math.abs(x.moisture() - y.moisture()) <= f
                && Math.abs(x.pathogen() - y.pathogen()) <= f && Math.abs(x.toxin() - y.toxin()) <= f
                && Math.abs(x.charred() - y.charred()) <= f && Math.abs(x.rot() - y.rot()) <= f
                && Math.abs(x.smoke() - y.smoke()) <= f && Math.abs(x.salt() - y.salt()) <= f
                && Math.abs(x.stamp() - y.stamp()) <= ProcessTuning.SORT_MERGE_STAMP_TICKS
                && Math.abs(x.envTu() - y.envTu()) <= ProcessTuning.SORT_MERGE_ENV_TU;
    }

    /** A copy without the components a food's own history rewrites. */
    private static ItemStack bare(ItemStack stack) {
        ItemStack copy = stack.copy();
        for (DataComponentType<?> type : java.util.List.of(FDataComponents.FOOD_STATE.get(), FDataComponents.TEMPERATURE.get(), FDataComponents.HEATED_AT.get(),
                DataComponents.CUSTOM_NAME, DataComponents.FOOD))
            copy.remove(type);
        return copy;
    }
}
