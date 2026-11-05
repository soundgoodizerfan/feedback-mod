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
package io.github.soundgoodizerfan.feedback.machine.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** A tube station; {@link #FACING} points at the tube it loads into and unloads from. */
public class TubeStationBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final boolean multiStage;

    public TubeStationBlock(Properties properties, boolean multiStage) {
        super(properties);
        this.multiStage = multiStage;
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public boolean multiStage() {
        return multiStage;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TubeStationBlockEntity st))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide) {
                // Plain: where unsorted units go. Sneaking: where units in every band go. Sneaking with
                // the gauge in the other hand is not needed; the reject outlet of a multi-stage station is
                // set by dyeing it while holding the gauge's cycle off hand.
                if (player.isShiftKeyDown() && !st.rules().isEmpty())
                    st.setMatchAddress(dye.getDyeColor());
                else if (hand == InteractionHand.OFF_HAND && st.multiStage())
                    st.setRejectAddress(dye.getDyeColor());
                else
                    st.setAddress(dye.getDyeColor());
                report(player, st);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.SORT_GAUGE.get())) {
            if (!level.isClientSide) {
                st.cycleRule(player.isShiftKeyDown());
                report(player, st);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof CarrierItem i) {
            if (!level.isClientSide) {
                int slot = i.kind() == CarrierItem.Kind.POD ? TubeStationBlockEntity.PODS : TubeStationBlockEntity.CARRIERS;
                ItemStack rest = st.getHandler().insertItem(slot, stack.copy(), false);
                if (!player.isCreative())
                    stack.setCount(rest.getCount());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TubeStationBlockEntity st) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    st.setAddress(null);
                    report(player, st);
                } else if (!(player instanceof ServerPlayer sp && st.board(sp)))
                    report(player, st);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void report(Player player, TubeStationBlockEntity st) {
        var none = Component.translatable("message.feedback.junction.none");
        var rules = Component.empty();
        for (var b : st.rules())
            rules.append(Component.translatable("tube.feedback.band." + b.key())).append(" ");
        player.displayClientMessage(Component.translatable("message.feedback.station",
                st.address().map(c -> Component.translatable("color.minecraft." + c.getName())).orElse(none),
                st.stock(TubeStationBlockEntity.CARRIERS), st.stock(TubeStationBlockEntity.PODS),
                st.rules().isEmpty() ? none : rules,
                st.matchAddress().map(c -> Component.translatable("color.minecraft." + c.getName())).orElse(none),
                st.rejectAddress().map(c -> Component.translatable("color.minecraft." + c.getName())).orElse(none)), true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TubeStationBlockEntity st)
            st.dropAll();
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((TubeStationBlockEntity) be).tickServer();
    }
}
