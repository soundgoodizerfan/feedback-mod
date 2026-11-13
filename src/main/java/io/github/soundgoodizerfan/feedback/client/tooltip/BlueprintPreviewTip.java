// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.GunPreview;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

/** A blueprint's tooltip shows its design turning, as the Blueprint Table's drawer does (no controls). */
public record BlueprintPreviewTip(ModularBlueprintItem.Preview preview) implements ClientTooltipComponent {

    private static final int SIZE = 64;

    @Override
    public int getHeight() {
        return SIZE + 4;
    }

    @Override
    public int getWidth(Font font) {
        return SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        float yaw = 90f + System.currentTimeMillis() % 12000L * 0.03f; // modulo on the long: a float of epoch millis has no fraction left to turn by
        GunPreview.drawStack(g, GunPreview.stackOf(preview.design()), x + SIZE / 2f, y + SIZE / 2f + 2, SIZE, yaw, 10f, 100f);
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void register(RegisterClientTooltipComponentFactoriesEvent e) {
            e.register(ModularBlueprintItem.Preview.class, BlueprintPreviewTip::new);
        }
    }
}
