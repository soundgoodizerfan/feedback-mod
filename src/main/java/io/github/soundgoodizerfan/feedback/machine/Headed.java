// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

/** A machine whose blow and wear are a replaceable head's: the Mechanical Hammer and Press ({@link HeadSlot}). */
public interface Headed {

    HeadSlot getHead();
}
