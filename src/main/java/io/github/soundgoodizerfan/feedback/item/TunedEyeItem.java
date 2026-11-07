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
package io.github.soundgoodizerfan.feedback.item;

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.machine.ender.EnderPortBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.ender.PairBook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A pearl tuned to a bonded pair (ender spec 2.6): used on a port it learns that pair and which end it was tuned at; used in the
 * air it says how far the other end is and which way, wherever it stands, loaded or not ({@link PairBook}). Untuned it does
 * nothing.
 */
public class TunedEyeItem extends Item {

    private static final String PAIR = "Pair", PORT = "Port";

    public TunedEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof EnderPortBlockEntity port) || port.pairId() == null)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(PAIR, port.pairId());
            tag.put(PORT, NbtUtils.writeBlockPos(context.getClickedPos()));
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            context.getPlayer().displayClientMessage(Component.translatable("message.feedback.tuned_eye.tuned"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel server))
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.hasUUID(PAIR)) {
            player.displayClientMessage(Component.translatable("message.feedback.tuned_eye.untuned"), true);
            return InteractionResultHolder.fail(stack);
        }
        UUID pair = tag.getUUID(PAIR);
        BlockPos from = NbtUtils.readBlockPos(tag, PORT).orElse(BlockPos.ZERO);
        PairBook.End end = PairBook.get(server.getServer()).partner(pair, level.dimension(), from);
        if (end == null)
            player.displayClientMessage(Component.translatable("message.feedback.tuned_eye.none"), true);
        else if (!end.dimension().equals(level.dimension()))
            player.displayClientMessage(Component.translatable("message.feedback.tuned_eye.elsewhere"), true);
        else {
            BlockPos d = end.pos().subtract(player.blockPosition());
            Direction way = Direction.getNearest(d.getX(), 0, d.getZ());
            player.displayClientMessage(Component.translatable("message.feedback.tuned_eye.bearing",
                    (int) Math.sqrt(player.blockPosition().distSqr(end.pos())), Component.translatable("direction.feedback." + way.getName()),
                    d.getY()), true);
        }
        return InteractionResultHolder.success(stack);
    }
}
