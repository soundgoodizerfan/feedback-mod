// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.LevitationSleeveFitting;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A sleeve of a diamagnetic material. Used on a shaft, flywheel or rotary bearing it is fitted (one a holder); the holder decides
 * whether it honours one, nothing here knows what a shaft is. The material rides on the stack and passes to the fitting.
 */
public class LevitationSleeveItem extends Item {

    public static final String FORM = "levitation_sleeve";

    public LevitationSleeveItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.LEVITATION_SLEEVE.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, FORM, 1));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? super.getName(stack) : io.github.soundgoodizerfan.feedback.item.FormBlock.name(made.material(), FORM);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null || !(be instanceof Fittable fittable))
            return InteractionResult.PASS;
        LevitationSleeveFitting fitting = new LevitationSleeveFitting(made.material());
        if (!fittable.canAdd(fitting))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            fittable.addUpgrade(fitting);
            fitting.onAttached();
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
