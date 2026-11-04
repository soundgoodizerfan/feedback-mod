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
package io.github.soundgoodizerfan.feedback.modular;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Where parts of one kind of stock are made: the smithing table, the woodworking table and the stonecutter. The Tool Table's
 * code with a {@link Station} filter on the stock. See {@link StationMenu}.
 */
public class StationBlock extends ToolTableBlock {

    public static final MapCodec<StationBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(), Station.CODEC.fieldOf("station").forGetter(b -> b.station)).apply(i, StationBlock::new));

    public final Station station;

    public StationBlock(Properties properties, Station station) {
        super(properties);
        this.station = station;
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape STONECUTTER_SHAPE = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 9, 16);

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return station == Station.STONECUTTING ? STONECUTTER_SHAPE : super.getShape(state, level, pos, context);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (station == Station.STONECUTTING)
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.feedback.stonecutter_wood"));
        else if (station == Station.ANVIL)
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.feedback.anvil_forge"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }
}
