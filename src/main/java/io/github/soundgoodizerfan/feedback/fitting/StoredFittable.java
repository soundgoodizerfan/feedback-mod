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
package io.github.soundgoodizerfan.feedback.fitting;

import java.util.List;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/** A {@link Fittable} whose fittings live in a {@link FittingStore}: the holder supplies the store and gets the rest. */
public interface StoredFittable extends Fittable {

    FittingStore fittings();

    @Nullable
    @Override
    default SidedFitting getSidedFitting(Direction side) {
        return fittings().get(side);
    }

    @Override
    default void setSidedFitting(Direction side, @Nullable SidedFitting fitting) {
        fittings().set(side, fitting);
    }

    @Override
    default List<UpgradeFitting> getUpgrades() {
        return fittings().upgrades();
    }

    @Override
    default boolean addUpgrade(UpgradeFitting upgrade) {
        return false;
    }

    @Override
    default void removeUpgrade(UpgradeFitting upgrade) {
        fittings().upgrades().remove(upgrade);
    }
}
