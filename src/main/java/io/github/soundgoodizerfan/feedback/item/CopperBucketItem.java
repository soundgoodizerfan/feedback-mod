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

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * A bucket of copper: one item that holds any one fluid, up to {@link #CAPACITY_MB}.
 * <p>
 * Not a vanilla {@code BucketItem} -- those are one item per fluid. The contents sit in a data
 * component behind the standard fluid-handler capability (registered in {@code FCapabilities}),
 * so every tank, pump and machine that already takes a bucket takes this one. Use on a source
 * block draws it in; use on empty ground sets it down.
 */
public class CopperBucketItem extends Item {

    public static final int CAPACITY_MB = 1000;

    public CopperBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static SimpleFluidContent contentOf(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.BUCKET_CONTENT.get(), SimpleFluidContent.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player,
                contentOf(stack).isEmpty() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);

        var result = contentOf(stack).isEmpty()
                ? FluidUtil.tryPickUpFluid(stack, player, level, hit.getBlockPos(), hit.getDirection())
                : FluidUtil.tryPlaceFluid(player, level, hand, hit.getBlockPos().relative(hit.getDirection()),
                        stack, contentOf(stack).copy());
        return result.isSuccess()
                ? InteractionResultHolder.sidedSuccess(result.getResult(), level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var content = contentOf(stack);
        tooltip.add(content.isEmpty()
                ? Component.translatable("tooltip.feedback.copper_bucket.empty")
                : Component.translatable("tooltip.feedback.copper_bucket.holds", content.copy().getHoverName(), content.getAmount()));
    }
}
