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
package io.github.soundgoodizerfan.feedback.machine.capacitor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds charge at a voltage and gives it back in a burst, and leaks (mechanics 6.6). The storage
 * that makes a pulse usable: an electrostatic machine's twenty ticks go into the jar, and come out
 * as long as something draws on it.
 *
 * <h2>Charge, and why it is not a unit</h2>
 * The jar holds {@code charge} in amp-ticks -- current times time, what has flowed in -- and its
 * voltage is charge over {@link FTuning#LEYDEN_JAR_CAPACITANCE}. Like {@code Work}, it is an
 * internal quantity the jar keeps and nothing else reads, so it gets no type: a reading of it is a
 * voltage, which the instruments already speak.
 *
 * <h2>One role at a time, and it changes</h2>
 * {@code ElectricMember} gives a node exactly one role, so the jar is a <em>sink</em> while a
 * higher voltage is on its run, charging at a fixed current up to that voltage, and a
 * <em>source</em> once nothing on the run holds it up, offering its own voltage at a large
 * current. It flips when the supply goes (as a sink it sees no more than it holds) or when it is
 * outbid (as a source it is blocked, the resolver's own word for it), and an empty jar always
 * waits as a sink. Role decides which neighbours conduct to it, so a flip rebuilds its run -- an
 * event, not a per-tick cost.
 * <p>
 * The voltage falls as it empties, so whatever runs on it slows as it goes, by the same
 * {@code ElectricRules.rate} as a long wire: a burst, not a battery. It never refuses and is never
 * destroyed.
 */
public class LeydenJarBlockEntity extends ElectricNode {

    private static final String CHARGE = "Charge", SUPPLYING = "Supplying";

    private float charge;
    private boolean supplying;

    public LeydenJarBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LEYDEN_JAR.get(), pos, state);
    }

    public float getStoredVolts() {
        return charge / FTuning.LEYDEN_JAR_CAPACITANCE;
    }

    /** Empties the jar and returns the volts it held. */
    public float drain() {
        float held = getStoredVolts();
        charge = 0;
        setChanged();
        return held;
    }

    /** A charged jar's terminals are bare: once it holds MV or more, touching it is a shock. */
    @Override
    public float touchVolts() {
        float held = getStoredVolts();
        return held >= FTuning.SHOCK_MIN_VOLTS ? held : 0f;
    }

    public boolean isSupplying() {
        return supplying;
    }

    @Override
    public Role role() {
        return supplying ? Role.SOURCE : Role.SINK;
    }

    @Override
    public float sourceVolts() {
        return supplying ? getStoredVolts() : 0;
    }

    @Override
    public float sourceAmps() {
        return supplying && charge > 0 ? FTuning.LEYDEN_JAR_BURST_AMPS : 0;
    }

    /** Charges only while what it sees is above what it holds; a full jar asks for nothing. */
    @Override
    public float wantedAmps() {
        return !supplying && getVolts() > getStoredVolts() + FTuning.LEYDEN_JAR_SWITCH_VOLTS
                ? FTuning.LEYDEN_JAR_CHARGE_AMPS : 0;
    }

    public void tickServer() {
        if (level == null)
            return;
        // Last tick's resolution: amps delivered in as a sink, drawn out as a source.
        float before = charge;
        charge += supplying ? -getAmps() : getAmps();
        if (!supplying)
            charge = Math.min(charge, getVolts() * FTuning.LEYDEN_JAR_CAPACITANCE);
        charge = Math.max(0, charge * (1 - FTuning.LEYDEN_JAR_LEAK_PER_TICK));
        if (charge != before)
            setChanged();

        float held = getStoredVolts();
        boolean flip = supplying
                ? held <= FTuning.LEYDEN_JAR_SWITCH_VOLTS || isBlocked()
                : held > FTuning.LEYDEN_JAR_SWITCH_VOLTS && getVolts() <= held + FTuning.LEYDEN_JAR_SWITCH_VOLTS;
        if (flip) {
            supplying = !supplying;
            setChanged();
            ElectricPropagator.rebuildFrom(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(CHARGE, charge);
        tag.putBoolean(SUPPLYING, supplying);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = tag.getFloat(CHARGE);
        supplying = tag.getBoolean(SUPPLYING);
    }
}
