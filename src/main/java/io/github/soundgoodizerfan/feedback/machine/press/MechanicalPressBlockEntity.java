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
package io.github.soundgoodizerfan.feedback.machine.press;

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
 * One large stamping stroke rather than many light blows -- same {@code Fu}/hardness gate as the
 * Hammer, through the same {@link Operation#BLOW} (a press is not a new physical action on the
 * material, only a different way of applying one, so it shares the Hammer's own deformation
 * chains rather than needing its own). One stroke per {@link #strokesDue}, so it presses as often
 * as it is turned, and a stroke that lands nothing wears the press like a wasted hammer blow.
 * <p>
 * Exists for the same reason the Rolling Mill does: unattended, network-driven throughput against
 * the Hammer's hands-on crank (philosophy §5).
 */
public class MechanicalPressBlockEntity extends ShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.BLOW;

    public MechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MECHANICAL_PRESS.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.MECHANICAL_PRESS_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            St strength = new St(FTuning.MECHANICAL_PRESS_ST.value() * condition);
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
