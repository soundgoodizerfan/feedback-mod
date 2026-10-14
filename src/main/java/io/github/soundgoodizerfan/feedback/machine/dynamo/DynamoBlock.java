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
package io.github.soundgoodizerfan.feedback.machine.dynamo;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * Mechanical to {@code Eu}: a coil turned on a shaft (mechanics 6.8). {@link #FACING} is the
 * input face, toward the shaft it was placed against. {@link #WINDING} is the one choice it
 * offers, flipped by a sneaking empty hand -- the Wire Drawer's convention for a discrete setting.
 */
public class DynamoBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Winding> WINDING = EnumProperty.create("winding", Winding.class);

    /**
     * Two windings that carry the same power at a given speed, so neither dominates (philosophy
     * 3): many turns of fine wire make more volts per RPM and carry fewer amps; a few turns of
     * coarse wire the other way round. One figure each, {@code voltsPerRpm}, used twice -- see
     * {@link DynamoBlockEntity}.
     */
    public enum Winding implements StringRepresentable {
        COARSE("coarse", FTuning.DYNAMO_COARSE_VOLTS_PER_RPM, FTuning.DYNAMO_COARSE_AMPS),
        FINE("fine", FTuning.DYNAMO_FINE_VOLTS_PER_RPM, FTuning.DYNAMO_FINE_AMPS);

        private final String key;
        final float voltsPerRpm;
        final float ratedAmps;

        Winding(String key, float voltsPerRpm, float ratedAmps) {
            this.key = key;
            this.voltsPerRpm = voltsPerRpm;
            this.ratedAmps = ratedAmps;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public DynamoBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WINDING, Winding.COARSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WINDING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            Winding next = state.getValue(WINDING) == Winding.COARSE ? Winding.FINE : Winding.COARSE;
            level.setBlockAndUpdate(pos, state.setValue(WINDING, next));
            player.displayClientMessage(Component.translatable("feedback.dynamo.winding." + next.getSerializedName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamoBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((DynamoBlockEntity) be).tickServer();
    }
}
