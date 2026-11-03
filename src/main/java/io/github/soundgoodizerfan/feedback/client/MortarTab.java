/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.OpenMortarPayload;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Mortar tab: a button on the right edge of the player inventory, always shown, that opens the Mortar's bench. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class MortarTab {

    private static Button tab;

    private MortarTab() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            tab = GuiStyle.button(GuiSkin.VANILLA, Component.translatable("gui.feedback.mortar.tab"),
                    b -> PacketDistributor.sendToServer(new OpenMortarPayload()), screen.getGuiLeft() + screen.getXSize() + 2, screen.getGuiTop() + 8, 48, 16);
            event.addListener(tab);
        }
    }

    /** The recipe book moves the panel; the tab follows it. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (tab != null && event.getScreen() instanceof InventoryScreen screen)
            tab.setX(screen.getGuiLeft() + screen.getXSize() + 2);
    }
}
