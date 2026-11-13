// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A modular item with no vanilla behaviour to inherit: the pickaxe, and every custom type. The
 * ones that do inherit some (an axe strips logs) are their own classes, and forward the same
 * hooks to {@link ModularStacks}; this is the shape they all repeat.
 */
public class ModularItem extends Item implements Modular {

    public ModularItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ModularStacks.enchantability(stack, 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ModularStacks.cap(stack, amount);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        ModularStacks.mined(stack, level, state, pos, miner);
        return result;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                        net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.InteractionResultHolder<ItemStack> traited = ModularTraits.use(level, player, hand, player.getItemInHand(hand));
        if (traited.getResult() != net.minecraft.world.InteractionResult.PASS) {
            if (ModularTraits.useDuration(player.getItemInHand(hand)) > 0)
                player.startUsingItem(hand);
            return traited;
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int duration = ModularTraits.useDuration(stack);
        return duration > 0 ? duration : super.getUseDuration(stack, entity);
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        net.minecraft.world.item.UseAnim anim = ModularTraits.useAnimation(stack);
        return anim != net.minecraft.world.item.UseAnim.NONE ? anim : super.getUseAnimation(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        ModularTraits.onUseTick(level, user, stack, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        ModularTraits.releaseUsing(stack, level, user, remaining);
    }
}
