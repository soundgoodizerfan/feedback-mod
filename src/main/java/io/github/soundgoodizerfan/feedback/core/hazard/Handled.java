// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A held thing that is its own hand-guard: the temperature, in Tu, up to which it can be held
 * unhurt by how it is gripped. Unlike {@link Insulator} it protects only against itself, not
 * against whatever is in the other hand -- a shield's strap does not make a hot ingot safe to hold.
 */
public interface Handled {

    float holdsUpToTu(ItemStack stack, Level level);
}
