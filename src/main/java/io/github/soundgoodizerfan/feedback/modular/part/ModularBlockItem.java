// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.bake.BlockFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** A fixed block made from parts ({@link BlockFigures}): places as its block, carrying the figures it was baked to. */
public class ModularBlockItem extends BlockItem implements Modular {

    public ModularBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Whether {@code stack} is a fine build: made from a quarter of the parts, a machine for finespace only. */
    public static boolean fine(ItemStack stack) {
        var design = ModularStacks.design(stack);
        return design != null && design.fine();
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = ModularStacks.name(stack, super.getName(stack));
        return fine(stack) ? Component.translatable("item.feedback.fine_build", name) : name;
    }

    /** A fine build stands only as a voxel of finespace (placed by {@code MicroPlacer}, which does not come through here). */
    @Override
    public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (fine(context.getItemInHand())) {
            if (context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.feedback.fine_build.finespace_only"), true);
            return net.minecraft.world.InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (fine(stack))
            tooltip.add(Component.translatable("tooltip.feedback.fine_build").withStyle(net.minecraft.ChatFormatting.AQUA));
        ModularTooltip.add(stack, tooltip);
        if (io.github.soundgoodizerfan.feedback.logistics.store.RackStock.mass(stack) > 0f)
            tooltip.add(Component.translatable("tooltip.feedback.rack_loaded").withStyle(net.minecraft.ChatFormatting.GOLD));
        CompoundTag figs = stack.get(FDataComponents.BLOCK_FIGURES.get());
        if (figs != null)
            for (String key : figs.getAllKeys())
                if (!key.startsWith("_"))
                tooltip.add(key.equals("Condition")
                        ? Component.translatable("figure.feedback.block.Condition", Math.round(figs.getFloat(key) * 100f))
                        : key.equals("Band")
                        ? Component.translatable("figure.feedback.block.Band", Component.translatable(
                                io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.values()[(int) figs.getFloat(key)].key()))
                        : Component.translatable("figure.feedback.block." + key, String.format(Locale.ROOT, "%.3g", figs.getFloat(key))));
    }

    /** A loaded rack destroyed as an item leaves its stock behind, as a shulker box does. */
    @Override
    public void onDestroyed(net.minecraft.world.entity.item.ItemEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!entity.level().isClientSide)
            io.github.soundgoodizerfan.feedback.logistics.store.RackStock.spill(entity.getItem(), entity.level(),
                    entity.getX(), entity.getY(), entity.getZ());
        super.onDestroyed(entity, source);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }
}
