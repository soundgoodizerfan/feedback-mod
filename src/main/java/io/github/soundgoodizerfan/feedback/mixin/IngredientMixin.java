// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin;

import io.github.soundgoodizerfan.feedback.modular.item.ConvertedIngredients;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Every recipe of every mod that asks for a vanilla item accepts what Feedback turns that item into
 * ({@link ConvertedIngredients}): a player never holds the vanilla bowl, so a recipe that wants one would
 * otherwise never match.
 */
@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void feedback$acceptsConverted(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && stack != null && ConvertedIngredients.accepts(((Ingredient) (Object) this)::test, stack))
            cir.setReturnValue(true);
    }

    @Inject(method = "getItems()[Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void feedback$listsConverted(CallbackInfoReturnable<ItemStack[]> cir) {
        cir.setReturnValue(ConvertedIngredients.widen(cir.getReturnValue()));
    }
}
