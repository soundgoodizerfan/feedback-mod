// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.differential;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationMember;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationSplit;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A differential: one shaft in at the {@link #FACING} face, two axles out along {@link #OUT}. The
 * propagator couples only the input face, so each axle is a run of its own; this block feeds them
 * ({@link RotationSplit}) so that the two axles' speeds average the input's, and the load decides how
 * the speed is shared. An open differential gives the speed to the axle that resists least (a wheel in
 * the air spins, the one with grip stands still); a limited-slip one lets the shares differ by no more than
 * {@link FTuning#DIFFERENTIAL_SLIP_LIMIT}; a locked one is a solid axle. Which of the three is the
 * block's {@link #MODE}, cycled by a sneak-click: a named choice, not a dial.
 */
public class DifferentialBlock extends Block implements EntityBlock, MicroRotation {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Direction.Axis> OUT = EnumProperty.create("out", Direction.Axis.class);
    /** 0 open, 1 limited slip, 2 locked. */
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);

    public DifferentialBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(OUT, Direction.Axis.X).setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OUT, MODE);
    }

    /**
     * The input is on the face the player clicked against (the shaft they are joining), and the axles
     * go out across the way they are looking, or the other horizontal axis if that is the input's.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction input = context.getClickedFace().getOpposite();
        Direction.Axis look = context.getHorizontalDirection().getAxis();
        Direction.Axis out = look != input.getAxis() ? look : look == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return defaultBlockState().setValue(FACING, input).setValue(OUT, out);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.GEARBOX_INERTIA;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /** Only the input: the axles are fed, not coupled. */
    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    /** The axles' faces, for the part that feeds them. */
    public static boolean isAxle(BlockState state, Direction face) {
        return state.getValue(OUT) == face.getAxis();
    }

    /**
     * One step of the split for the differential {@code self} stands for: reads the two axles' loads,
     * moves the share toward what they ask, and drives them. Returns the share to keep (it is smoothed,
     * so a wheel losing grip hands over its speed over a few ticks rather than at once).
     */
    public static float step(Level level, RotationMember self, BlockState state, float share) {
        RotationNetwork in = self.getNetwork();
        if (in == null)
            return share;
        Direction.Axis axis = state.getValue(OUT);
        RotationMember a = RotationSplit.across(level, self, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        RotationMember b = RotationSplit.across(level, self, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        if (a == null && b == null)
            return share;
        float la = load(a), lb = load(b);
        int mode = state.getValue(MODE);
        float want = 0;
        if (mode != 2 && la + lb > 1e-3f)
            // The axle that resists more gives up speed to the one that resists less.
            want = (lb - la) / (la + lb);
        if (mode == 1)
            want = Math.max(-FTuning.DIFFERENTIAL_SLIP_LIMIT, Math.min(FTuning.DIFFERENTIAL_SLIP_LIMIT, want));
        share += (want - share) * FTuning.DIFFERENTIAL_SHARE_RATE;
        float rpm = self.getRpm().value();
        List<RotationMember> outs = new ArrayList<>(2);
        float[] wants = new float[2];
        if (a != null) {
            wants[outs.size()] = rpm * (1 + share);
            outs.add(a);
        }
        if (b != null) {
            wants[outs.size()] = rpm * (1 - share);
            outs.add(b);
        }
        RotationSplit.drive(in, outs, wants);
        return share;
    }

    private static float load(@Nullable RotationMember member) {
        return member == null || member.getNetwork() == null ? 0 : member.getNetwork().getLoadSu().value();
    }

    /** A sneak-click on a full-size differential steps its mode. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (!level.isClientSide)
            cycle(level, pos, state, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void cycle(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState next = state.setValue(MODE, (state.getValue(MODE) + 1) % 3);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message.feedback.differential.mode." + next.getValue(MODE)), true);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((DifferentialBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((DifferentialBlockEntity) be).tickServer();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DifferentialBlockEntity(pos, state);
    }
}
