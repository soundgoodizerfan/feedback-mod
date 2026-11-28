// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.process.CropFeed;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;

/**
 * Ammonium fertiliser: a dose feeds one crop block, which then grows faster and yields more when harvested ripe,
 * until it is harvested ({@link CropFeed}). A dose on a crop that is still fed is more than it can take: the crop
 * scorches, with no harvest.
 */
public class FertiliserItem extends Item {

    public FertiliserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CropBlock))
            return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (CropFeed.isFed(server, pos)) {
                CropFeed.forget(server, pos);
                server.destroyBlock(pos, false);
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
            } else {
                CropFeed.feed(server, pos);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0);
            }
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
