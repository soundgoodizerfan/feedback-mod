// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.mixin.thaumaturge;

import io.github.soundgoodizerfan.feedback.modular.item.StandIns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.state.BlockState;

/** As {@link BlueprintBlockMixin}, where the multiblock names a whole state (the golem press's piston facing up). */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.api.recipe.BlueprintSource$StateSource", remap = false)
public abstract class BlueprintStateMixin {

    @Shadow
    @Final
    private BlockState state;

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$acceptsStandIn(BlockState placed, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && StandIns.standsFor(placed, state))
            cir.setReturnValue(true);
    }
}
