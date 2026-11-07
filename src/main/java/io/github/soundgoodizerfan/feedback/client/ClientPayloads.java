// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;

import net.minecraft.client.Minecraft;

/** Client-side ends of server-to-client payloads that touch client-only classes. */
public final class ClientPayloads {

    private ClientPayloads() {
    }

    /** Routes a link reply to the open Programmer screen, if it is still that Programmer's. */
    public static void programmerLinked(ProgrammerLinkedPayload payload) {
        if (Minecraft.getInstance().screen instanceof ProgrammerScreen screen && screen.getMenu().getPos().equals(payload.pos()))
            screen.applyLink(payload);
    }

    /** A cast wants the view turned: {@link CameraTurn} eases it there. */
    public static void turn(float yaw, float pitch) {
        CameraTurn.aim(yaw, pitch);
    }
}
