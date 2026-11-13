// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The input side of a solid-state DC converter (grid spec 5): a SINK on the line it steps from, whose twin
 * {@link ConverterTapBlockEntity}, set against it, is a SOURCE on the line it steps to. The two never share a run
 * ({@link #joins}); this block reads the tap's current each tick and asks the input line for the power it carries, over the
 * efficiency. The ratio is one of the named {@link Ratio}s, a block state. The loss is heat in this block, shed to air and
 * faster beside aluminium {@link HeatsinkBlock}s; past the service temperature the block loses condition, and a worn one
 * wastes more. It never refuses a line and is never destroyed.
 */
public class ConverterBlockEntity extends ElectricNode implements ThermalBody, Wearing {

    private float temperature = ThermalTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private BlockPos tapPos;
    private int sinks;

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CONVERTER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public boolean joins(ElectricMember other) {
        return !(other instanceof ConverterTapBlockEntity);
    }

    /** The band of the line it sees: a converter takes any, and is limited by the power it can pass, not by a rated band. */
    @Override
    public VoltageBand ratedBand() {
        return getBand();
    }

    public Ratio ratio() {
        return getBlockState().getValue(ConverterBlock.RATIO);
    }

    /** The share of the power it takes in that comes out: the base efficiency, less what wear and a hot body waste. */
    public float efficiency() {
        return ElectricMachineTuning.CONVERTER_EFFICIENCY * (0.6f + 0.4f * condition);
    }

    @Override
    public float wantedAmps() {
        ConverterTapBlockEntity tap = tap();
        float out = tap == null ? 0f : tap.getAmps();
        // Input current = output current times the packet ratio, over efficiency: the same Eu in as out plus the loss.
        VoltageBand outBand = ratio().out(getBand());
        float step = outBand == null ? 0f : ElectricRules.packetEu(outBand) / ElectricRules.packetEu(getBand());
        return ElectricMachineTuning.CONVERTER_STANDING_AMPS + out * step / efficiency();
    }

    private ConverterTapBlockEntity tap() {
        if (level == null)
            return null;
        if (tapPos != null && level.getBlockEntity(tapPos) instanceof ConverterTapBlockEntity tap)
            return tap;
        tapPos = null;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ConverterTapBlockEntity tap) {
                tapPos = tap.getBlockPos();
                return tap;
            }
        return null;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        if (level == null || level.isClientSide)
            return;
        ConverterTapBlockEntity tap = tap();
        VoltageBand outBand = eu > 0f ? ratio().out(band) : null;
        if (tap != null) {
            // What the tap offers is this block's band, stepped, never more Eu/t than the converter's rating passes. A starved
            // input line (delivered below wanted) lowers the offer in proportion, so the output sags rather than stopping.
            float fed = wanted > 0f ? Math.min(1f, amps / wanted) : 0f;
            tap.offer(outBand, outBand != null ? ElectricMachineTuning.CONVERTER_RATED_EU_PER_TICK / ElectricRules.packetEu(outBand) * fed : 0f);
            float power = tap.getPacketEu() * tap.getAmps();
            if (power > 0f)
                Heat.warm(this, power * (1f / efficiency() - 1f) * ElectricMachineTuning.CONVERTER_HEAT_SCALE);
        }
        if (temperature > ElectricMachineTuning.CONVERTER_SERVICE_TU) {
            condition = Math.max(ElectricMachineTuning.CONVERTER_CONDITION_FLOOR, condition - (temperature - ElectricMachineTuning.CONVERTER_SERVICE_TU) * ElectricMachineTuning.MACHINE_HEAT_WEAR_PER_TU);
            setChanged();
        }
        if (level.getGameTime() % ElectricMachineTuning.CONVERTER_SCAN_TICKS == 0)
            sinks = countSinks();
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, ambient, ambient, false);
    }

    private int countSinks() {
        int n = 0;
        for (Direction d : Direction.values())
            if (level.getBlockState(worldPosition.relative(d)).getBlock() instanceof HeatsinkBlock)
                n++;
        return Math.min(n, ElectricMachineTuning.CONVERTER_MAX_HEATSINKS);
    }

    /** The number of heatsinks set against it, as of the last scan. */
    public int heatsinks() {
        return sinks;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        ConverterTapBlockEntity tap = tap();
        return tap != null && tap.getAmps() > 0f;
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return ElectricMachineTuning.CONVERTER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(ElectricMachineTuning.CONVERTER_LEAK.workPerTickPerTu() * (1f + ElectricMachineTuning.CONVERTER_HEATSINK_LEAK_MULTIPLE * sinks));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
