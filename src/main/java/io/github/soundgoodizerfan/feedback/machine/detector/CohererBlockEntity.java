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
package io.github.soundgoodizerfan.feedback.machine.detector;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tube of loose redstone grains (compendium 3.3): an open line until a spark welds the weak
 * links between grains, then a wire that stays a wire. Hearing any spark in range welds it;
 * a tap (a hand, or a controller switching it off) shakes the grains loose again. Left alone it
 * stays welded and passes everything: the latch is the detector's memory, and forgetting to
 * tap it is the detector's failure.
 *
 * <h2>Overrun</h2>
 * Current through the welded tube past {@link FTuning#COHERER_RATING_AMPS} sinters the grains
 * together: condition falls toward {@link FTuning#COHERER_CONDITION_FLOOR}, and a tap frees it
 * only with probability {@code condition}, so a sintered coherer needs more taps -- a slow, readable
 * path, never a refusal. Wear only from that misuse; weld and tap cost nothing.
 *
 * <h2>Sense and stop</h2>
 * Sense: a spark within earshot. Stop: the tap. A controller drives it as a {@link Switchable}
 * whose only effect is the tap: it can switch it off, and no controller can switch it on, because
 * only a spark welds.
 */
public class CohererBlockEntity extends SparkDetectorBlockEntity implements Switchable, Wearing, io.github.soundgoodizerfan.feedback.instrument.Testable {

    private boolean welded;
    private float condition = 1f;

    public CohererBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COHERER.get(), pos, state);
    }

    @Override
    public boolean conducting() {
        return welded;
    }

    @Override
    protected void hear(double distance) {
        if (!welded)
            set(true);
    }

    /** One tap. Frees the grains with probability {@code condition}. */
    public boolean tap() {
        if (!welded || level == null || level.isClientSide)
            return false;
        if (level.random.nextFloat() >= condition)
            return false;
        set(false);
        return true;
    }

    private void set(boolean value) {
        welded = value;
        setChanged();
        if (level != null) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CohererBlock.WELDED, value));
            ElectricPropagator.reconnect(level, worldPosition);
        }
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (welded && amps > FTuning.COHERER_RATING_AMPS)
            condition = Math.max(FTuning.COHERER_CONDITION_FLOOR,
                    condition - FTuning.COHERER_SINTER_PER_AMP * (amps - FTuning.COHERER_RATING_AMPS));
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (!engaged)
            tap();
    }

    @Override
    public boolean isEngaged() {
        return welded;
    }


    @Override
    public void test(io.github.soundgoodizerfan.feedback.instrument.Instrument tester, java.util.function.Consumer<net.minecraft.network.chat.Component> out) {
        out.accept(net.minecraft.network.chat.Component.translatable(welded ? "feedback.test.welded" : "feedback.test.open"));
        out.accept(io.github.soundgoodizerfan.feedback.instrument.Testable.condition(tester, condition));
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return welded && getAmps() > 0f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Welded", welded);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        welded = tag.getBoolean("Welded");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
