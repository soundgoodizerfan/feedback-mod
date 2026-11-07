// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import net.minecraft.world.item.ItemStack;

/**
 * One unit of cargo on a run: a stack (capped by mass) and where it is along the run. A free unit's
 * position is {@code base + run.phase}, so it needs no per-tick update; a pinned one sits at {@code pin}.
 */
final class Cargo {

    /** Why a pinned unit is pinned. */
    enum Reason { NONE, QUEUE, EVENT, EXIT }

    ItemStack stack;
    double base;
    double pin;
    Reason reason = Reason.NONE;
    /** Index of the next run event (module or slope) this unit has yet to meet. */
    int ev;
    long seed;

    Cargo(ItemStack stack, long seed) {
        this.stack = stack;
        this.seed = seed;
    }

    boolean pinned() {
        return reason != Reason.NONE;
    }

    double pos(double phase) {
        return pinned() ? pin : base + phase;
    }

    void pinAt(double at, Reason why) {
        pin = at;
        reason = why;
    }

    void free(double at, double phase) {
        reason = Reason.NONE;
        base = at - phase;
    }
}
