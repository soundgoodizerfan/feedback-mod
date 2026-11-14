// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pyro;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A plate of a pyroelectric crystal: it answers only to change. Every {@link PyroTuning#SAMPLE_INTERVAL_TICKS} ticks it reads the
 * temperature of each thermal body it touches and keeps the fastest change of any of them (a steady hot body gives nothing). That
 * rate, quantised to a resolution the material sets (coarser for a weaker pyroelectric), is the reading a Controller asks for
 * ({@link SensorFitting}, an unsided node) . It gives no power and acts on nothing it measures; Between samples it does nothing.
 */
public class PyroPlateBlockEntity extends BlockEntity implements SensorFitting {

    @Nullable
    private ResourceLocation material;
    private float coupling = -1f;
    /** Last sampled temperature of the body on each face, NaN where there was none. */
    private final float[] last = {Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN};
    private float rate;
    private int sinceSample;
    private final Set<DataNodeRef> links = new HashSet<>();

    public PyroPlateBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PYRO_PLATE.get(), pos, state);
    }

    @Nullable
    public ResourceLocation material() {
        return material;
    }

    /** 0 to 1: the material's pyroelectric coupling. */
    public float coupling() {
        if (coupling < 0f)
            coupling = material == null ? 0f : MaterialTable.get().materials().get(material) == null ? 0f
                    : MaterialTable.get().materials().get(material).electrical().pyroCoupling().orElse(0f);
        return coupling;
    }

    /** The true rate of the fastest-changing touched body, Tu per second, as of the last sample. */
    public float rate() {
        return rate;
    }

    public void tickServer() {
        if (level == null || ++sinceSample < PyroTuning.SAMPLE_INTERVAL_TICKS)
            return;
        sinceSample = 0;
        float seconds = PyroTuning.SAMPLE_INTERVAL_TICKS / 20f;
        float fastest = 0f;
        for (Direction d : Direction.values()) {
            int i = d.get3DDataValue();
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ThermalBody body) {
                if (!Float.isNaN(last[i])) {
                    float r = (body.getTemperature().value() - last[i]) / seconds;
                    if (Math.abs(r) > Math.abs(fastest))
                        fastest = r;
                }
                last[i] = body.getTemperature().value();
            } else
                last[i] = Float.NaN;
        }
        rate = fastest;
    }

    // --- SensorFitting / Instrument -------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.HEATING_RATE;
    }

    /** A weaker pyroelectric gives less charge for the same change, so it resolves it more coarsely. */
    @Override
    public float resolution(Quantity quantity) {
        return PyroTuning.RESOLUTION_TU_PER_S / Math.max(0.1f, coupling());
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.pyro_plate");
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.readout.tu_per_s",
                Instruments.format(coupling() <= 0f ? 0f : rate, resolution(Quantity.HEATING_RATE))));
    }

    @Override
    public float readRaw() {
        return coupling() <= 0f ? 0f : Instruments.quantise(rate, resolution(Quantity.HEATING_RATE));
    }

    @Override
    public ItemStack getPickItem() {
        return material == null ? ItemStack.EMPTY : PyroPlateItem.of(material);
    }

    // --- DataNode ---------------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence ------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        MadeOf made = input.get(FDataComponents.MADE_OF.get());
        if (made != null)
            material = made.material();
        coupling = -1f;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (material != null)
            components.set(FDataComponents.MADE_OF.get(), new MadeOf(material, PyroPlateItem.FORM, 1));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material != null)
            tag.putString("Material", material.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.contains("Material") ? ResourceLocation.parse(tag.getString("Material")) : null;
        coupling = -1f;
    }
}
