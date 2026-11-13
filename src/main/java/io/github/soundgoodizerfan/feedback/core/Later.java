// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import io.github.soundgoodizerfan.feedback.Feedback;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runs a task a whole number of server ticks from now (the server's own task queue runs it the same tick, so it cannot delay). */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Later {

    private record Task(int[] ticks, Runnable run) {
    }

    private static final List<Task> QUEUE = new ArrayList<>();

    private Later() {
    }

    /** Run {@code run} on the server thread after {@code ticks} ticks. */
    public static void in(int ticks, Runnable run) {
        synchronized (QUEUE) {
            QUEUE.add(new Task(new int[]{ticks}, run));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        List<Runnable> due = new ArrayList<>();
        synchronized (QUEUE) {
            QUEUE.removeIf(t -> {
                if (--t.ticks[0] > 0)
                    return false;
                due.add(t.run);
                return true;
            });
        }
        due.forEach(Runnable::run);
    }
}
