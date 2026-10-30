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
package io.github.soundgoodizerfan.feedback.machine.stockpot;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.core.FTuning;
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
 * add one. A bowl: ladle a serving. A stick: stir and skim (the stirring screen). Empty hand: read it, or take the salt a boiled-dry sea pot left; sneaking, tip it out.
 */
public class StockpotBlock extends BaseEntityBlock {

    public static final MapCodec<StockpotBlock> CODEC = simpleCodec(StockpotBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    /** Who has the stirring screen open: the pot, the last accepted packet, and the last tick one was accepted. */
    private record Session(BlockPos pos, long lastSeen, long lastAccepted) {
    }

    private static final java.util.Map<java.util.UUID, Session> SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long SESSION_IDLE_TICKS = 300;
    private static final long MIN_PACKET_GAP_TICKS = 3;

    /** Whether this player may stir this pot now: a live session, the rod in hand, not faster than one packet per few ticks. Refreshes the session. */
    public static boolean acceptInput(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        Session s = SESSIONS.get(player.getUUID());
        long now = player.level().getGameTime();
        if (s == null || !s.pos().equals(pos) || now - s.lastSeen() > SESSION_IDLE_TICKS
                || !player.getMainHandItem().is(net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        if (now - s.lastAccepted() < MIN_PACKET_GAP_TICKS)
            return false;
        SESSIONS.put(player.getUUID(), new Session(pos, now, now));
        return true;
    }

    public static void endSession(java.util.UUID player) {
        SESSIONS.remove(player);
    }

    public StockpotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        boolean spoon = held.is(net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN);
        var potion = !(held.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem) ? null : held.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
        if (!water && food.isEmpty() && !bowl && !spoon && potion == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (potion != null) {
            if (pot.lace(potion)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
            } else
                player.displayClientMessage(Component.translatable("feedback.stockpot.lace_refused"), true);
        } else if (spoon) {
            SESSIONS.put(player.getUUID(), new Session(pos, level.getGameTime(), level.getGameTime()));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player,
                    new io.github.soundgoodizerfan.feedback.net.PotStatePayload(pos, true, pot.scum(), pot.mixed(), pot.charred(), pot.water()));
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
                player.displayClientMessage(Component.translatable("feedback.stockpot.status", pot.servings(),
                        Component.translatable("tooltip.feedback.food." + doneness)), true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
