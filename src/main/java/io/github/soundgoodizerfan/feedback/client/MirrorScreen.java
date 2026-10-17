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

import io.github.soundgoodizerfan.feedback.net.FrameMirrorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Chassis Frame's screen: three toggles, one mirror plane each through the middle of the frame
 * block, and the lock that keeps lifts off it. Opened by using the Welder on the frame block itself.
 */
public class MirrorScreen extends Screen {

    private final BlockPos frame;
    private int mirror;
    private boolean locked;

    private MirrorScreen(BlockPos frame, int mirror, boolean locked) {
        super(Component.translatable("screen.feedback.mirror"));
        this.frame = frame;
        this.mirror = mirror;
        this.locked = locked;
    }

    public static void open(BlockPos frame, int mirror, boolean locked) {
        Minecraft.getInstance().setScreen(new MirrorScreen(frame, mirror, locked));
    }

    @Override
    protected void init() {
        int x = width / 2 - 75, y = height / 2 - 40;
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            int bit = 1 << i;
            String axis = axes[i];
            addRenderableWidget(Button.builder(label(axis, bit), b -> {
                mirror ^= bit;
                b.setMessage(label(axis, bit));
                PacketDistributor.sendToServer(new FrameMirrorPayload(frame, mirror, locked));
            }).bounds(x, y + i * 24, 150, 20).build());
        }
        addRenderableWidget(Button.builder(lockLabel(), b -> {
            locked = !locked;
            b.setMessage(lockLabel());
            PacketDistributor.sendToServer(new FrameMirrorPayload(frame, mirror, locked));
        }).bounds(x, y + 76, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 104, 150, 20).build());
    }

    private Component lockLabel() {
        return Component.translatable("screen.feedback.mirror.locked",
                Component.translatable(locked ? "options.on" : "options.off"));
    }

    private Component label(String axis, int bit) {
        return Component.translatable("screen.feedback.mirror." + axis,
                Component.translatable((mirror & bit) != 0 ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.feedback.mirror.hint"), width / 2, height / 2 - 52, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
