// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

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
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;
import net.minecraft.world.item.ArmorItem;

/** A piece of armour; what it gives is on the stack, not in the material vanilla wants it constructed with. */
public class ModularArmor extends net.minecraft.world.item.ArmorItem implements Modular {

    public ModularArmor(ArmorItem.Type type, Item.Properties properties) {
        super(FModular.ARMOUR_MATERIAL, type, properties);
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
        if (entity != null && entity.hasInfiniteMaterials())
            return 0;
        if (entity != null && !entity.level().isClientSide)
            ModularStacks.struck(stack, amount * ModularTuning.COAT_ARMOUR_STRAIN);
        return ModularStacks.cap(stack, amount + (entity == null ? 0 : ModularStacks.rust(stack, entity)));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        ModularStacks.mined(stack, level, state, pos, miner);
        return result;
    }
}
