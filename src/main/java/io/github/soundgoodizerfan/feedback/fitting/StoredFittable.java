// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
