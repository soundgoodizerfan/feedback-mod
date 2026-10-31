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
package io.github.soundgoodizerfan.feedback.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ammonium fertiliser: a crop that is still growing is pushed on as with bone meal; a crop already ripe is given
 * more than it can take and scorches, with no harvest. Soil nutrient has no state of its own in the crop block, so
 * overdose is read off the one thing a crop does carry, whether it is done growing.
 */
public class FertiliserItem extends Item {

    public FertiliserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop))
            return InteractionResult.PASS;
        if (crop.isMaxAge(state)) {
            if (level instanceof ServerLevel server) {
                server.destroyBlock(pos, false);
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
                context.getItemInHand().consume(1, context.getPlayer());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return BoneMealItem.applyBonemeal(context.getItemInHand(), level, pos, context.getPlayer())
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
}
