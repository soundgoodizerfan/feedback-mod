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
package io.github.soundgoodizerfan.feedback.machine.skillet;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.item.TongsItem;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A pan on a fire (food spec 6, {@code specs/feedback_cooking_inworld_spec.md}). It holds up to four pieces, and
 * the whole interface is what is in the hand: food lays a piece in the quarter the cursor is over; a spatula flips
 * the piece under the cursor; tongs held on the pan lift it off the fire ({@code TongsItem}); a plate or bowl takes a piece off
 * safely; a bare hand takes it and burns if it is hot. Nothing is refused.
 */
public class SkilletBlock extends BaseEntityBlock {

    public static final MapCodec<SkilletBlock> CODEC = simpleCodec(SkilletBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public SkilletBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkilletBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.SKILLET.get() ? null : (l, p, s, be) -> ((SkilletBlockEntity) be).tickServer();
    }

    /** One tick of a food of {@code mass} closing on a pan's heat: a thick piece's core lags longer. Shared with the campfire's grill. */
    public static float warm(float foodTu, float panTu, float mass) {
        return foodTu + (panTu - foodTu) / (io.github.soundgoodizerfan.feedback.core.FTuning.SKILLET_LAG_TICKS * mass);
    }

    /** The quarter of the pan the cursor is over. */
    public static int slot(BlockPos pos, BlockHitResult hit) {
        return SkilletBlockEntity.slotAt(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
    }

    /** The named figure of the spatula blade's shape, else {@code fallback}. */
    private static float bladeParam(ItemStack spatula, String name, float fallback) {
        var design = ModularStacks.design(spatula);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().get(design.type());
        var blade = type == null ? null : design.part(type.primary());
        return blade == null ? fallback : type.roles().get(type.primary()).variant(blade)
                .map(v -> v.params().getOrDefault(name, fallback)).orElse(fallback);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof SkilletBlockEntity pan))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Tongs lift the pan by a held use (TongsItem); the spatula flips a piece with a click.
        if (held.getItem() instanceof TongsItem)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (held.is(FTags.SPATULA)) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            if (!ModularStacks.broken(held)) {
                // The blade's shape is the choice: a wide one turns two pieces at once, a slotted one drains each as it turns.
                int at = slot(pos, hit);
                float drain = bladeParam(held, "drain", 0f);
                boolean turned = pan.flip(at, drain);
                if (bladeParam(held, "flips", 1f) > 1f)
                    turned |= pan.flip(at ^ 1, drain);
                if (turned)
                    held.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
            return ItemInteractionResult.CONSUME;
        }
        boolean plate = held.is(FTags.LADLE_CONTAINERS);
        if (!plate && Cooking.find(level, held).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        int slot = slot(pos, hit);
        if (plate) {
            ItemStack piece = pan.take(slot);
            if (!piece.isEmpty() && !player.getInventory().add(piece))
                player.drop(piece, false);
        } else if (pan.lay(slot, held.copyWithCount(1)) && !player.getAbilities().instabuild)
            held.shrink(1);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof SkilletBlockEntity pan))
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            // Lifting the bare pan: a hot one burns the hand, and that is all that happens.
            HotContact.burn(player, new Tu(pan.panTu()));
            return InteractionResult.CONSUME;
        }
        int slot = slot(pos, hit);
        float tu = pan.takeTu(slot);
        ItemStack piece = pan.take(slot);
        if (!piece.isEmpty()) {
            HotContact.burn(player, new Tu(tu));
            if (!player.getInventory().add(piece))
                player.drop(piece, false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SkilletBlockEntity pan)
            pan.dropContents();
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
