// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin.thaumaturge;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.attunement.Rites;
import io.github.soundgoodizerfan.feedback.modular.item.ConvertedIngredients;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Thaumaturge research asks for items outside {@code Ingredient}: ours counts where it asks for what Feedback replaces. */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.api.research.ResearchRequirement", remap = false)
public abstract class ResearchRequirementMixin {

    @Shadow
    @Final
    private HolderSet<Item> items;

    @Shadow
    @Final
    private DataComponentPatch components;

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$acceptsStandIn(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ())
            return;
        if (ConvertedIngredients.accepts(asked -> asked.is(items), stack)) {
            cir.setReturnValue(true);
            return;
        }
        // An enchanted book asked for (Silk Touch) is unobtainable: the rite of its attunement stands for it.
        Optional<? extends ItemEnchantments> wanted = components.get(DataComponents.STORED_ENCHANTMENTS);
        if (wanted != null && wanted.isPresent() && items.contains(Items.ENCHANTED_BOOK.builtInRegistryHolder())
                && Rites.standsForBook(stack, wanted.get()))
            cir.setReturnValue(true);
    }
}
