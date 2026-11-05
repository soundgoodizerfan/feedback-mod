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
package io.github.soundgoodizerfan.feedback.machine.lamp;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A SINK that asks {@link FTuning#LAMP_RATED_AMPS} at MV and shows how well it is fed as one of four
 * brightness steps. It never refuses a line and passes nothing along.
 *
 * <p>The step is a function of the rate fraction ({@link ElectricRules#rate}) times condition, set in
 * {@link #onResolved} with a few float operations and no scheduled tick; the block state is written
 * only when the step changes. Each threshold has a gap each way ({@link FTuning#LAMP_HYSTERESIS}) so a
 * line near a boundary does not flicker the light.
 *
 * <p>Mild overvoltage costs condition (and so brightness, for good, to a floor). Volts at or above
 * {@link FTuning#LAMP_BURNOUT_VOLT_FACTOR} times rated burn the panel out at once: the block turns
 * {@link LampBlock.Glow#BURNT}, dark, wanting nothing. A cheap panel may pop (digest, 2026-11-03). Breaking
 * a burnt lamp drops the lamp item like any other; there is no repair.
 */
public class LampBlockEntity extends ElectricNode implements Wearing {

    private float condition = 1f;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LAMP.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return getBlockState().getValue(LampBlock.GLOW) == LampBlock.Glow.BURNT ? 0f : FTuning.LAMP_RATED_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (level == null || level.isClientSide)
            return;
        BlockState state = getBlockState();
        LampBlock.Glow now = state.getValue(LampBlock.GLOW);
        if (now == LampBlock.Glow.BURNT)
            return;

        if (volts >= FTuning.LAMP_RATED_VOLTS * FTuning.LAMP_BURNOUT_VOLT_FACTOR) {
            setGlow(state, LampBlock.Glow.BURNT);
            return;
        }
        float excess = ElectricRules.overVoltage(volts, ratedBand());
        if (excess > 0f) {
            condition = Math.max(FTuning.LAMP_CONDITION_FLOOR, condition - FTuning.LAMP_CONDITION_LOSS_PER_EXCESS * excess);
            setChanged();
        }
        float fraction = ElectricRules.rate(volts, amps, wanted, ratedBand()) * condition;
        LampBlock.Glow next = step(now.ordinal(), fraction);
        if (next != now)
            setGlow(state, next);
    }

    private void setGlow(BlockState state, LampBlock.Glow glow) {
        level.setBlock(worldPosition, state.setValue(LampBlock.GLOW, glow), Block.UPDATE_CLIENTS);
    }

    /** The step for a fraction, given the current one: rise past a threshold plus the gap, fall below it minus the gap. */
    static LampBlock.Glow step(int current, float fraction) {
        float[] at = FTuning.LAMP_STEP_THRESHOLDS;
        int step = current;
        while (step < at.length && fraction > at[step] + FTuning.LAMP_HYSTERESIS)
            step++;
        while (step > 0 && fraction < at[step - 1] - FTuning.LAMP_HYSTERESIS)
            step--;
        return LampBlock.Glow.values()[step];
    }

    @Override
    public float getCondition() {
        return condition;
    }

    /** A lamp is lit or not at a glance; it is never "running" in the sense of an unmeasurable machine. */
    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
