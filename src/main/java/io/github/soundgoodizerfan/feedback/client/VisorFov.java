// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.trait.ClosedVisor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/**
 * A closed visor is a slot to look through: the wearer's field of view narrows by the trait's {@code fov_mult}. An open face, which gives up
 * protection instead, sees the whole of it. Reads the worn traits only when the player's helmet slot holds a modular piece.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class VisorFov {

    private VisorFov() {
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!(event.getPlayer().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularArmor))
            return;
        for (ModularTraits.Worn w : ModularTraits.worn(event.getPlayer()))
            if (w.trait() instanceof ClosedVisor)
                event.setNewFovModifier(event.getNewFovModifier() * w.params().get("fov_mult", 0.88f));
    }
}
