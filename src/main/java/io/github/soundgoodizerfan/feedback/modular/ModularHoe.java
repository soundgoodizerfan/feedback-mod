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

/** A hoe that tills like any other; see {@link ModularItem}. */
public class ModularHoe extends net.minecraft.world.item.HoeItem implements Modular {

    public ModularHoe(Item.Properties properties) {
        super(ModularTier.INSTANCE, properties);
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
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        net.minecraft.world.InteractionResult result = super.useOn(context);
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!result.consumesAction() || context.getLevel().isClientSide || player == null || player.isShiftKeyDown())
            return result;
        ModularTraits.of(stack).filter(a -> a.trait() instanceof io.github.soundgoodizerfan.feedback.modular.trait.Kama).ifPresent(a -> {
            for (BlockPos spot : io.github.soundgoodizerfan.feedback.modular.trait.Kama.extra(context, a.params())) {
                if (ModularStacks.broken(stack))
                    break;
                super.useOn(new net.minecraft.world.item.context.UseOnContext(context.getLevel(), player, context.getHand(), stack,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(spot),
                                net.minecraft.core.Direction.UP, spot, false)));
            }
            player.getCooldowns().addCooldown(this, (int) io.github.soundgoodizerfan.feedback.core.Haste.time((long) a.params().get("cooldown_ticks", 20f), player));
        });
        return result;
    }
}
