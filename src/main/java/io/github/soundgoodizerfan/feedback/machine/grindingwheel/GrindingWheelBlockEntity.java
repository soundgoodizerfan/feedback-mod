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
package io.github.soundgoodizerfan.feedback.machine.grindingwheel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same removal primitive as the Drill Press and the Lathe, with the wheel's own hardness rating
 * set above both of theirs -- the only route through a material like hardened tool steel, not a
 * finishing upgrade over them (see {@link FTuning#GRINDING_WHEEL_HARDNESS}).
 *
 * <h2>The one real exception to wear-only-from-misuse</h2>
 * The Hammer and the Lathe's tool bit both wear <em>only</em> from misuse -- force or a cut the
 * tool cannot use, absorbed rather than wasted for free. This wheel is different on purpose: grit
 * is genuinely consumed by correct use, the same way a real abrasive wheel wears down whether or
 * not the operator made a mistake. So {@link #tickServer} costs condition on every <em>landed</em>
 * grind, not only on a failed one, and its floor is zero rather than the Hammer's 0.5 -- a bald
 * wheel's effective hardness is also zero, which is a genuine hard gate (§7) rather than a machine
 * silently refusing to run.
 *
 * <h2>The thermal tie-in is the differentiator landing a third time</h2>
 * Grinding produces real interface heat. {@link #tickServer} bumps the workpiece's own {@code
 * ItemHeat} proportionally to Fu removed -- and keeps heating whatever it rubs and cannot cut, so
 * overrun grinding heats too -- no new mechanic, since a workpiece is hot wherever it
 * is regardless of what heated it (philosophy 9). Overrun grinding a part that was deliberately
 * hardened can walk its temperature back into a tempering-relevant range and soften it, which
 * needed nothing new to be written for it to happen.
 */
public class GrindingWheelBlockEntity extends ShopMachineBlockEntity {

    public GrindingWheelBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GRINDING_WHEEL.get(), pos, state);
    }

    public boolean isCoarseGrit() {
        return getBlockState().getValue(GrindingWheelBlock.COARSE);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.GRINDING_WHEEL_LOAD_SU;
    }

    private St toolHardness() {
        float rating = getBlockState().getBlock() instanceof GrindingWheelBlock w
                ? w.gritHardness().value() : FTuning.GRINDING_WHEEL_HARDNESS.value();
        return new St(rating * condition);
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty())
            return;
        float passes = passesDue();
        if (passes <= 0)
            return;

        boolean coarse = isCoarseGrit();
        St driveForce = coarse ? FTuning.GRINDING_WHEEL_ST_COARSE : FTuning.GRINDING_WHEEL_ST_FINE;
        Removing.Cut cut = Removing.apply(workpiece, driveForce, toolHardness(), Operation.GRIND,
                passes, carriedFu, level);
        carriedFu = cut.carried();

        // A wheel rubbing something it cannot cut still heats it -- as if every pass were Fu.
        float fu = cut.landed() ? cut.delivered() : driveForce.value() * passes;
        if (cut.landed()) {
            workpiece = cut.result();
            float wearPerFu = coarse ? FTuning.GRINDING_WHEEL_WEAR_PER_FU_COARSE : FTuning.GRINDING_WHEEL_WEAR_PER_FU_FINE;
            condition = Math.max(FTuning.GRINDING_WHEEL_CONDITION_FLOOR, condition - fu * wearPerFu);
            maybeDropSwarf(cut);
        }

        Tu heated = ItemHeat.get(workpiece, level);
        ItemHeat.set(workpiece, new Tu(heated.value() + fu * FTuning.GRINDING_HEAT_PER_FU), level);
        sync();
    }
}
