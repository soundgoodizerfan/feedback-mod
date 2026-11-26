// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin.thaumaturge;

import io.github.soundgoodizerfan.feedback.modular.item.ConvertedIngredients;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Thaumaturge research asks for items outside {@code Ingredient}: ours counts where it asks for what Feedback replaces. */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.api.research.ResearchRequirement", remap = false)
public abstract class ResearchRequirementMixin {

    @Shadow
    @Final
    private HolderSet<Item> items;

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$acceptsStandIn(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ConvertedIngredients.accepts(asked -> asked.is(items), stack))
            cir.setReturnValue(true);
    }
}
