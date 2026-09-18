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
package io.github.soundgoodizerfan.feedback.fitting.sensor;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The mod's first fitting -- proof of {@code fitting/}'s shape the way the Thermometer proved
 * {@link io.github.soundgoodizerfan.feedback.instrument.Instrument}. Reads any holder's own
 * temperature, because a sensor fitting caring which kind of vessel it is bolted to would be an
 * identity check on the block instead of the item (§11's rule applied one level up).
 *
 * <h2>Two references, not a generic bound</h2>
 * A holder needs to be both a {@link BlockEntity} (for position and level) and a {@link
 * ThermalBody} (for the reading), and no existing class in the mod is typed as the intersection
 * of the two -- {@code CrucibleBlockEntity} and {@code ThermalVesselBlockEntity} both separately
 * implement {@code ThermalBody} while extending {@code BlockEntity}. A generic {@code <H extends
 * BlockEntity & ThermalBody>} would need every call site to already hold one reference of that
 * combined type, which an {@code instanceof} chain against a plain {@code BlockEntity} cannot
 * produce without an unchecked cast. Two plain fields, filled from two separate checks at the
 * point of attachment, is the honest version of the same fact.
 */
public class TemperatureSensorFitting implements SensorFitting {

    private static final String LINKS = "Links";

    private final BlockEntity holder;
    private final ThermalBody thermal;
    private final Direction side;
    private final Set<DataNodeRef> links = new HashSet<>();

    public TemperatureSensorFitting(BlockEntity holder, ThermalBody thermal, Direction side) {
        this.holder = holder;
        this.thermal = thermal;
        this.side = side;
    }

    // --- Instrument -----------------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.TEMPERATURE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.TEMPERATURE_SENSOR_RESOLUTION_TU;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.temperature_sensor");
    }

    @Override
    public Component readValue() {
        float shown = readRaw();
        return Instruments.signed(this, Component.translatable("feedback.readout.tu", Math.round(shown)));
    }

    @Override
    public float readRaw() {
        return Instruments.quantise(thermal.getTemperature().value(), resolution(Quantity.TEMPERATURE));
    }

    // --- Fitting ----------------------------------------------------------------------------

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.TEMPERATURE_SENSOR.get());
    }

    // --- DataNode ---------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return holder.getBlockPos();
    }

    @Override
    public Direction getNodeSide() {
        return side;
    }

    @Override
    public Level getNodeLevel() {
        return holder.getLevel();
    }

    @Override
    public BlockEntity getNodeOwner() {
        return holder;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence --------------------------------------------------------------------------

    public void writeNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
    }

    public void readNbt(CompoundTag tag) {
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
    }
}
