// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * The apps that come on disks. A Floppy Disk with the {@code app} component carries one, and the app runs on a Computer exactly while
 * that disk sits in one of its four drive bays: pull the disk and the app's window closes, put it in another Computer and the app is
 * there. The core apps (Drive, Editor, Settings, Monitor, Links, Console) are built in. A disk with an app is not a program medium: the
 * Computer will not edit, run or copy onto it.
 */
public final class ComputerApps {

    /** Every app that needs a disk, in the order the creative tab lists them. */
    public static final List<String> DISK_APPS = List.of("scope", "linkmap", "notes", "converter", "calc", "library", "power", "arcade", "blueprint");

    private ComputerApps() {
    }

    /** The app a stack carries, or null for none (or an id nothing here knows). */
    public static String idOf(ItemStack stack) {
        String id = stack.isEmpty() ? null : stack.get(FDataComponents.APP.get());
        return id != null && DISK_APPS.contains(id) ? id : null;
    }

    public static boolean isApp(ItemStack stack) {
        return idOf(stack) != null;
    }

    /** Whether any of the first {@code bays} slots of {@code container} holds a disk with {@code id}. */
    public static boolean installed(Container container, int bays, String id) {
        for (int i = 0; i < bays; i++)
            if (id.equals(idOf(container.getItem(i))))
                return true;
        return false;
    }

    /** A Floppy Disk carrying the app, named for it. */
    public static ItemStack disk(String id) {
        ItemStack stack = new ItemStack(FItems.FLOPPY_DISK.get());
        stack.set(FDataComponents.APP.get(), id);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.feedback.app." + id));
        return stack;
    }
}
