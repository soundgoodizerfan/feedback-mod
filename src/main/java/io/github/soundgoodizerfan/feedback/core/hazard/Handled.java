/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
