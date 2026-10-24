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
package io.github.soundgoodizerfan.feedback.machine.breaker;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electricity's stop (philosophy 10): it cuts nearly instantly, and the contacts arc and wear
 * when they open under load. A wire in the run while closed; open, it joins nothing, and the line
 * through it is two runs.
 *
 * <h2>Wear from one act</h2>
 * Opening while current flows draws an arc, and the arc pits the contacts: condition falls by the
 * current broken, never by time closed. Pitted contacts lose more volts per amp, so a breaker
 * flapped under load by a controller with no deadband (philosophy 13) shows it as a sag on the
 * far side -- sloppy logic destroying downstream hardware, by a gradient. Never broken; it keeps
 * switching at its floor.
 *
 * <h2>It trips</h2>
 * Over {@link FTuning#BREAKER_TRIP_AMPS} -- a copper cable's rating, the thing it is there to
 * protect -- it opens itself and stays open: that is an opening under load too, and it is charged
 * like one. A hand or a controller closes it again; closing it back onto the fault trips it again,
 * and wears it again.
 */
public class BreakerBlockEntity extends ElectricNode implements Switchable, Wearing {

    private boolean closed = true;
    private float condition = 1f;

    public BreakerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BREAKER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    @Override
    public boolean conducting() {
        return closed;
    }

    @Override
    public float dropPerAmp() {
        return FTuning.BREAKER_DROP_PER_AMP / condition;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (closed && amps > FTuning.BREAKER_TRIP_AMPS)
            setEngaged(false);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (engaged == closed || level == null || level.isClientSide)
            return;
        if (!engaged && getAmps() > 0f)
            io.github.soundgoodizerfan.feedback.core.electric.Sparks.emit(level, worldPosition, getAmps());
        if (!engaged && getAmps() > 0f)
            condition = Math.max(FTuning.BREAKER_CONDITION_FLOOR, condition - FTuning.BREAKER_ARC_WEAR_PER_AMP * getAmps());
        closed = engaged;
        setChanged();
        ElectricPropagator.reconnect(level, worldPosition);
    }

    @Override
    public boolean isEngaged() {
        return closed;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return closed && getAmps() > 0f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Closed", closed);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        closed = !tag.contains("Closed") || tag.getBoolean("Closed");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
