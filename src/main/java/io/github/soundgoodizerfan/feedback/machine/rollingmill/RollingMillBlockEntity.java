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
package io.github.soundgoodizerfan.feedback.machine.rollingmill;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A driven roller pair. Same shape as {@code MechanicalHammerBlockEntity}'s plate-making, through
 * {@link Operation#ROLL}: one pass of {@link FTuning#ROLLING_MILL_ST} per {@link #strokesDue}, the
 * same {@link Deforming#apply} the Hammer and Wire Drawer already share. A pass that cannot move
 * the metal is absorbed by the rolls.
 * <p>
 * Exists alongside the Hammer for a real reason, not a cosmetic one: a mill geared correctly runs
 * unattended off the network, where the Hammer needs either a Clutch or a hand on the crank --
 * capital against attention (philosophy §5), not a second way to make the same plate.
 */
public class RollingMillBlockEntity extends ShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.ROLL;

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROLLING_MILL.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.ROLLING_MILL_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            St strength = new St(FTuning.ROLLING_MILL_ST.value() * condition);
            Deforming.Blow blow = Deforming.apply(workpiece, strength, level, OPERATION);
            if (!blow.landed()) {
                wear(strength.value());
                continue;
            }
            workpiece = blow.result();
            changed = true;
        }
        if (changed)
            sync();
    }
}
