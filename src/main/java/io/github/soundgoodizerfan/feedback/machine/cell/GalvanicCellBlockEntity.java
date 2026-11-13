// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cell;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The continuous source of slice 4: a band from its electrode pair and a small
 * current, and it is a curiosity rather than a supply (roadmap, slice 4).
 *
 * <h2>The band is the pairing, in steps</h2>
 * Each electrode's {@code electrode_potential} is read from its material file and only the
 * magnitude of the gap matters: a close pair is LV, a wide pair MV, and cells in series (a pack) raise it
 * a band per {@link ElectricMachineTuning#GALVANIC_CELLS_PER_BAND}, like a voltaic pile. No potential is held or shown. A cell whose electrodes lack the figure, or are the same
 * metal, holds nothing: an unmeasured material is a cell that does nothing, not one that guesses.
 *
 * <h2>It eats its anode</h2>
 * A real cell consumes its anode (user ruling, 2026-10-23). The current it supplies spends
 * {@link ElectricMachineTuning#GALVANIC_ANODE_EU} Eu of a full anode, and a spent cell holds no
 * voltage. An ingot of the anode metal used on the block restores part of it, so the running cost is
 * a real item. The remaining anode travels with the mined block.
 */
public class GalvanicCellBlockEntity extends ElectricNode {

    private int figuresGeneration = -1;
    private boolean paired;
    private VoltageBand pairBand = VoltageBand.LV;
    private float anode = 1f;

    private final FigureStore figures = new FigureStore();

    public GalvanicCellBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GALVANIC_CELL.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public VoltageBand sourceBand() {
        if (figuresGeneration != MaterialTable.generation()) {
            figuresGeneration = MaterialTable.generation();
            paired = false;
            if (getBlockState().getBlock() instanceof GalvanicCellBlock block) {
                var materials = MaterialTable.get().materials();
                Material anode = materials.get(block.anode());
                Material cathode = materials.get(block.cathode());
                if (anode != null && cathode != null
                        && anode.electrical().electrodePotential().isPresent()
                        && cathode.electrical().electrodePotential().isPresent()) {
                    // The nobility gap is read as a step: close pair LV, wide pair MV; each cells-per-band in series raises one more.
                    float gap = Math.abs(cathode.electrical().electrodePotential().get() - anode.electrical().electrodePotential().get());
                    paired = gap > 0f;
                    int step = (gap >= ElectricMachineTuning.GALVANIC_WIDE_GAP ? 1 : 0)
                            + (block.series() - 1) / ElectricMachineTuning.GALVANIC_CELLS_PER_BAND;
                    pairBand = VoltageBand.values()[Math.min(VoltageBand.values().length - 1, step + VoltageBand.LV.ordinal())];
                }
            }
        }
        return anode <= 0f || !paired ? null : pairBand;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        if (amps > 0f && anode > 0f) {
            anode = Math.max(0f, anode - amps * eu / figures.get("AnodeEu", ElectricMachineTuning.GALVANIC_ANODE_EU));
            setChanged();
        }
    }

    /** How much of the anode is left, 0 to 1. */
    public float getAnode() {
        return anode;
    }

    /** Feeds an anode ingot in; false when the anode is already whole. */
    public boolean refill() {
        if (anode >= 1f)
            return false;
        anode = Math.min(1f, anode + ElectricMachineTuning.GALVANIC_ANODE_PER_INGOT);
        setChanged();
        return true;
    }

    @Override
    public float sourceAmps() {
        return figures.get("Amps", ElectricMachineTuning.GALVANIC_CELL_AMPS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putFloat("Anode", anode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        anode = tag.contains("Anode") ? tag.getFloat("Anode") : 1f;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get()), input.get(io.github.soundgoodizerfan.feedback.registry.FModular.FIGURES.get()));
        anode = input.getOrDefault(FDataComponents.ANODE_REMAINING.get(), 1f);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
        if (anode < 1f)
            components.set(FDataComponents.ANODE_REMAINING.get(), anode);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Anode");
    }
}
