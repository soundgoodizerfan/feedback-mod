// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin.thaumaturge;

import io.github.soundgoodizerfan.feedback.modular.item.ConvertedIngredients;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;

/** Thaumaturge's stack matching (golem components, filters): ours counts where the reference is what Feedback replaces. */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.api.items.ItemMatch", remap = false)
public abstract class ItemMatchMixin {

    @Inject(method = "test", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$acceptsStandIn(ItemStack reference, ItemStack candidate, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !reference.isEmpty()
                && ConvertedIngredients.accepts(asked -> asked.is(reference.getItem()), candidate))
            cir.setReturnValue(true);
    }
}
