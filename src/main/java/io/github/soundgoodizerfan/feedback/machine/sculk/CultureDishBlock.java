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
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.xp.Culture;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Culture Dish. Use a Culture Seed on an empty dish to start a culture; use a hoe on a ripe one to cut it. The
 * {@link #CULTURE} state is only the look: 0 empty, 1 growing, 2 ripe, 3 ailing.
 */
public class CultureDishBlock extends Block implements EntityBlock {

    public static final IntegerProperty CULTURE = IntegerProperty.create("culture", 0, 3);

    public CultureDishBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CULTURE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CULTURE);
    }

    /** The block state that shows a culture at {@code stage}. */
    static int look(Culture.Stage stage) {
        return switch (stage) {
            case EMPTY -> 0;
            case GROWING -> 1;
            case RIPE -> 2;
            default -> 3;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CultureDishBlockEntity dish))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(FItems.CULTURE_SEED.get()) && !dish.culture().alive()) {
            if (!level.isClientSide) {
                dish.seed();
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ItemTags.HOES) && dish.culture().isRipe()) {
            if (!level.isClientSide) {
                boolean bud = dish.harvest();
                Block.popResource(level, pos.above(), new ItemStack(Items.SCULK, FTuning.CULTURE_HARVEST_BLOCKS));
                Block.popResource(level, pos.above(), new ItemStack(Items.SCULK_VEIN, FTuning.CULTURE_HARVEST_VEINS));
                if (bud)
                    Block.popResource(level, pos.above(), new ItemStack(FItems.CULTURE_SEED.get()));
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof CultureDishBlockEntity dish && dish.xpStore() != null
                && dish.xpStore().points() > 0)
            ExperienceOrb.award(server, Vec3.atCenterOf(pos), dish.xpStore().points());
        super.onRemove(state, level, pos, next, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CultureDishBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.CULTURE_DISH.get() ? null
                : (l, p, s, be) -> ((CultureDishBlockEntity) be).tickServer();
    }
}
