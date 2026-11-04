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
package io.github.soundgoodizerfan.feedback.machine.creative;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A development block: right click it and it says how much heat reaches it, which source on which side gives it, and at what share.
 * It reads through {@link HeatSource#at}, exactly as a vessel does, so what it prints is what a vessel in its place would feel.
 * Operator tab only.
 */
public class HeatProbeBlock extends Block {

    public HeatProbeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        float ambient = AmbientTable.at(level).value();
        player.displayClientMessage(Component.translatable("feedback.debug.heat_probe.total",
                Math.round(HeatSource.at(level, pos).value()), Math.round(ambient)), false);
        for (Direction dir : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(dir)) instanceof HeatSource source))
                continue;
            float share = source.shareToward(dir.getOpposite());
            player.displayClientMessage(Component.translatable("feedback.debug.heat_probe.source", dir.getName(),
                    level.getBlockState(pos.relative(dir)).getBlock().getName(), Math.round(source.getFireTu().value()),
                    Math.round(share * 100f), source.heatFacing().getName()), false);
        }
        if (level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.LAVA))
            player.displayClientMessage(Component.translatable("feedback.debug.heat_probe.lava"), false);
        return InteractionResult.CONSUME;
    }
}
