/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
        if (!(context.getLevel().getBlockEntity(pos) instanceof BeltBlockEntity belt) || belt.module() != null)
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
