// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin.sorter;

import java.lang.reflect.Method;

import io.github.soundgoodizerfan.feedback.compat.sorter.FoodMerge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;

/**
 * Inventory Sorter (cpw, GPL-3.0) groups stacks to merge through {@code InventoryHandler.ItemStackComparator}, a comparator that
 * answers 0 only for the same item with the same components. Taken: where it decides (read in its 1.21 source). Added: a food
 * whose state is within a minute and a few percent of another's counts as the same food ({@link FoodMerge}), and both stacks
 * are set to the worse state first. Rejected: a mixin into its stack-key hash (stack identity is a hazard) and its move-matching
 * check (scroll-move keeps exact food).
 */
@Pseudo
@Mixin(targets = "cpw.mods.inventorysorter.InventoryHandler$ItemStackComparator", remap = false)
public abstract class ItemStackComparatorMixin {

    // shortcut: reads the holder's record accessor by reflection, as its class is not on our compile path; upgrade if the sorter publishes an API.
    private static Method feedback$accessor;
    private static boolean feedback$looked;

    private static ItemStack feedback$stack(Object holder) {
        try {
            if (!feedback$looked) {
                feedback$looked = true;
                feedback$accessor = holder.getClass().getMethod("is");
            }
            return feedback$accessor == null ? null : (ItemStack) feedback$accessor.invoke(holder);
        } catch (ReflectiveOperationException | RuntimeException e) {
            feedback$accessor = null;
            return null;
        }
    }

    @Inject(method = "compare(Lcpw/mods/inventorysorter/ItemStackHolder;Lcpw/mods/inventorysorter/ItemStackHolder;)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void feedback$nearFood(@Coerce Object first, @Coerce Object second, CallbackInfoReturnable<Integer> cir) {
        ItemStack a = feedback$stack(first), b = feedback$stack(second);
        if (a != null && b != null && a != b && FoodMerge.near(a, b)) {
            FoodMerge.worsen(a, b);
            cir.setReturnValue(0);
        }
    }
}
