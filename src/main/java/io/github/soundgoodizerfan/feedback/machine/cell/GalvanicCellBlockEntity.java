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
package io.github.soundgoodizerfan.feedback.machine.cell;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The continuous source of slice 4: a fixed potential from its electrode pair and a small
 * current, and it is a curiosity rather than a supply (roadmap, slice 4).
 *
 * <h2>Voltage is the difference of two properties</h2>
 * Each electrode's {@code electrode_potential} is read from its material file and the cell holds
 * the magnitude of the difference. A cell whose electrodes lack the figure, or are the same
 * metal, holds nothing: an unmeasured material is a cell that does nothing, not one that guesses.
 *
 * <h2>What it does not do yet</h2>
 * It never runs down. A real cell consumes its anode, and that is a chemistry-slice
 * consequence that wants a decision of its own ({@code TODO.md}, <b>[OPEN]</b>); until then it
 * is the same "stores chemically and leaks chemically" block mechanics 6.6 describes with
 * neither half modelled.
 */
public class GalvanicCellBlockEntity extends ElectricNode {

    private int figuresGeneration = -1;
    private float volts;

    public GalvanicCellBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GALVANIC_CELL.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public float sourceVolts() {
        if (figuresGeneration != MaterialTable.generation()) {
            figuresGeneration = MaterialTable.generation();
            volts = 0f;
            if (getBlockState().getBlock() instanceof GalvanicCellBlock block) {
                var materials = MaterialTable.get().materials();
                Material anode = materials.get(block.anode());
                Material cathode = materials.get(block.cathode());
                if (anode != null && cathode != null
                        && anode.electrical().electrodePotential().isPresent()
                        && cathode.electrical().electrodePotential().isPresent())
                    volts = Math.abs(cathode.electrical().electrodePotential().get()
                            - anode.electrical().electrodePotential().get());
            }
        }
        return volts;
    }

    @Override
    public float sourceAmps() {
        return FTuning.GALVANIC_CELL_AMPS;
    }
}
