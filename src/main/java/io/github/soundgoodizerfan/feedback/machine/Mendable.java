// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

/** A machine whose misuse wear a {@code mend} part can close: its condition, 1 as built, and a way to give some back. */
public interface Mendable {

    float mendableCondition();

    void mend(float by);
}
