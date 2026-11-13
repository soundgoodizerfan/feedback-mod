// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.List;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * The Settings app: how this Computer looks. Phosphor and wallpaper are tab rows; the CRT effects and the desktop's widgets are check
 * boxes. Every control reads the settings each frame and applies a change at once ({@link ComputerContext#change}); the server keeps it
 * on the block, so it is the same for everyone who opens this Computer.
 */
final class SettingsWindow extends DesktopWindow {

    private static final String[] FLAGS = {"scanlines", "glow", "band", "boot", "clock", "scope", "memory", "icons_right"};

    private final ComputerContext ctx;
    private AbstractWidget phosphor, wallpaper;
    private final AbstractWidget[] flags = new AbstractWidget[FLAGS.length];

    SettingsWindow(ComputerContext ctx) {
        super(40, 10, 250, 168, 220, 150, false);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.settings");
    }

    @Override
    public Glyph glyph() {
        return Glyph.SETTINGS;
    }

    @Override
    protected void build(GuiSkin skin) {
        int w = contentW();
        phosphor = add(GuiStyle.tabs(skin, 0, 0, w, 14,
                List.of(Component.translatable("gui.feedback.computer.phosphor.green"), Component.translatable("gui.feedback.computer.phosphor.amber"),
                        Component.translatable("gui.feedback.computer.phosphor.white"), Component.translatable("gui.feedback.computer.phosphor.blue")),
                () -> ctx.settings().phosphor().ordinal(),
                i -> ctx.change(ctx.settings().with(ComputerSettings.Phosphor.values()[i]))), 0, 10);
        wallpaper = add(GuiStyle.tabs(skin, 0, 0, w, 14,
                List.of(Component.translatable("gui.feedback.computer.wallpaper.grid"), Component.translatable("gui.feedback.computer.wallpaper.scans"),
                        Component.translatable("gui.feedback.computer.wallpaper.circuit"), Component.translatable("gui.feedback.computer.wallpaper.horizon"),
                        Component.translatable("gui.feedback.computer.wallpaper.blank")),
                () -> ctx.settings().wallpaper().ordinal(),
                i -> ctx.change(ctx.settings().with(ComputerSettings.Wallpaper.values()[i]))), 0, 40);
        for (int i = 0; i < FLAGS.length; i++) {
            final int bit = i;
            int column = i % 2, row = i / 2;
            int y = (row < 2 ? 70 : 106) + (row % 2) * 14;
            flags[i] = add(GuiStyle.checkbox(skin, Component.translatable("gui.feedback.computer.setting." + FLAGS[i]),
                    () -> ctx.settings().flag(bit), b -> ctx.change(ctx.settings().toggled(bit)), 0, 0, w / 2 - 4), column * (w / 2), y);
        }
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ui.text(0, 0, Component.translatable("gui.feedback.computer.phosphor"), skin.dim());
        ui.text(0, 30, Component.translatable("gui.feedback.computer.wallpaper"), skin.dim());
        ui.text(0, 60, Component.translatable("gui.feedback.computer.screen"), skin.dim());
        ui.text(0, 96, Component.translatable("gui.feedback.computer.desktop"), skin.dim());
        ui.wrapped(0, 136, ui.w(), Component.translatable("gui.feedback.computer.icons_note"), skin.dim());
    }
}
