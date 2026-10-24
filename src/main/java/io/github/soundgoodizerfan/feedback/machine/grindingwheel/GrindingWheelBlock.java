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
package io.github.soundgoodizerfan.feedback.machine.grindingwheel;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Operation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The removal primitive's third and hardest-hitting machine, sitting directly on the network
 * like a Gearbox. Right click with an item to insert or swap the workpiece, empty-handed to take
 * it back out -- unchanged from every other machine in this family. Sneak with an empty hand
 * swaps coarse/fine grit.
 */
public class GrindingWheelBlock extends Block implements EntityBlock, Rotatable {

    public static final BooleanProperty COARSE = BooleanProperty.create("coarse");

    /** Material the grit is made of; null is the stone wheel and its flat rating. */
    @Nullable
    private final ResourceLocation grit;

    public GrindingWheelBlock(Properties properties) {
        this(properties, null);
    }

    public GrindingWheelBlock(Properties properties, @Nullable ResourceLocation grit) {
        super(properties);
        this.grit = grit;
        registerDefaultState(defaultBlockState().setValue(COARSE, true));
    }

    /** The wheel's cutting rating: the grit material's own hardness, or the stone wheel's flat figure. */
    public St gritHardness() {
        Material m = grit == null ? null : MaterialTable.get().materials().get(grit);
        return m == null ? FTuning.GRINDING_WHEEL_HARDNESS : m.hardness();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COARSE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof GrindingWheelBlockEntity wheel))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide) {
            if (wheel.getWorkpiece().isEmpty())
                wheel.insert(stack);
            else
                give(player, wheel.removeWorkpiece());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GrindingWheelBlockEntity wheel))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            if (player.isShiftKeyDown())
                level.setBlockAndUpdate(pos, state.setValue(COARSE, !state.getValue(COARSE)));
            else
                give(player, wheel.removeWorkpiece());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GrindingWheelBlockEntity wheel) {
            ItemStack held = wheel.getWorkpiece();
            if (!held.isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("feedback.tooltip.performs", Component.translatable(Operation.GRIND.translationKey())));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrindingWheelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((GrindingWheelBlockEntity) be).tickServer();
    }
}
