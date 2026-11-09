// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.WorkHardening;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** What a worked piece says about itself: how far it is cold-worked ({@link WorkHardening}), and a filament's texture. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class StrainTooltip {

    private StrainTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int strain = WorkHardening.strain(stack), texture = WorkHardening.texture(stack);
        if (strain <= 0 && texture <= 0)
            return;
        var level = Minecraft.getInstance().level;
        if (strain > 0 && level != null)
            WorkHardening.strained(ClientMaterials.on(level), stack).ifPresent(m ->
                    event.getToolTip().add(Component.translatable("gui.feedback.strain", strain, WorkHardening.limit(m))
                            .withStyle(strain >= WorkHardening.limit(m) ? ChatFormatting.RED : ChatFormatting.GRAY)));
        if (texture > 0)
            event.getToolTip().add(Component.translatable("gui.feedback.texture", texture).withStyle(ChatFormatting.GRAY));
    }
}
