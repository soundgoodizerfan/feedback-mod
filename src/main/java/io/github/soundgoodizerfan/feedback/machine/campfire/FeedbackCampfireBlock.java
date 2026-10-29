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
package io.github.soundgoodizerfan.feedback.machine.campfire;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Feedback's campfire (the vanilla one is swapped for it, see VanillaVesselSwap). Same block, same
 * look, same lit/unlit and water behaviour; what changes is the food on it. Vanilla looks the food
 * up in a recipe and outputs the cooked form on a timer; here it is a fire at a fixed temperature
 * ({@link FeedbackCampfireBlockEntity}) and the food warms on it through {@link Cooking}.
 * Right-click food to put it on (up to four), right-click anything else to take one off.
 */
public class FeedbackCampfireBlock extends CampfireBlock {

    private final boolean soul;

    public FeedbackCampfireBlock(boolean soul, Properties properties) {
        super(true, soul ? 2 : 1, properties);
        this.soul = soul;
    }

    /** The flame's temperature while lit. */
    public Tu flame() {
        return soul ? FTuning.FIRE_TU_SOUL_CAMPFIRE : FTuning.FIRE_TU_CAMPFIRE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedbackCampfireBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide)
            return state.getValue(LIT) ? createTickerHelper(type, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
        return createTickerHelper(type, BlockEntityType.CAMPFIRE, (l, p, st, be) -> {
            if (be instanceof FeedbackCampfireBlockEntity fire)
                FeedbackCampfireBlockEntity.serverTick(l, p, st, fire);
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof FeedbackCampfireBlockEntity fire))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (Cooking.findCookable(level, held).isPresent() || FeedbackCampfireBlockEntity.airlessInput(held)) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            return fire.put(player, held) ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!held.isEmpty() || !fire.hasFood())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide)
            fire.take(player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        Item item = soul ? Items.SOUL_CAMPFIRE : Items.CAMPFIRE;
        return new ItemStack(item);
    }
}
