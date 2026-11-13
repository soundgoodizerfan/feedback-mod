// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularPartItem;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Raw stock the enchanting table attuned (a rod, a grip) is not a {@link ModularPartItem}, so it names its attunements here. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class AttunedStockTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        MadeOf made = event.getItemStack().get(FDataComponents.MADE_OF.get());
        if (made != null && !(event.getItemStack().getItem() instanceof ModularPartItem))
            ModularPartItem.attuned(made, event.getToolTip());
    }
}
