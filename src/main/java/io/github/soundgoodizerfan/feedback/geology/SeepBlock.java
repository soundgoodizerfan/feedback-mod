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

import io.github.soundgoodizerfan.feedback.item.CopperBucketItem;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * A tar seep (petrochemistry spec 2.5, 3.4): the surface sign of a basin. Bubbles gas, burns, and a
 * copper bucket used on it scoops weathered heavy crude. A scooped seep is dry until a scheduled tick
 * refills it, which is the rate limit ({@value #SCOOP_MB} mB per {@value #REFILL_TICKS} ticks) without
 * a block entity; scooping never draws on the basin, so it does not measurably deplete.
 */
public class SeepBlock extends Block implements io.github.soundgoodizerfan.feedback.core.hazard.Vapour.BurnsOut {

    public static final BooleanProperty SCOOPED = BooleanProperty.create("scooped");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    private static final int SCOOP_MB = 250, REFILL_TICKS = 1200;

    public SeepBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SCOOPED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SCOOPED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
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

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    /** Ignitable hazard: fire catches it and burns it away. */
    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0)
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + random.nextDouble(), pos.getY() + 0.15,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        if (random.nextInt(20) == 0)
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 0.2,
                    pos.getZ() + random.nextDouble(), 0, 0.03, 0);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof CopperBucketItem) || !CopperBucketItem.contentOf(stack).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(SCOOPED)) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.seep.dry"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            FluidStack crude = CrudeGrade.stamp(new FluidStack(FFluids.CRUDE_OIL.get(), SCOOP_MB), CrudeGrade.WEATHERED);
            stack.set(FDataComponents.BUCKET_CONTENT.get(), SimpleFluidContent.copyOf(crude));
            level.setBlock(pos, state.setValue(SCOOPED, true), 3);
            level.scheduleTick(pos, this, REFILL_TICKS);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A seep is open ground: weathered crude gives off a little vapour, and lit it burns dry until it refills. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FluidStack crude = CrudeGrade.stamp(new FluidStack(FFluids.CRUDE_OIL.get(), SCOOP_MB), CrudeGrade.WEATHERED);
        io.github.soundgoodizerfan.feedback.core.hazard.Vapour.source(level, pos,
                io.github.soundgoodizerfan.feedback.core.hazard.Vapour.potency(crude), REFILL_TICKS * 4,
                io.github.soundgoodizerfan.feedback.core.hazard.Vapour.burnTicks(crude) / 4, true);
    }

    @Override
    public void burnedOut(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(SCOOPED, true), 3);
        level.scheduleTick(pos, this, REFILL_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SCOOPED))
            level.setBlock(pos, state.setValue(SCOOPED, false), 3);
    }
}
