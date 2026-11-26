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

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A Thaumaturge multiblock that names a block (the golem press's anvil): ours stands in the world where the vanilla one cannot. */
@Pseudo
@Mixin(targets = "com.leclowndu93150.thaumaturge.api.recipe.BlueprintSource$BlockSource", remap = false)
public abstract class BlueprintBlockMixin {

    @Shadow
    @Final
    private Block block;

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true, require = 0)
    private void feedback$acceptsStandIn(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && StandIns.standsFor(state, block))
            cir.setReturnValue(true);
    }
}
