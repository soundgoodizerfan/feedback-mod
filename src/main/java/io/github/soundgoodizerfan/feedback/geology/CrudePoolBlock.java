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
package io.github.soundgoodizerfan.feedback.geology;

import io.github.soundgoodizerfan.feedback.core.hazard.Vapour;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Spilled crude (petrochemistry spec 4.3): a thin film where a wellhead blew out. It gives off vapour by
 * the crude's light fraction (a fresh basin's spill is far worse than a weathered one) and, lit, burns
 * for the crude material's {@code burn_ticks} and is gone. Its own scheduled tick keeps the source alive.
 * <p>
 * shortcut: no spread, no volume (a pool is one block however much was lost), never dries; the spill
 * grade is {@link CrudeGrade#DEFAULT}. Upgrade when a spill should carry a stamped grade.
 */
public class CrudePoolBlock extends Block implements Vapour.BurnsOut {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private static final int REFRESH_TICKS = 100;

    public CrudePoolBlock(Properties properties) {
        super(properties);
    }

    /** Lays a pool at {@code pos} if it is air or replaceable with a sturdy floor. */
    public static boolean place(ServerLevel level, BlockPos pos) {
        BlockState there = level.getBlockState(pos);
        BlockState pool = FBlocks.CRUDE_POOL.get().defaultBlockState();
        if (!there.canBeReplaced() || !there.getFluidState().isEmpty() || !pool.canSurvive(level, pos))
            return false;
        level.setBlockAndUpdate(pos, pool);
        return true;
    }

    private void register(ServerLevel level, BlockPos pos) {
        FluidStack crude = CrudeGrade.stamp(new FluidStack(FFluids.CRUDE_OIL.get(), 1000), CrudeGrade.DEFAULT);
        Vapour.source(level, pos, Vapour.potency(crude), REFRESH_TICKS * 3, Vapour.burnTicks(crude), true);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level instanceof ServerLevel server) {
            register(server, pos);
            level.scheduleTick(pos, this, REFRESH_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        register(level, pos);
        level.scheduleTick(pos, this, REFRESH_TICKS);
    }

    @Override
    public void burnedOut(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
}
