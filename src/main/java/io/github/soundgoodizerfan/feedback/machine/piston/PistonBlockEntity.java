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
package io.github.soundgoodizerfan.feedback.machine.piston;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.linkage.Reciprocating;
import io.github.soundgoodizerfan.feedback.machine.linkage.StrengthPair;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bellows' own crank-linkage mechanism, generalised. Given strokes it pushes; given none it
 * sits still -- but unlike the Bellows, a Piston can be told to stop pushing without stopping the
 * shaft that drives it, because it is {@link Switchable} the same way a Clutch is. That is what
 * makes it a real actuator rather than a second Bellows: a controller can gate it.
 *
 * <h2>Why this is not merely the Bellows renamed</h2>
 * The Bellows only ever has one thing to push into (a firebox) and no reason to ever stop pushing
 * short of stopping the shaft. A Piston's first real job -- the Drill Press's automatic feed --
 * needs the opposite: the shaft keeps turning (driving the bit) while the feed itself starts and
 * stops on command, which is exactly {@code Switchable}'s vocabulary (§13, start or stop a
 * supply) applied to a linear push instead of a rotating one.
 */
public class PistonBlockEntity extends BlockEntity implements Reciprocating, StrengthPair, Switchable {

    public PistonBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PISTON.get(), pos, state);
    }

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(PistonBlock.ENGAGED);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || isEngaged() == engaged)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PistonBlock.ENGAGED, engaged));
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.3f,
                engaged ? 1.1f : 0.8f);
    }

    @Override
    public void onStroke(St strength) {
        if (level == null || level.isClientSide || !isEngaged())
            return;
        if (level.getBlockEntity(worldPosition.relative(getBlockState().getValue(PistonBlock.FACING)))
                instanceof Pushable pushable)
            pushable.push();
    }

    @Override
    public St getStrength(Throw installed) {
        return installed == Throw.LONG ? FTuning.PISTON_ST_LONG : FTuning.PISTON_ST_SHORT;
    }

    @Override
    public St getMaxStrength() {
        return FTuning.PISTON_MAX_ST;
    }

    @Override
    public Su getLoadSu() {
        return FTuning.PISTON_LOAD_SU;
    }
}
