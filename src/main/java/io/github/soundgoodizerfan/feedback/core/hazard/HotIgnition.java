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
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * A hot piece lying on the ground sets fire to what it touches (philosophy 9 permits consequences anywhere; user
 * ruling, 2026-10-23): a dropped workpiece past {@link FTuning#HOT_IGNITE_TU} lights the air block it lies in when
 * a flammable block is next to it. Checked a few times a second on the heat stamp alone, so the overwhelming
 * majority of items (never heated) cost one component lookup. Not a lock: a hot piece in a hand, a chest or on
 * stone does nothing here.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HotIgnition {

    private HotIgnition() {
    }

    /** Whether a block is an open flame or a lit fire box: what sets a vapour cloud off. By tag and property, never by block. */
    public static boolean isSource(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.FIRE) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                || (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT));
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || item.tickCount % FTuning.HOT_IGNITE_INTERVAL_TICKS != 0)
            return;
        Level level = item.level();
        if (level.isClientSide || item.isInWater() || !item.getItem().has(FDataComponents.TEMPERATURE.get()))
            return;
        if (ItemHeat.get(item.getItem(), level).value() < FTuning.HOT_IGNITE_TU)
            return;
        BlockPos at = item.blockPosition();
        if (!level.getBlockState(at).isAir())
            return;
        for (Direction d : Direction.values()) {
            BlockPos next = at.relative(d);
            if (level.getBlockState(next).isFlammable(level, next, d.getOpposite())
                    && BaseFireBlock.canBePlacedAt(level, at, Direction.UP)) {
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                return;
            }
        }
    }
}
