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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * One block of an assembled frame ({@code docs/feedback_vehicles_spec.md} section 4, <em>cells,
 * again</em>): hidden, placed only on assembly, and lifted into Sable like any other block.
 *
 * <p>Two shapes, because Sable asks two different questions. Its physics collider is baked once
 * per {@link BlockState}, so what the vehicle meets the world with is named by the state: which of
 * the cell's eight half-block {@link #OCTANTS} the frame passes through, 255 shapes of at most
 * eight boxes. Players, the look-at outline and entity collision ask per position, so there the
 * cell's block entity answers with the frame traced at 1/16 ({@link ChassisCellBlockEntity}).
 *
 * <p>{@link #LOAD} carries the cell's share of plate and tube mass to Sable through its
 * {@code physics_block_properties} table, since mass is per state too: {@link #massOf}.
 *
 * <p>Not something a player breaks: plates come off by the Welder, and the cells follow.
 */
public class ChassisCellBlock extends BaseEntityBlock implements BlockSubLevelCollisionShape {

    public static final MapCodec<ChassisCellBlock> CODEC = simpleCodec(ChassisCellBlock::new);

    /** Bit {@code x + 2y + 4z} set: the half-block octant at (x, y, z) is solid. Never zero. */
    public static final IntegerProperty OCTANTS = IntegerProperty.create("octants", 1, 255);
    public static final int LOADS = 16;
    public static final IntegerProperty LOAD = IntegerProperty.create("load", 0, LOADS - 1);
    /** Sable mass at {@code load=0}, kpg; each step up is the square root of two times it. */
    public static final double MASS_AT_ZERO = 0.005;

    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    public ChassisCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OCTANTS, 255).setValue(LOAD, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OCTANTS, LOAD);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** The Sable mass {@code load} stands for, kpg; keep in step with the properties table. */
    public static double massOf(int load) {
        return MASS_AT_ZERO * Math.pow(2, load / 2.0);
    }

    /** The load whose mass is nearest {@code kpg}, by ratio. */
    public static int loadFor(double kpg) {
        int k = (int) Math.round(2 * Math.log(Math.max(kpg, 1e-9) / MASS_AT_ZERO) / Math.log(2));
        return Math.clamp(k, 0, LOADS - 1);
    }

    /** The half-block boxes of {@code octants}. */
    public static VoxelShape octantShape(int octants) {
        VoxelShape s = SHAPES[octants];
        if (s == null) {
            s = Shapes.empty();
            for (int i = 0; i < 8; i++)
                if ((octants & (1 << i)) != 0) {
                    int x = (i & 1) * 8, y = (i >> 1 & 1) * 8, z = (i >> 2 & 1) * 8;
                    s = Shapes.or(s, Block.box(x, y, z, x + 8, y + 8, z + 8));
                }
            SHAPES[octants] = s = s.optimize();
        }
        return s;
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        return octantShape(state.getValue(OCTANTS));
    }

    /**
     * The frame at this position, traced at 1/16 -- or the octants where nothing has traced it,
     * which is also what Sable's state-keyed solidity check sees, so a cell always counts as solid.
     */
    private static VoxelShape fine(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ChassisCellBlockEntity cell && cell.hasShape())
            return cell.shape();
        return octantShape(state.getValue(OCTANTS));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fine(state, level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fine(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChassisCellBlockEntity(pos, state);
    }
}
