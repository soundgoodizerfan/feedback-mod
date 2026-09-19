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
package io.github.soundgoodizerfan.feedback.machine.shaft;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Transmits rotation along its axis, and does nothing else.
 * <p>
 * The shaft is not a machine and is deliberately dumb: laying one out is a spatial and budget
 * puzzle (philosophy 3, "keep the shaft, skip the fluid dynamics"), so the block exists and the
 * simulation inside it does not.
 */
public class ShaftBlock extends RotatedPillarBlock implements EntityBlock, Rotatable {

    /** Cap on how far an extend click will scan, so a very long line cannot stall the server. */
    private static final int MAX_EXTEND_SCAN = 64;

    private static final VoxelShape X = box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape Y = box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape Z = box(6, 6, 0, 10, 10, 16);

    private final Drag dragSuPerRpm;
    private final Inertia inertia;

    public ShaftBlock(Properties properties) {
        this(properties, FTuning.SHAFT_DRAG_SU_PER_RPM, FTuning.SHAFT_INERTIA);
    }

    /**
     * A Bearing is this same block with a lower drag -- see {@link
     * io.github.soundgoodizerfan.feedback.registry.FBlocks#BEARING} and {@code
     * FTuning#BEARING_DRAG_SU_PER_RPM}. One class, two constants, the same shape {@link
     * io.github.soundgoodizerfan.feedback.machine.cog.CogBlock} already uses for its two sizes --
     * a physical upgrade you point at, not a shaft-wide multiplier.
     */
    public ShaftBlock(Properties properties, Drag dragSuPerRpm, Inertia inertia) {
        super(properties);
        this.dragSuPerRpm = dragSuPerRpm;
        this.inertia = inertia;
    }

    public Drag getDragSuPerRpm() {
        return dragSuPerRpm;
    }

    public Inertia getInertia() {
        return inertia;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Place along the face the player clicked, so a shaft run extends the way it is pointed.
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    /**
     * Clicking a shaft with a shaft in hand extends the run along its axis, placing at the far
     * end rather than against the face you hit.
     * <p>
     * Pure quality of life, borrowed from Create because laying a shaft line by walking backwards
     * on right click is miserable. Nothing about the mod depends on it.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(asItem()) || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Direction.Axis axis = state.getValue(AXIS);
        // Clicking an end face says which way to grow. Clicking the side of a shaft does not, so
        // grow away from the player -- which is the direction they are almost certainly building.
        Direction growth = hit.getDirection().getAxis() == axis
                ? hit.getDirection()
                : awayFromPlayer(axis, player, pos);

        BlockPos target = pos.relative(growth);
        for (int scanned = 0; scanned < MAX_EXTEND_SCAN; scanned++) {
            BlockState at = level.getBlockState(target);
            if (!at.is(this) || at.getValue(AXIS) != axis)
                break;
            target = target.relative(growth);
        }

        if (!level.getBlockState(target).canBeReplaced())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide) {
            level.setBlockAndUpdate(target, defaultBlockState().setValue(AXIS, axis));
            SoundType sound = getSoundType(state);
            level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Direction awayFromPlayer(Direction.Axis axis, Player player, BlockPos pos) {
        Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal();
        double along = player.position().x * normal.getX()
                + player.position().y * normal.getY()
                + player.position().z * normal.getZ();
        double blockAlong = pos.getX() * normal.getX() + pos.getY() * normal.getY() + pos.getZ() * normal.getZ();
        return Direction.get(along > blockAlong + 0.5
                ? Direction.AxisDirection.NEGATIVE
                : Direction.AxisDirection.POSITIVE, axis);
    }

    /**
     * Kept out of the chunk mesh, because {@code RotatingVisual} draws this block itself and would
     * otherwise put a turning copy on top of a motionless one. With Flywheel's backend off,
     * {@code RotatingRenderer} draws it instead; there is no path where nothing does.
     * <p>
     * {@code ENTITYBLOCK_ANIMATED} rather than {@code INVISIBLE}: the chunk mesh skips both, but
     * only {@code INVISIBLE} also suppresses block-breaking particles, and a shaft that shatters
     * silently is a sense taken away for nothing.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Only the client needs a tick, and only to advance the rendered angle.
        return level.isClientSide ? (l, p, s, be) -> ((ShaftBlockEntity) be).tickClient() : null;
    }
}
