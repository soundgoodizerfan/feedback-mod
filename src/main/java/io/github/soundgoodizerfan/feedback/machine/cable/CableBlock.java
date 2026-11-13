// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cable;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A length of wire: one block, connecting on every face to anything electrical it touches.
 *
 * <h2>Made of a material, not of a name</h2>
 * One block serves every metal: the material rides on the item ({@code MadeOf}) and lives in the
 * block entity, and every figure that matters -- how many amps it carries, what it loses per amp,
 * how hot it may get -- is read from that material's file. A new conductor is a
 * material file and nothing else. Registered as {@code cable}, not {@code wire}: wire is a form
 * (the stuff a cable is made from) and the two must not be told apart by anything but their id.
 *
 * <h2>The shape is a core, and you can walk through it</h2>
 * A wire run along a floor is not an obstacle. It is also not a full block, because a cube that
 * looks like a block and carries current is a trap that teaches nothing. A bare conductor shocks
 * (stage 1) -- {@link #entityInside} -- which is why it has to be passable: standing
 * in one is how you find out. A bare one also arcs to a bare one an air block away, which
 * {@link #neighborChanged} keeps honest when the gap fills or empties.
 */
public class CableBlock extends Block implements EntityBlock {

    private static final BooleanProperty[] ARMS = {
            BooleanProperty.create("down"), BooleanProperty.create("up"), BooleanProperty.create("north"),
            BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east")};

    /** The gauge: block state because it changes the look and the shape; the block entity reads it for rating and drop. */
    public static final EnumProperty<CableGauge> GAUGE = EnumProperty.create("gauge", CableGauge.class);

    private static final VoxelShape[] SHAPES = new VoxelShape[64 * CableGauge.values().length];

    /**
     * Core and arms of a gauge, matching the models: the core is {@code w} wide each way except
     * {@code t} up and down; an arm north or south is {@code w} by {@code t}, east or west likewise
     * rotated, up or down {@code w} by {@code t} in x and z. Arms start inside the core so none leave a gap.
     */
    private static VoxelShape shape(CableGauge gauge, int key) {
        double w = gauge.halfWidth(), t = gauge.halfThick();
        VoxelShape shape = Block.box(8 - w, 8 - t, 8 - w, 8 + w, 8 + t, 8 + w);
        VoxelShape[] arms = {
                Block.box(8 - w, 0, 8 - t, 8 + w, 8 - t, 8 + t), Block.box(8 - w, 8 + t, 8 - t, 8 + w, 16, 8 + t),
                Block.box(8 - w, 8 - t, 0, 8 + w, 8 + t, 8 - t), Block.box(8 - w, 8 - t, 8 + t, 8 + w, 8 + t, 16),
                Block.box(0, 8 - t, 8 - w, 8 - t, 8 + t, 8 + w), Block.box(8 + t, 8 - t, 8 - w, 16, 8 + t, 8 + w)};
        for (int i = 0; i < 6; i++)
            if ((key & (1 << i)) != 0)
                shape = Shapes.or(shape, arms[i]);
        return shape;
    }

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(GAUGE, CableGauge.STANDARD);
        for (BooleanProperty arm : ARMS)
            state = state.setValue(arm, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMS);
        builder.add(GAUGE);
    }

    protected static BooleanProperty arm(Direction d) {
        return ARMS[d.get3DDataValue()];
    }

    /** An arm towards every adjacent electrical member. Looks only: the run joins through {@code ElectricPropagator}. */
    private static BlockState armed(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values())
            state = state.setValue(arm(d), level.getBlockEntity(pos.relative(d)) instanceof ElectricNode);
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        CableGauge gauge = context.getItemInHand().getOrDefault(FDataComponents.CABLE_GAUGE.get(), CableGauge.STANDARD);
        return armed(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(GAUGE, gauge));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = 0;
        for (Direction d : Direction.values())
            if (state.getValue(arm(d)))
                key |= 1 << d.get3DDataValue();
        CableGauge gauge = state.getValue(GAUGE);
        int slot = gauge.ordinal() * 64 + key;
        VoxelShape shape = SHAPES[slot];
        if (shape == null)
            SHAPES[slot] = shape = shape(gauge, key);
        return shape;
    }

    /**
     * A bare conductor under load hurts whatever is touching it. Throttled to twice a second so
     * it is a hazard and not a death trap; the entity is hurt only while insulation is gone and
     * current is actually flowing.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.tickCount % 10 != 0)
            return;
        if (level.getBlockEntity(pos) instanceof CableBlockEntity wire)
            Shock.apply(entity, wire.touchBand());
    }

    /**
     * A block appearing or vanishing in the air gap between two bare wires makes or breaks their arc.
     * The gap block is adjacent to both wires, so each hears it here; only a change in a gap that
     * has a bare wire across it queues a rebuild, so ordinary neighbour updates cost one lookup.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, movedByPiston);
        BlockState next = armed(level, pos, state);
        if (next != state)
            level.setBlock(pos, next, 2);
        if (level.isClientSide || level.getBlockEntity(fromPos) != null
                || !(level.getBlockEntity(pos) instanceof CableBlockEntity wire) || !wire.bare())
            return;
        BlockPos far = fromPos.offset(fromPos.getX() - pos.getX(), fromPos.getY() - pos.getY(), fromPos.getZ() - pos.getZ());
        if (level.getBlockEntity(far) instanceof CableBlockEntity other && other.bare())
            ElectricPropagator.requeue(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CableBlockEntity cable)
            Fittable.dropAll(cable, level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
}
