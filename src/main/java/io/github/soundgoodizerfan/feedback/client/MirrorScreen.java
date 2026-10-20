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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Chassis Frame's screen: three toggles, one mirror plane each through the middle of the frame
 * block, and the lock that keeps lifts off it. Opened by using the Welder on the frame block itself.
 * An instrument panel ({@link GuiSkin#PANEL}): each setting a lamp and a switch.
 */
public class MirrorScreen extends Screen {

    private static final int W = 190, H = 150;
    private static final GuiSkin SKIN = GuiSkin.PANEL;

    private final BlockPos frame;
    private final Opening opening = new Opening(SKIN);
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

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        opening.start();
        int x = left() + 12, y = top();
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            int bit = 1 << i;
            addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.mirror.across", axes[i].toUpperCase(java.util.Locale.ROOT)),
                    () -> (mirror & bit) != 0, b -> {
                        mirror ^= bit;
                        send();
                    }, x, y + 54 + i * 16, W - 24));
        }
        addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.mirror.lock"), () -> locked, b -> {
            locked = !locked;
            send();
        }, x, y + 110, W - 24));
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                left() + W / 2 - 30, y + H - 22, 60, 14));
    }

    private void send() {
        PacketDistributor.sendToServer(new FrameMirrorPayload(frame, mirror, locked));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, H, () -> {
            SKIN.panel(g, x, y, W, H);
            Component t = Component.literal(title.getString().toUpperCase(java.util.Locale.ROOT));
            g.drawString(font, t, x + (W - font.width(t)) / 2, y + 10, SKIN.ink(), false);
            int ly = y + 22;
            for (var l : font.split(Component.translatable("screen.feedback.mirror.hint"), W - 24)) {
                g.drawString(font, l, x + (W - font.width(l)) / 2, ly, SKIN.dim(), false);
                ly += 9;
            }
            g.drawString(font, Component.translatable("screen.feedback.mirror.planes"), x + 12, y + 44, SKIN.dim(), false);
            g.fill(x + 12, y + 105, x + W - 12, y + 106, 0x40D8DCD8);
            super.render(g, mouseX, mouseY, partialTick);
        });
    }

    /** The dimming is the opening's. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !opening.settled() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
