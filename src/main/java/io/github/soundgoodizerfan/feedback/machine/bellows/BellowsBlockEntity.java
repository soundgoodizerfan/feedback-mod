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
package io.github.soundgoodizerfan.feedback.machine.bellows;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.linkage.Reciprocating;
import io.github.soundgoodizerfan.feedback.machine.linkage.StrengthPair;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.St;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Squeezes. That is the entire device.
 *
 * <h2>It has no target and no thermometer</h2>
 * Given strokes it moves air; given none it stops. It cannot be told a temperature, it cannot read
 * one, and it is not {@link io.github.soundgoodizerfan.feedback.control.Switchable} -- stopping it means
 * stopping the shaft that drives it, which is what the clutch is for. That is philosophy 10 taken
 * literally: measurement, control and actuation are three separate systems, and the player is the
 * one who wires them together.
 * <p>
 * It is also the slice's <b>only</b> actuator, and it points one way. Nothing removes heat.
 * Overshoot the window and there is nothing to do but wait, on a vessel chosen for stability and
 * therefore equally stubborn about coming back down. The player cannot ask for cooling; they can
 * only stop asking for heat. Slice 2's damper is earned by withholding it.
 *
 * <h2>The same part, meaning the opposite thing</h2>
 * The player met the crank linkage on the hammer, where the short throw is the strong option
 * because a lever trades distance for force. A bellows wants displaced air rather than force, so
 * the <b>long</b> throw is the useful one here. A component that composes two ways is the first
 * time in the slice that a crank stops being a required adapter and starts being a choice.
 *
 * <h2>Why gearing buys nothing here, which is not a restriction</h2>
 * The linkage multiplies a machine's stated force by the gear advantage and clamps to the
 * machine's ceiling. This machine's ceiling <em>is</em> its long throw, so the clamp bites at once
 * and no gear train ever moves more air per stroke. That is the physics rather than a rule: a
 * bellows holds what it holds, and squeezing it harder does not find more air inside it. So
 * gearing buys force on the hammer and stroke rate here, out of one mechanism, because a ceiling
 * is a property of the machine.
 */
public class BellowsBlockEntity extends BlockEntity implements Reciprocating, StrengthPair {

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BELLOWS.get(), pos, state);
    }

    /**
     * {@code strength} is air here, in the same units the fire consumes it.
     * <p>
     * The interface calls it strength because on the hammer that is what it is. Renaming it to
     * something neutral was tempting and would have been worse: the whole point of a linkage is
     * that it delivers one physical quantity per stroke and the machine decides what that quantity
     * means to it. A second name would imply a second kind of stroke.
     */
    @Override
    public void onStroke(St strength) {
        if (level == null || level.isClientSide)
            return;

        // The linkage delivers what the drive put behind the stroke, ceiling and all, so the
        // clamp lives here. The surplus simply vanishes and nothing is damaged by it: a bellows
        // is already fully compressed at its long throw, and squeezing an empty bag harder finds
        // no more air and breaks nothing. That is the opposite of what the hammer does with its
        // surplus, out of the same stroke, which is why the ceiling belongs to the machine.
        float air = Math.min(strength.value(), getMaxStrength().value());

        // One stroke is one bagful, shared among whatever it blows into -- not a full bagful each.
        List<Blown> targets = new ArrayList<>(6);
        for (Direction face : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(face)) instanceof Blown blown)
                targets.add(blown);
        for (Blown blown : targets)
            blown.addAir(air / targets.size());
        boolean blew = !targets.isEmpty();

        // A bellows squeezing at nothing still makes the noise. It has no way to know it is
        // pointed at a wall, and a machine that fell silent when it was useless would be telling
        // the player something the machine cannot possibly know.
        level.playSound(null, worldPosition, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundSource.BLOCKS,
                blew ? 0.5f : 0.35f, 0.7f);
    }

    /**
     * <h3>[OPEN] The bellows answers a question about force with a volume of air</h3>
     * {@link StrengthPair} is typed in {@link St} because that is what a linkage delivers, and
     * this returns an air figure through it at one-to-one. Typing the units is what made that
     * visible -- it was invisible while both were {@code float} -- and it is left standing rather
     * than papered over, because the honest fix is a decision the units table has not taken: air
     * has no unit in philosophy 17 at all. Either it gains one (a volume, so {@code mB}, with a
     * stated conversion from the force compressing the bag) or the firebox is re-expressed in a
     * unit that already exists. Until then the numbers are right and the naming is not.
     */
    @Override
    public St getStrength(Throw installed) {
        return new St(installed == Throw.LONG
                ? FTuning.BELLOWS_AIR_LONG
                : FTuning.BELLOWS_AIR_SHORT);
    }

    @Override
    public St getMaxStrength() {
        return new St(FTuning.BELLOWS_MAX_AIR);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.BELLOWS_LOAD_SU;
    }
}
