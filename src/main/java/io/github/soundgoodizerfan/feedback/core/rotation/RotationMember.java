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
package io.github.soundgoodizerfan.feedback.core.rotation;

import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Anything {@link RotationNetwork} counts: what a member supplies, demands and weighs, and the two
 * callbacks the network makes. A {@link RotationNode} is one (a block entity); a micro rotor, one
 * rotation voxel inside a micro cell ({@code docs/specs/feedback_micro_spec.md}, <em>Micro rotation
 * nodes</em>), is the other, and is not a block entity -- the network never needed one.
 */
public interface RotationMember {

    float getRatio();

    void setRatio(float ratio);

    @Nullable
    RotationNetwork getNetwork();

    void setNetwork(@Nullable RotationNetwork network);

    Rpm getGeneratedRpm();

    Su getCapacitySu();

    Su getLoadSu();

    Drag getDragSuPerRpm();

    Inertia getInertia();

    default boolean isSource() {
        return getGeneratedRpm().value() != 0;
    }

    /** This member's actual speed; with no network, the speed it last had (a reload inherits it). */
    Rpm getRpm();

    /** The Su ledger moved. */
    void onNetworkChanged(Su capacitySu, Su loadSu);

    /** The network's speed moved; called every tick while it does, so keep it cheap. */
    void onNetworkSpeedChanged();

    /** Tell clients the speed and angle now, changed or not (after a rebuild). */
    void syncSpeedNow();

    @Nullable
    Level getLevel();

    /** The block this member is in: a node's own, a rotor's cell. */
    BlockPos getBlockPos();

    /** False once this member is no longer what stands in the world (broken, replaced by a reload). */
    boolean isLive();
}
