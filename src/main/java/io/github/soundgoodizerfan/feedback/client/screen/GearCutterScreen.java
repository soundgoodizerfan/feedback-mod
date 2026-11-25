// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import io.github.soundgoodizerfan.feedback.machine.cog.CogSpec;
import io.github.soundgoodizerfan.feedback.net.GearCutterPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Gear Cutter's tooth picker: one button per tooth count, one click sets the next cut. The server is told
 * the count and checks it; the screen holds no authority of its own.
 */
public class GearCutterScreen extends Screen {

    private static final int BUTTON_W = 40, BUTTON_H = 20, GAP = 6;

    private final BlockPos pos;
    private int teeth;

    private GearCutterScreen(BlockPos pos, int teeth) {
        super(Component.translatable("feedback.screen.gear_cutter"));
        this.pos = pos;
        this.teeth = teeth;
    }

    public static void open(BlockPos pos, int teeth) {
        Minecraft.getInstance().setScreen(new GearCutterScreen(pos, teeth));
    }

    @Override
    protected void init() {
        int count = CogSpec.OPTIONS.size();
        int total = count * BUTTON_W + (count - 1) * GAP;
        int x = (width - total) / 2;
        int y = height / 2 - BUTTON_H / 2;
        for (int option : CogSpec.OPTIONS) {
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(option)), b -> pick(option))
                    .bounds(x, y, BUTTON_W, BUTTON_H).build());
            x += BUTTON_W + GAP;
        }
    }

    private void pick(int option) {
        teeth = option;
        PacketDistributor.sendToServer(new GearCutterPayload(pos, option));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("feedback.screen.gear_cutter.current", teeth),
                width / 2, height / 2 + 24, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
