// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Electricity's bookkeeping unit (philosophy 21): energy per tick, or the energy in one packet. A
 * {@link VoltageBand} is a packet size in {@code Eu}, a current is a whole count of packets per tick
 * (amps, like {@link Fu}), and {@code Eu/t = packet x amps}, so any two supplies compare by converting
 * both to {@code Eu/t} ({@code ElectricRules.euPerTick}). This record is a supply or a load as the
 * pair a player meets it as, band and amps; the field is named {@code voltage} for the band only.
 * DC only, no frequency, resistance not modelled; if AC arrives, frequency is a third component.
 * Nothing outside this record should assume exactly two.
 *
 * <h2>{@link #raw()} is the current</h2>
 * {@link Unit#raw()} has to answer with one float, and an {@code Eu} has two components. It
 * answers with the current, because that is the figure a load is rated and starved in; it says
 * nothing about voltage, so it must never be used to compare two {@code Eu}; compare their {@code Eu/t}. Read
 * {@link #voltage()} and {@link #current()}. Display goes through {@link #figure()}, not
 * {@link Units#figure}, for the same reason.
 *
 * <p>The codec and stream codec live here and not in {@link Units}: the {@code Units} helpers
 * wrap one number, and this is the one unit that is not one.
 */
public record Eu(VoltageBand voltage, int current) implements Unit {

    public static final Codec<Eu> CODEC = RecordCodecBuilder.create(i -> i.group(
            VoltageBand.CODEC.fieldOf("voltage").forGetter(Eu::voltage),
            Codec.INT.fieldOf("current").forGetter(Eu::current)).apply(i, Eu::new));

    public static final StreamCodec<ByteBuf, Eu> STREAM_CODEC = StreamCodec.composite(
            VoltageBand.STREAM_CODEC, Eu::voltage,
            ByteBufCodecs.VAR_INT, Eu::current,
            Eu::new);

    @Override
    public float raw() {
        return current;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.eu";
    }

    /** {@code 3 Eu at LV}: the whole reading, voltage and current together. */
    public Component figure() {
        return Component.translatable("feedback.unit.eu.figure", current, Component.translatable(voltage.key()));
    }
}
