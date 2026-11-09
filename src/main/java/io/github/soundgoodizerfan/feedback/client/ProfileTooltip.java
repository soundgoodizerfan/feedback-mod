// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.ProfileTuning;
import io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a weapon is good at, in words: one line of the axes of its {@link WeaponProfile} that pass
 * {@link ProfileTuning#WORD}. Shift glosses each word with the share that earned it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class ProfileTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        WeaponProfile p = WeaponProfile.of(event.getItemStack());
        MutableComponent line = null;
        for (var axis : new Object[][] {{"burst", p.burst()}, {"tempo", p.tempo()}, {"precision", p.precision()}, {"stealth", p.stealth()}}) {
            float share = (float) axis[1];
            if (share < ProfileTuning.WORD)
                continue;
            String name = (String) axis[0];
            Component word = Readout.gloss(Component.translatable("tooltip.feedback.profile." + name), "feedback.gloss.profile." + name,
                    Math.round(share * 100));
            line = line == null ? Component.translatable("tooltip.feedback.profile").append(word) : line.append(Component.literal(", ")).append(word);
        }
        if (line != null)
            event.getToolTip().add(line.withStyle(ChatFormatting.AQUA));
    }
}
