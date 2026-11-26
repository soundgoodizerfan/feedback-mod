// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import net.minecraft.world.level.Level;

/**
 * The server's cap on robot program steps in one tick ({@link RobotConfig#STEP_BUDGET}): a robot asks before it steps and, when the tick's
 * steps are spent, waits and asks again next tick, so a big fleet stretches its steps over ticks instead of stalling the server. Game time
 * is shared by every dimension, so the count is one for the whole server. Nothing is stored per robot.
 */
public final class RobotBudget {

    private RobotBudget() {
    }

    private static long tick = -1;
    private static int left;

    /** Ticks a refused robot waits before its step is granted whatever the budget (entity order would otherwise starve the robots last in line). */
    public static final int STARVED_TICKS = 20;

    /** Whether one more program step may run in this tick; {@code waited} is how many ticks in a row this robot has been refused. */
    public static boolean grant(Level level, int waited) {
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            left = RobotConfig.STEP_BUDGET.get();
        }
        if (left <= 0 && waited < STARVED_TICKS)
            return false;
        left--;
        return true;
    }
}
