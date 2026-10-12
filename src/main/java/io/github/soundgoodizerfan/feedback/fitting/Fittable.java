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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

/**
 * What it means for a block to accept fittings -- GTCEu's {@code ICoverable}, split for
 * Feedback's three fitting kinds instead of one. See {@code TODO.md} §4d for the read this came
 * from and what was and wasn't kept.
 *
 * <h2>Two storage shapes, because the kinds aren't all sided</h2>
 * A {@link SensorFitting} or {@link AdapterFitting} occupies one face -- at most one of either
 * per {@link Direction}, the same one-per-side rule as a cover. An {@link UpgradeFitting} isn't
 * placed on a face at all, so it lives in a plain list instead.
 *
 * <h2>{@code canMount} is where {@code ThermalBody.hasThermowell()} widens to, not replaces</h2>
 * A sealed vessel already refuses ambient reading via {@code hasThermowell()}; a holder that is
 * also a {@code ThermalBody} should have its {@code canMount} check that flag for
 * {@link SensorFitting}s rather than duplicating the fact in a second place. Nothing in this
 * interface does that wiring itself -- it stays a per-holder decision, the same way GTCEu's
 * {@code canAttach()} is decided by the cover, not by {@code ICoverable}.
 */
public interface Fittable {

    @Nullable
    SidedFitting getSidedFitting(Direction side);

    void setSidedFitting(Direction side, @Nullable SidedFitting fitting);

    default boolean hasSidedFitting(Direction side) {
        return getSidedFitting(side) != null;
    }

    /** Whether this holder will accept the given fitting on the given face. */
    default boolean canMount(SidedFitting fitting, Direction side) {
        return true;
    }

    /**
     * Whether this holder honours the given upgrade -- false by default, because an upgrade
     * attached to a holder that never reads it would be a part that silently does nothing. A
     * holder says yes only for upgrades whose figures it actually folds into its own.
     */
    default boolean canAdd(UpgradeFitting upgrade) {
        return false;
    }

    List<UpgradeFitting> getUpgrades();

    boolean addUpgrade(UpgradeFitting upgrade);

    void removeUpgrade(UpgradeFitting upgrade);

    /** The holder is being broken: every fitting comes off and drops as its item. */
    static void dropAll(Fittable holder, Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            SidedFitting fitting = holder.getSidedFitting(side);
            if (fitting == null)
                continue;
            fitting.onRemoved();
            holder.setSidedFitting(side, null);
            Block.popResource(level, pos, fitting.getPickItem());
        }
        for (UpgradeFitting upgrade : List.copyOf(holder.getUpgrades())) {
            upgrade.onRemoved();
            holder.removeUpgrade(upgrade);
            Block.popResource(level, pos, upgrade.getPickItem());
        }
    }
}
