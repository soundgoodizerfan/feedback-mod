// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import java.util.List;

/** A block with {@link HeatedTank}s, so a HUD can read what is hot inside it without knowing which block it is. */
public interface HeatedTanks {

    List<HeatedTank> heatedTanks();
}
