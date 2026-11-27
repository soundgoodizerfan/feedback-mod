// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.weapon.SightCell;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A gun whose sights run on a cell prints the cell's charge as a word: none, flat, low, half or full ({@link #LOW}, {@link #HALF} and
 * {@link #FULL} are the state-of-charge lines between them, glossed on Shift). The charge is the cell's own state of charge, so a worn cell reads lower.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SightCellTooltip {

    private static final float LOW = 0.2f, HALF = 0.5f, FULL = 0.9f;

    private SightCellTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var gun = event.getItemStack();
        if (!SightCell.fitted(gun))
            return;
        var cell = SightCell.cell(gun);
        var state = cell.isEmpty() ? null : BatteryStack.state(cell);
        String word = state == null ? "none" : BatteryStack.charge(state) <= 0f ? "flat" : level(BatteryStack.stateOfCharge(state));
        event.getToolTip().add(Component.translatable("tooltip.feedback.gun.sight_cell", Component.translatable("tooltip.feedback.gun.sight_cell." + word))
                .withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown())
            event.getToolTip().add(Component.translatable("tooltip.feedback.gun.sight_cell.gloss", Math.round(LOW * 100), Math.round(HALF * 100), Math.round(FULL * 100))
                    .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String level(float soc) {
        return soc < LOW ? "low" : soc < HALF ? "half" : soc < FULL ? "most" : "full";
    }
}
