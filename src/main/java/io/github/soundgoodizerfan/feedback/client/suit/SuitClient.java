// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.suit;

import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.suit.Ability;

/** The client's copy of the wearer's suit, as the server last sent it. Everything the suit's HUD, wheel and views read comes from here. */
public final class SuitClient {

    private SuitClient() {
    }

    private static volatile SuitSyncPayload state = SuitSyncPayload.OFF;
    /** Game time of the last change, for the HUD's slide and the views' fades. */
    private static long changedAt;

    public static void apply(SuitSyncPayload payload) {
        state = payload;
        changedAt = System.nanoTime() / 1_000_000L;
    }

    public static SuitSyncPayload state() {
        return state;
    }

    public static boolean worn() {
        return state.worn();
    }

    public static boolean on(Ability a) {
        return (state.toggles() & a.bit()) != 0;
    }

    public static boolean can(Ability a) {
        return (state.available() & a.bit()) != 0;
    }

    /** An ability that is on and can run: the battery has charge and the heat has not cut the servos out. */
    public static boolean live(Ability a) {
        return state.worn() && on(a) && can(a) && state.battery() && state.charge() > 0f && !state.cut();
    }

    /** The wheel's segments: the five built-in ones always, then each module ability the suit has. */
    public static Ability[] shown() {
        return java.util.Arrays.stream(Ability.values()).filter(a -> a.builtIn() || can(a)).toArray(Ability[]::new);
    }

    public static void clear() {
        state = SuitSyncPayload.OFF;
    }
}
