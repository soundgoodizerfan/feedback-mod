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
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
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
 * The galvanometer as a fitting: a sided, read-only sensor on a wire (mechanics 6.7), following
 * {@link TemperatureSensorFitting} exactly. It reads the current at the block it sits on and
 * nothing else, so it can be linked to a controller through the data connector like any other
 * sensor, and a program can act on a wire's current without the player standing next to it.
 *
 * <p>Like every instrument it never enters the calculation: it has no way to act on the line.
 */
public class CurrentSensorFitting implements SensorFitting {

    private static final String LINKS = "Links";

    private final ElectricNode holder;
    private final Direction side;
    private final Set<DataNodeRef> links = new HashSet<>();

    public CurrentSensorFitting(ElectricNode holder, Direction side) {
        this.holder = holder;
        this.side = side;
    }

    // --- Instrument -----------------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CURRENT;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.CURRENT_SENSOR_RESOLUTION_AMPS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.current_sensor");
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.readout.amps", format(readRaw())));
    }

    @Override
    public float readRaw() {
        return Instruments.quantise(holder.getAmps(), resolution(Quantity.CURRENT));
    }

    static String format(float amps) {
        return String.format(java.util.Locale.ROOT, "%.2f", amps);
    }

    // --- Fitting ----------------------------------------------------------------------------

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.CURRENT_SENSOR.get());
    }

    @Override
    public void onRemoved() {
        unlinkAll();
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
