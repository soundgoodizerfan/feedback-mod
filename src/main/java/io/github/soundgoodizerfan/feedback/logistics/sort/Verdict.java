// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.sort;

/** What a detector concluded about one unit against one band. */
public enum Verdict {
    /** The unit is in the band. */
    MATCH,
    /** The unit is outside the band. */
    MISS,
    /** The material table cannot place the unit: the gear's unexpected-cargo ladder decides. */
    UNKNOWN
}
