// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;

/** A machine whose wear is a {@code Condition} figure in its {@link FigureStore} (pump, water wheel): an attuned part's mend closes it. */
public interface Figured {

    FigureStore figureStore();
}
