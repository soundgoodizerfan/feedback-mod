// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

/**
 * A menu a player can edit a program through: the Programmer's own, or a Computer's desktop with its editor window. The edit payloads
 * check the player's open menu against this, so both reach the same editing logic.
 */
public interface ProgrammerAccess {

    BlockPos getPos();

    @Nullable
    ProgrammerBlockEntity getProgrammer();
}
