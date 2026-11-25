// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.client.bench.EngineeringTableScreen;
import io.github.soundgoodizerfan.feedback.client.combat.CameraTurn;
import io.github.soundgoodizerfan.feedback.client.desktop.ComputerScreen;
import io.github.soundgoodizerfan.feedback.client.minigame.MinigameScreens;
import io.github.soundgoodizerfan.feedback.client.screen.ProgrammerScreen;

import net.minecraft.client.Minecraft;

/** Client-side ends of server-to-client payloads that touch client-only classes. */
public final class ClientPayloads {

    private ClientPayloads() {
    }

    /** The Engineering Table asked for a hand-work game: open its screen over the table screen it belongs to. */
    public static void openMinigame(io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload payload) {
        if (Minecraft.getInstance().screen instanceof EngineeringTableScreen screen && screen.getMenu().containerId == payload.container())
            MinigameScreens.open(screen, payload);
    }

    /** An oscilloscope's row for the phosphor screen at the given position: the screen keeps it for its renderer. */
    public static void scopeTrace(io.github.soundgoodizerfan.feedback.net.ScopeTracePayload payload) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(payload.screen()) instanceof io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlockEntity screen)
            screen.setTrace(payload.samples());
    }

    /** A Flight Sensor was used: open the Contraption Diagram for the body it sits on. */
    public static void openDiagram(io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramOpenPayload payload) {
        Minecraft.getInstance().setScreen(new io.github.soundgoodizerfan.feedback.client.diagram.DiagramScreen(payload.sensor()));
    }

    /** The server's figures for the diagram that asked. */
    public static void diagramData(io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramDataPayload payload) {
        io.github.soundgoodizerfan.feedback.client.diagram.DiagramFeed.accept(payload);
    }

    /** The Research Table's trial wants playing. */
    public static void openResearch(io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload payload) {
        if (Minecraft.getInstance().screen instanceof io.github.soundgoodizerfan.feedback.client.bench.ResearchTableScreen screen
                && screen.getMenu().containerId == payload.container())
            MinigameScreens.openResearch(screen, payload);
    }

    /** A cast wants the view turned: {@link CameraTurn} eases it there. */
    public static void turn(float yaw, float pitch) {
        CameraTurn.aim(yaw, pitch);
    }
}
