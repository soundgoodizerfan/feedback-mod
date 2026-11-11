// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.resources.ResourceLocation;

/**
 * The hand-work games the Engineering Table's Crafting tab can play instead of completing at once. A recipe's {@code minigame}
 * field or a modular assembly names one by id; the server knows only the id, the number of rounds ("pulls") and the fastener quality
 * (what it tells the client to open) and the fastest an honest play could finish. The screens live on the client
 * ({@code client/MinigameScreens} maps each id to one). Add a game: a constant here, a case in {@link #known} and {@link #minTicks}, and
 * one {@code MinigameScreens.register} call.
 */
public final class EngineeringMinigames {

    /** Hold to wind a gauge and release inside the green band, one pull per fastener spent. */
    public static final ResourceLocation TORQUE = Feedback.id("torque");

    /** Seat the assembly's parts in the order they physically go; a wrong click is a mis-seat that costs score. */
    public static final ResourceLocation FITTING = Feedback.id("fitting");

    private EngineeringMinigames() {
    }

    /** Whether {@code id} names a game the table can play. */
    public static boolean known(String id) {
        return TORQUE.toString().equals(id) || FITTING.toString().equals(id);
    }

    /**
     * The game a modular assembly plays: the type's own {@code minigame} when it names a known one, else torque when the assembly spends
     * fasteners, else none (empty).
     */
    public static String forType(io.github.soundgoodizerfan.feedback.modular.ModularType type, int fasteners) {
        if (known(type.minigame()))
            return type.minigame();
        return fasteners > 0 ? TORQUE.toString() : "";
    }

    /** The rounds an assembly spending {@code fasteners} fasteners and made of {@code pieces} grid parts plays; 0 when the game has nothing to do. */
    public static int rounds(String id, int fasteners, int pieces) {
        if (FITTING.toString().equals(id))
            return pieces >= 2 ? Math.min(pieces, ModularTuning.FIT_MAX_PIECES) : 0;
        if (TORQUE.toString().equals(id))
            return Math.min(fasteners, ModularTuning.TORQUE_MAX_PULLS);
        return 0;
    }

    /** The fewest ticks an honest play of {@code rounds} rounds takes; a result sent sooner is scored as nothing. */
    public static long minTicks(String id, int rounds) {
        if (FITTING.toString().equals(id))
            return (long) rounds * ModularTuning.FIT_MIN_SEAT_TICKS;
        return (long) rounds * ModularTuning.TORQUE_MIN_PULL_TICKS;
    }
}
