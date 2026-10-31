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
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;

/**
 * The Creeper Pen (roadmap 6.6). Use a lead held on a creeper to put it in, feed it a pair of minerals by hand or hopper,
 * shear the sac when it has grown. {@link #STAGE} and {@link #CHARGED} are only the look: 0 empty, 1 creeper, 2 swelling.
 */
public class CreeperPenBlock extends Block implements EntityBlock {

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public CreeperPenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(CHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, CHARGED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CreeperPenBlockEntity pen))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(Items.LEAD) && !pen.occupied()) {
            Creeper held = leashed(level, pos, player);
            if (held == null)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                pen.admit(held.isPowered(), held.getPersistentData().getFloat("FeedbackCharge"),
                        held.getPersistentData().getInt("FeedbackFull"));
                held.dropLeash(true, false);
                held.discard();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!pen.occupied())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(Tags.Items.TOOLS_SHEAR) || stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)) {
            if (!level.isClientSide)
                pen.shear(player, stack, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!stack.isEmpty()) {
            if (!level.isClientSide) {
                int taken = pen.feed(stack);
                if (taken > 0) {
                    if (!player.getAbilities().instabuild)
                        stack.shrink(taken);
                } else
                    player.displayClientMessage(Component.translatable("message.feedback.creeper.wont_eat"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** A creeper leashed to this player within reach of the pen. */
    @Nullable
    private static Creeper leashed(Level level, BlockPos pos, Player player) {
        for (Creeper c : level.getEntitiesOfClass(Creeper.class, new AABB(pos).inflate(8)))
            if (c.getLeashHolder() == player)
                return c;
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CreeperPenBlockEntity pen
                && pen.occupied()) {
            Creeper c = EntityType.CREEPER.create(level);
            if (c != null) {
                c.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                c.getPersistentData().putFloat("FeedbackCharge", pen.charge());
                c.getPersistentData().putInt("FeedbackFull", pen.fullFor());
                if (pen.charged())
                    c.thunderHit((net.minecraft.server.level.ServerLevel) level, new net.minecraft.world.entity.LightningBolt(EntityType.LIGHTNING_BOLT, level));
                level.addFreshEntity(c);
            }
            pen.dropFeed();
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreeperPenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.CREEPER_PEN.get() ? null
                : (l, p, s, be) -> ((CreeperPenBlockEntity) be).tickServer();
    }
}
