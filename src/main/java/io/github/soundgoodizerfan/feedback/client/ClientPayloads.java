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
