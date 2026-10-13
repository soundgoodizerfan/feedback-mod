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
package io.github.soundgoodizerfan.feedback.machine.electrostatic;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pulse source: a turn of the handle puts a voltage on the line for a fixed run of ticks,
 * then nothing.
 *
 * <h2>Why a pulse and not a store</h2>
 * Smoothing is a block the player places, not a buffer hidden in a machine (mechanics 6.6), so
 * this holds no charge: it is live or it is idle. Being idle is a real state the resolver sees, not
 * a zero it happens to compute -- an idle source is not part of the supply, and a lower-voltage
 * cell on the same line is blocked only while this one is pulsing.
 *
 * <h2>Not a conversion</h2>
 * The player turns it, which is not a mechanical network feeding electricity: nothing in the
 * block network drives it, and nothing converts to or from {@code Eu} here (mechanics 6.8's
 * mechanical boundary is a dynamo, and it is a later slice).
 *
 * <h2>The band is a placeholder</h2>
 * An electrostatic machine is the top band by voltage and nearly nothing by current, which is the
 * reason {@code Eu} has two components (mechanics 6.3). The band anchors are <b>[OPEN]</b>, so it
 * is built at LV's reference like everything else in this slice; the character is the pulse.
 */
public class ElectrostaticGeneratorBlockEntity extends ElectricNode {

    private int pulseTicks;

    public ElectrostaticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROSTATIC_GENERATOR.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    /** One turn of the handle. Turning again mid-pulse tops it up to the full length, not past it. */
    public void turn() {
        pulseTicks = FTuning.ELECTROSTATIC_PULSE_TICKS;
        setChanged();
        if (level != null)
            level.playSound(null, worldPosition, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.6f, 1.6f);
    }

    public boolean isPulsing() {
        return pulseTicks > 0;
    }

    public void tickServer() {
        if (pulseTicks > 0)
            pulseTicks--;
    }

    @Override
    public float sourceVolts() {
        return pulseTicks > 0 ? FTuning.ELECTROSTATIC_VOLTS : 0f;
    }

    @Override
    public float sourceAmps() {
        return pulseTicks > 0 ? FTuning.ELECTROSTATIC_AMPS : 0f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Pulse", pulseTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulseTicks = tag.getInt("Pulse");
    }
}
