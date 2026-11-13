// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringMinigames;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.client.bench.EngineeringTableScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Engineering Table's hand-work games on the client: game id to the screen that plays it. A game screen is a plain
 * {@code Screen} opened over the table's screen ({@code parent}), plays its rounds, sends ONE
 * {@link io.github.soundgoodizerfan.feedback.net.MinigameResultPayload} through {@link #report} and returns to the parent with
 * {@code setScreen(parent)} once its {@code Opening} is shut. Add a game: {@link #register} it below and give the server side an
 * {@link EngineeringMinigames} id.
 */
public final class MinigameScreens {

    /** Builds a game's screen from the table screen, its menu and what the server asked for. */
    interface Opener {
        public Screen open(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session);
    }

    private static final Map<ResourceLocation, Opener> GAMES = new HashMap<>();

    static {
        register(EngineeringMinigames.TORQUE, TorqueScreen::new);
        register(EngineeringMinigames.UPSET, UpsetScreen::new);
        register(EngineeringMinigames.FITTING, FittingScreen::new);
        register(EngineeringMinigames.NOTES, NotesScreen::new);
    }

    private MinigameScreens() {
    }

    static void register(ResourceLocation id, Opener opener) {
        GAMES.put(id, opener);
    }

    public static void open(EngineeringTableScreen parent, MinigameOpenPayload session) {
        ResourceLocation id = ResourceLocation.tryParse(session.game());
        Opener opener = id == null ? null : GAMES.get(id);
        LocalPlayer player = Minecraft.getInstance().player;
        if (opener != null && player != null && session.rounds() > 0)
            Minecraft.getInstance().setScreen(opener.open(parent, parent.getMenu(), session));
    }

    /** Sends the one result of a game: a score in 0..1. */
    static void report(int container, float score) {
        PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.MinigameResultPayload(container,
                Math.round(Math.max(0f, Math.min(1f, score)) * 100f)));
    }
}
