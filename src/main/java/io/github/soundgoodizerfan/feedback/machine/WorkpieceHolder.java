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
package io.github.soundgoodizerfan.feedback.machine;

import net.minecraft.world.item.ItemStack;

/** A machine with one workpiece slot -- the Hammer and every {@link ShopMachineBlockEntity}. */
public interface WorkpieceHolder {

    ItemStack getWorkpiece();

    /** Takes one item off {@code stack} if the slot is clear. */
    boolean insert(ItemStack stack);

    /** Empties the slot; what comes out carries its own heat. */
    ItemStack removeWorkpiece();
}
