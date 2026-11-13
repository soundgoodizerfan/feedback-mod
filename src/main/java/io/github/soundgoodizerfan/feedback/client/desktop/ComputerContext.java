// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerMenu;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerSettings;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;

import org.jetbrains.annotations.Nullable;

/** What the Computer's apps may ask of the desktop they run on: the Computer's state, and the few things an app can make happen. */
interface ComputerContext {

    ComputerMenu menu();

    @Nullable
    ComputerBlockEntity computer();

    /** Sends a {@link io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload} action for {@code drive}. */
    void send(int action, int drive);

    /** The program in the RUN slot, or empty. */
    ProgramGraph running();

    /** The drive icon last opened (0 to 3, or {@code ComputerBays.SIZE} for RUN). */
    int drive();

    void showDrive(int drive);

    ComputerSettings settings();

    /** Applies new settings at once on this screen and sends them to the server. */
    void change(ComputerSettings settings);

    /** Whether a disk with this app is in a bay. */
    boolean installed(String id);

    /** A short notice in the desktop's corner. */
    void toast(net.minecraft.network.chat.Component text);
}
