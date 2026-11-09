// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.ScrollArea;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;

import net.minecraft.network.chat.Component;

/** The Console app: what the Computer did, newest at the bottom: programs started and stopped, things switched, power lost and back, media moved. */
final class ConsoleWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private final ScrollArea scroll = new ScrollArea();
    private int seen = -1;

    ConsoleWindow(ComputerContext ctx) {
        super(30, 14, 250, 140, 170, 80, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.console");
    }

    @Override
    public Glyph glyph() {
        return Glyph.CONSOLE;
    }

    private static Component text(ComputerBlockEntity.LogEntry e) {
        Component arg = e.key().startsWith("switch") || e.key().equals("refused") ? Component.translatable(e.arg()) : Component.literal(e.arg());
        return Component.translatable("gui.feedback.computer.log." + e.key(), arg);
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ComputerBlockEntity computer = ctx.computer();
        var entries = computer == null ? java.util.List.<ComputerBlockEntity.LogEntry>of() : computer.logEntries();
        if (entries.isEmpty()) {
            ui.wrapped(0, 0, ui.w(), Component.translatable("gui.feedback.computer.log_empty"), skin.dim());
            return;
        }
        if (entries.size() != seen) {
            seen = entries.size();
            scroll.toEnd();
        }
        int y = scroll.begin(ui, 0, 0, ui.w(), ui.h());
        int top = y, width = scroll.innerWidth() - 36;
        for (ComputerBlockEntity.LogEntry e : entries) {
            ui.text(4, y, ComputerScreen.clockText(e.time()), skin.dim());
            y += ui.wrapped(36, y, width, text(e), e.key().equals("power_off") || e.key().equals("refused") ? skin.warn() : skin.ink()) * Ui.LINE;
        }
        scroll.end(ui, y - top + 4);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return scroll.scrolled(lx, ly, wheel);
    }
}
