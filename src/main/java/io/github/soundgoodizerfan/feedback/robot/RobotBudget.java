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

    /** Whether one more program step may run in this tick. */
    public static boolean grant(Level level) {
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            left = RobotConfig.STEP_BUDGET.get();
        }
        if (left <= 0)
            return false;
        left--;
        return true;
    }
}
