// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A module for a belt cell: use it on a belt to fit it into that cell's space (one per cell). */
public class BeltModuleItem extends Item {

    private final BeltModule module;

    public BeltModuleItem(BeltModule module, Properties properties) {
        super(properties);
        this.module = module;
    }

    public BeltModule module() {
        return module;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel().getBlockEntity(pos) instanceof ModuleHost belt) || belt.module() != null
                || (belt instanceof BeltHeadBlockEntity && !module.onHead()))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            Direction side = context.getClickedFace();
            // A module acting beside the cell turns to the player's right when the click was on the top or bottom,
            // except a funnel or chute, which may open straight up or down.
            if (side.getAxis() == Direction.Axis.Y && module != BeltModule.FUNNEL_IN && module != BeltModule.FUNNEL_OUT && module != BeltModule.CHUTE)
                side = context.getHorizontalDirection().getClockWise();
            belt.install(module, side);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
