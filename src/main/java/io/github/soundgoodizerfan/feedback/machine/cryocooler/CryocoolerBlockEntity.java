// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cryocooler;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cryocooler cold head: an electrical sink that, while it is fed, holds the face it points at at a temperature far under
 * ambient. It is a {@link HeatSource} whose temperature is below ambient: {@link HeatSource#at} ignores it, {@link HeatSource#coldAt}
 * (read by the reactor) chills the vessel against it toward that temperature. The stage is a three-way choice, each a colder or
 * warmer head; a vessel settles a little short of the stage, so a stage is a rough cut of cold, not a thermostat. It reads no
 * vessel and no fluid, and a head that is not fed holds nothing.
 * shortcut: the head's own temperature jumps to the stage with no inertia and its hot side is not modelled (the Refrigeration Unit
 * does warm its radiator); give it a thermal mass if a cold start should be felt.
 */
public class CryocoolerBlockEntity extends ElectricNode implements HeatSource {

    /** The cold head's stages, deepest first. They name a temperature, never a product. */
    public enum Stage {
        DEEP(VesselTuning.CRYOCOOLER_DEEP_TU), MID(VesselTuning.CRYOCOOLER_MID_TU), SHALLOW(VesselTuning.CRYOCOOLER_SHALLOW_TU);

        private final Tu tu;

        Stage(Tu tu) {
            this.tu = tu;
        }

        public Tu tu() {
            return tu;
        }
    }

    private Stage stage = Stage.DEEP;
    private boolean running;

    public CryocoolerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRYOCOOLER.get(), pos, state);
    }

    public Stage stage() {
        return stage;
    }

    public void cycleStage() {
        stage = Stage.values()[(stage.ordinal() + 1) % Stage.values().length];
        setChanged();
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return VesselTuning.CRYOCOOLER_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        running = ElectricRules.rate(volts, amps, wanted, ratedBand()) > 0f;
    }

    @Override
    public Direction heatFacing() {
        return HeatSource.facingOf(getBlockState());
    }

    /** The head cools the face it points at and nothing beside it. */
    @Override
    public float sideShare() {
        return 0f;
    }

    @Override
    public Tu getFireTu() {
        return running ? stage.tu() : level == null ? ThermalTuning.AMBIENT_TU : AmbientTable.at(level);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stage", stage.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stage = Stage.values()[Math.floorMod(tag.getInt("Stage"), Stage.values().length)];
    }
}
