// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

/** A block entity that carries a Programmer without being one: the Computer. The editing menu and payloads reach it through here. */
public interface ProgrammerProvider {

    ProgrammerBlockEntity programmer();
}
