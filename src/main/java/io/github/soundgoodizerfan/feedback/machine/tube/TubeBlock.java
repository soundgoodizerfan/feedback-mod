// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.machine.compressor.CompressorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import org.jetbrains.annotations.Nullable;

/**
 * A pneumatic tube block. The four kinds differ by the part they are: a plain narrow tube, a lined
 * one (lower wall friction), and the two wide-bore kinds that pods ride, plain wide and evacuated.
 * Arms are looks only; the run joins on tube adjacency ({@code TubeNetworks}).
 */
public class TubeBlock extends Block implements EntityBlock {

    public enum Kind {
        STANDARD(false), LINED(false), WIDE(true), EVACUATED(true);

        public final boolean wide;

        Kind(boolean wide) {
            this.wide = wide;
        }
    }

    private static final BooleanProperty[] ARMS = {
            BooleanProperty.create("down"), BooleanProperty.create("up"), BooleanProperty.create("north"),
            BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east")};

    private static final VoxelShape[][] SHAPES = new VoxelShape[2][64];

    private final Kind kind;

    public TubeBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        BlockState state = defaultBlockState();
        for (BooleanProperty arm : ARMS)
            state = state.setValue(arm, false);
        registerDefaultState(state);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMS);
    }

    private static BooleanProperty arm(Direction d) {
        return ARMS[d.get3DDataValue()];
    }

    private static boolean joins(Level level, BlockPos beyond, Direction d) {
        BlockState there = level.getBlockState(beyond);
        if (there.getBlock() instanceof TubeBlock || there.getBlock() instanceof AirSourceBlock
                || there.getBlock() instanceof VacuumPumpBlock)
            return true;
        if (there.getBlock() instanceof TubeStationBlock)
            return there.getValue(TubeStationBlock.FACING) == d.getOpposite();
        if (there.getBlock() instanceof CompressorBlock)
            return there.getValue(CompressorBlock.FACING) == d.getOpposite();
        return level.getCapability(Capabilities.FluidHandler.BLOCK, beyond, d.getOpposite()) != null;
    }

    private BlockState armed(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values())
            state = state.setValue(arm(d), joins(level, pos.relative(d), d));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return armed(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from,
                                   boolean moving) {
        BlockState next = armed(level, pos, state);
        if (next != state)
            level.setBlock(pos, next, 2);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = 0;
        for (Direction d : Direction.values())
            if (state.getValue(arm(d)))
                key |= 1 << d.get3DDataValue();
        int w = kind.wide ? 1 : 0;
        VoxelShape shape = SHAPES[w][key];
        if (shape == null) {
            double lo = kind.wide ? 3 : 5, hi = 16 - lo;
            shape = Block.box(lo, lo, lo, hi, hi, hi);
            for (Direction d : Direction.values())
                if (state.getValue(arm(d))) {
                    double x0 = lo, y0 = lo, z0 = lo, x1 = hi, y1 = hi, z1 = hi;
                    switch (d) {
                        case DOWN -> y0 = 0;
                        case UP -> y1 = 16;
                        case NORTH -> z0 = 0;
                        case SOUTH -> z1 = 16;
                        case WEST -> x0 = 0;
                        case EAST -> x1 = 16;
                    }
                    shape = Shapes.or(shape, Block.box(x0, y0, z0, x1, y1, z1));
                }
            SHAPES[w][key] = shape;
        }
        return shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TubeBlockEntity tube && tube.network() != null) {
            var net = tube.network();
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable("message.feedback.tube.run", net.members.size(),
                    net.ledger.getFluidAmount(), net.volume(), Math.round(net.pressure().value() * 10f) / 10f));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TubeBlockEntity tube)
            tube.spill();
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }
}
