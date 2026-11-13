// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.stockpot;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/**
 * A pot on a fire that cooks a batch ({@link StockpotBlockEntity}). Water bucket: fill it. A food:
 * add one. A bowl: ladle a serving. A spoon held aimed at it stirs, a tap skims ({@code ModularSpoon}). Empty hand: read it, or take the salt a boiled-dry sea pot left; sneaking, tip it out (a hot pot burns the hand).
 */
public class StockpotBlock extends BaseEntityBlock {

    public static final MapCodec<StockpotBlock> CODEC = simpleCodec(StockpotBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public StockpotBlock(Properties properties) {
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
        return new StockpotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((StockpotBlockEntity) be).tickServer();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StockpotBlockEntity pot))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean water = held.getItem() instanceof BucketItem bucket && bucket.content.is(FluidTags.WATER);
        var food = Cooking.find(level, held);
        boolean bowl = held.is(io.github.soundgoodizerfan.feedback.registry.FTags.LADLE_CONTAINERS);
        var potion = !(held.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem) ? null : held.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
        // A spoon stirs by a held use and skims by a tap (ModularSpoon); the block only steps aside.
        if (held.is(io.github.soundgoodizerfan.feedback.registry.FTags.SPOON))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!water && food.isEmpty() && !bowl && potion == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (potion != null) {
            if (pot.lace(potion)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
            } else
                player.displayClientMessage(Component.translatable("feedback.stockpot.lace_refused"), true);
        } else if (water) {
            if (pot.addWater(1000f) && !player.getAbilities().instabuild)
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        } else if (food.isPresent() && Material.find(ClientMaterials.on(level), held).filter(l -> l.material().traits().contains(Material.Trait.SALINE)).isPresent()) {
            if (pot.addPinch(food.get()) && !player.getAbilities().instabuild)
                held.shrink(1);
        } else if (food.isPresent()) {
            if (pot.addFood(held, food.get()) && !player.getAbilities().instabuild)
                held.shrink(1);
        } else {
            ItemStack meal = pot.ladle();
            if (meal.isEmpty() && pot.servings() > 0)
                player.displayClientMessage(Component.translatable("feedback.stockpot.thin"), true);
            if (!meal.isEmpty()) {
                if (!player.getAbilities().instabuild)
                    held.shrink(1);
                if (!player.getInventory().add(meal))
                    player.drop(meal, false);
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof StockpotBlockEntity pot) {
            if (player.isShiftKeyDown()) {
                // Tipping a hot pot out with a bare hand burns it; the pot tips anyway.
                if (player.getMainHandItem().isEmpty())
                    io.github.soundgoodizerfan.feedback.core.hazard.HotContact.burn(player, new io.github.soundgoodizerfan.feedback.core.unit.Tu(pot.temperature()));
                pot.empty();
                return InteractionResult.CONSUME;
            }
            int salt = pot.takeSalt();
            if (salt > 0) {
                ItemStack crust = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.SALT.get(), salt);
                if (!player.getInventory().add(crust))
                    player.drop(crust, false);
            } else if (pot.volume() <= 0f)
                player.displayClientMessage(Component.translatable("feedback.stockpot.empty"), true);
            else {
                String doneness = pot.charred() >= 0.5f ? "charred" : pot.cook() >= 1f ? "done" : "raw";
                io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable("feedback.stockpot.status", pot.servings(),
                        Component.translatable("tooltip.feedback.food." + doneness)));
            }
        }
        return InteractionResult.CONSUME;
    }
}
