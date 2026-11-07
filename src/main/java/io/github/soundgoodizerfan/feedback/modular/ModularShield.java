// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

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

/** A shield; what it stops is read by {@link ModularEvents#onShieldBlock}. */
public class ModularShield extends net.minecraft.world.item.ShieldItem implements Modular,
        io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.Cooling, io.github.soundgoodizerfan.feedback.core.hazard.Handled {

    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.TuRate coolingRate(ItemStack stack) {
        return ShieldHeat.coolingRate(stack);
    }

    @Override
    public float holdsUpToTu(ItemStack stack, Level level) {
        return ShieldHeat.handlesUpToTu(level, stack);
    }


    public ModularShield(Item.Properties properties) {
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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.InteractionResultHolder<ItemStack> traited = ModularTraits.use(level, player, hand, player.getItemInHand(hand));
        return traited.getResult() == net.minecraft.world.InteractionResult.PASS ? super.use(level, player, hand) : traited;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        ModularTraits.onUseTick(level, user, stack, remaining);
    }
}
