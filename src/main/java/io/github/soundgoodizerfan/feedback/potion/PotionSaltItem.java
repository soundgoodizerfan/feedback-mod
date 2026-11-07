// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Crystals left when a vacuum dryer takes the water out of a potion (spec 14.8). They carry the same {@link PotionState} component as a bottle, stamped
 * 0 so the potency never fades. Used on a water source they dissolve into a bottle with {@link PotionTuning#SALT_KEEP} of that potency, give or take.
 */
public class PotionSaltItem extends Item {

    public PotionSaltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (state == null || hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
            return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            float keep = PotionTuning.SALT_KEEP + (level.random.nextFloat() - 0.5f) * PotionTuning.SALT_KEEP_SPREAD;
            PotionState wet = new PotionState(state.effect(), state.amplifier(), state.duration(), state.potency() * keep, "water", state.taste(),
                    level.getGameTime(), AmbientTable.at(level).value(), state.tint());
            ItemStack bottle = new ItemStack(FItems.POTION.get());
            bottle.set(FDataComponents.POTION_STATE.get(), wet);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            if (!player.getInventory().add(bottle))
                player.drop(bottle, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        return state == null || state.dud() ? Component.translatable("item.feedback.potion_salts")
                : Component.translatable("item.feedback.potion_salts.of", PotionEffects.name(state.effect().get()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.feedback.potion_salts").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
