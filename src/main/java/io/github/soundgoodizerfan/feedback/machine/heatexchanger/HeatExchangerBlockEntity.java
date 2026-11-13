// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.heatexchanger;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wall between two {@link ThermalBody}s. Bridges whatever it is placed against on its own axis
 * -- {@link HeatExchangerBlock#FACING} and its opposite -- and moves heat between them every tick,
 * hot side to cold side, at {@link FTuning#HEAT_EXCHANGER_CONDUCTANCE}.
 *
 * <h2>No identity check, and no direction to pick</h2>
 * It does not ask what either side is -- a Crucible, a Boiler, a future block that has not been
 * written yet, all answer the same {@link ThermalBody} contract and this reads it the same way
 * {@code Heat.tick} already reads a vessel's own fire. Flow direction is not a setting either: {@link
 * Heat#exchange} already runs hot to cold by construction, the same way a vessel already cannot
 * pass its own fire's temperature (see that method's own doc). {@code
 * feedback_philosophy.md} §10 names a genuine <em>pumped</em> exchanger -- forcing heat uphill on
 * borrowed power, with the failure mode of overrunning both ends at once -- as a real later
 * mechanic; this is the passive half only, the wall itself, built first per the same
 * infrastructure-before-necessity corollary as {@code Instrument} and {@code fitting/}.
 *
 * <h2>Placement is the whole decision</h2>
 * Nothing here is powered and nothing is switched. The player's only choice is which two things
 * to stand it between -- philosophy 3's "shafts and pipes are still real blocks... a genuine
 * spatial and budgetary puzzle" applied to a wall instead of a pipe.
 */
public class HeatExchangerBlockEntity extends BlockEntity implements io.github.soundgoodizerfan.feedback.machine.Mendable {


    private final FigureStore figures = new FigureStore();

    /** Tu between its two sides past which the wall takes thermal shock; condition lost per tick at one more such gap; the least it falls to. [OPEN] until played */
    private static final float SHOCK_TU = 600f, SHOCK_WEAR_PER_TICK = 0.0004f, CONDITION_FLOOR = 0.5f;
    private float condition = 1f;

    @Override
    public float mendableCondition() {
        return condition;
    }

    @Override
    public void mend(float by) {
        condition = Math.min(1f, condition + by);
        setChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.HEAT_EXCHANGER.get(), pos, state);
    }

    public void tickServer() {
        if (level == null)
            return;

        Direction axis = getBlockState().getValue(HeatExchangerBlock.FACING);

        if (!(level.getBlockEntity(worldPosition.relative(axis)) instanceof ThermalBody a))
            return;
        if (!(level.getBlockEntity(worldPosition.relative(axis.getOpposite())) instanceof ThermalBody b))
            return;

        // Thermal shock is misuse: a wall between two bodies further apart than it can take cracks a little each tick (never below the floor),
        // and a cracked wall passes less.
        float gap = Math.abs(a.getTemperature().value() - b.getTemperature().value());
        if (gap > SHOCK_TU && condition > CONDITION_FLOOR) {
            condition = Math.max(CONDITION_FLOOR, condition - SHOCK_WEAR_PER_TICK * Math.min(2f, gap / SHOCK_TU - 1f) * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(this));
            setChanged();
        }
        Heat.exchange(a, b, new Conductance(figures.get("Conductance", FTuning.HEAT_EXCHANGER_CONDUCTANCE.workPerTickPerTu()) * condition));
    }
}
