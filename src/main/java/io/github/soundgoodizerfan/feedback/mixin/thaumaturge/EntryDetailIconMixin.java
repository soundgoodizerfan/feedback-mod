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

/** The obtain row of a research page draws the item a player can hold. Client only. */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen", remap = false)
public abstract class EntryDetailIconMixin {

    @Inject(method = "pickRotatingItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$showsStandIn(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(ConvertedIngredients.shown(cir.getReturnValue()));
    }
}
