// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.desktop.ContextMenu;
import io.github.soundgoodizerfan.feedback.client.desktop.Desktop;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The right-click menu of a drive, shared by the drive icons on the wallpaper and the rows of the Files app. */
final class DriveMenu {

    private static final int RUN = ComputerBays.SIZE;

    private DriveMenu() {
    }

    static String letter(int drive) {
        return drive == RUN ? "RUN" : String.valueOf((char) ('A' + drive));
    }

    /** What can be done with the medium in {@code drive}; empty when the bay is empty. */
    static List<ContextMenu.Item> of(ComputerContext ctx, Desktop desktop, int drive) {
        ItemStack medium = ctx.menu().getSlot(drive).getItem();
        if (medium.isEmpty())
            return List.of();
        boolean running = drive == RUN, app = ComputerApps.isApp(medium);
        boolean editing = ctx.computer() != null && ctx.computer().editing();
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.open"), () -> {
            ctx.showDrive(drive);
            desktop.open("drive");
        }));
        if (!running && !app)
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.edit"), () -> {
                ctx.showDrive(drive);
                ctx.send(ComputerActionPayload.EDIT, drive);
                desktop.open("editor");
            }, !editing));
        if (!app)
            items.add(new ContextMenu.Item(Component.translatable(running ? "gui.feedback.computer.stop" : "gui.feedback.computer.run"),
                    () -> ctx.send(running ? ComputerActionPayload.STOP : ComputerActionPayload.RUN, drive), !editing));
        if (!running && !app)
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.copy"),
                    () -> ctx.send(ComputerActionPayload.COPY, drive), ctx.installed("library") && !editing));
        if (!running) {
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.eject"),
                    () -> ctx.send(ComputerActionPayload.EJECT, drive), !editing));
            if (!app)
                items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.menu.erase"), () -> confirmErase(ctx, desktop, drive), !editing));
        }
        return items;
    }

    /** Erasing cannot be taken back, so it asks first. */
    static void confirmErase(ComputerContext ctx, Desktop desktop, int drive) {
        desktop.openMenuAtPointer(List.of(
                new ContextMenu.Item(Component.translatable("gui.feedback.computer.menu.erase_ask", letter(drive)), () -> {
                }, false),
                new ContextMenu.Item(Component.translatable("gui.feedback.computer.menu.erase_yes"),
                        () -> ctx.send(ComputerActionPayload.ERASE, drive)),
                new ContextMenu.Item(Component.translatable("gui.feedback.computer.menu.cancel"), () -> {
                })));
    }
}
