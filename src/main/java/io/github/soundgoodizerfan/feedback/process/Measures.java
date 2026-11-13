// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a made part carries beyond its material and shape: four unrelated things that used to share one
 * grab-bag map ({@code dims}), kept apart because each has its own reader and its own rule.
 *
 * @param axes     as-built measures of the part, by the names its role declares
 *                 ({@link io.github.soundgoodizerfan.feedback.modular.ModularType.Axis}): a barrel's bore, outside and
 *                 length. The item stores the as-built value only; the aim lives in a blueprint's drawn design.
 *                 Empty for a part whose role declares none, and a name left out reads the axis's default.
 * @param work     the work its forging delivered over the work the pattern asked
 *                 ({@code ForgeRhythm}): short is thick and heavy, overrun thin and light. 1 for a part not forged by hand.
 * @param attuned  the attuned (enchant) effects on it, by {@code Attunements.Effect#key}, the level as the value
 *                 (negative when it took on the mirror side).
 * @param state    live state of a food or workpiece part (a dish part's doneness, ingredient shares, cut; a bloom's scale
 *                 loss), by the names the system that wrote it defines. Not a measure of the part's geometry.
 * @param slots    the materials of a composed part beyond its first slot (which is {@code MadeOf.material}), by slot name
 *                 ({@link io.github.soundgoodizerfan.feedback.modular.ModularType.PartSlot}): a scope's lens, a blade's edge. A slot left out is
 *                 made of the main material.
 */
public record Measures(Map<String, Float> axes, float work, Map<String, Float> attuned, Map<String, Float> state, Map<String, Slot> slots) {

    /** One extra slot's material and the form it went in as. */
    public record Slot(ResourceLocation material, String form) {
        static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Slot::material),
                Codec.STRING.fieldOf("form").forGetter(Slot::form)
        ).apply(i, Slot::new));
        static final StreamCodec<ByteBuf, Slot> STREAM = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Slot::material, ByteBufCodecs.STRING_UTF8, Slot::form, Slot::new);
    }

    public static final Measures NONE = new Measures(Map.of(), 1f, Map.of(), Map.of(), Map.of());

    public Measures {
        axes = Map.copyOf(axes);
        attuned = Map.copyOf(attuned);
        state = Map.copyOf(state);
        slots = Map.copyOf(slots);
    }

    public Measures(Map<String, Float> axes, float work, Map<String, Float> attuned, Map<String, Float> state) {
        this(axes, work, attuned, state, Map.of());
    }

    /** The material in slot {@code name}, if it is a slot beyond the first and was filled apart from the main one. */
    public Optional<Slot> slot(String name) {
        return Optional.ofNullable(slots.get(name));
    }

    public Measures withSlots(Map<String, Slot> slots) {
        return new Measures(axes, work, attuned, state, slots);
    }

    public static Measures ofAxes(Map<String, Float> axes) {
        return new Measures(axes, 1f, Map.of(), Map.of());
    }

    public static Measures ofState(Map<String, Float> state) {
        return new Measures(Map.of(), 1f, Map.of(), state);
    }

    /** True when nothing is set: the part is exactly as its drawing and material say. */
    public boolean isNone() {
        return axes.isEmpty() && work == 1f && attuned.isEmpty() && state.isEmpty() && slots.isEmpty();
    }

    /** The as-built axis {@code name}, else {@code fallback}. */
    public float axis(String name, float fallback) {
        return axes.getOrDefault(name, fallback);
    }

    /** The state entry {@code name}, else {@code fallback}. */
    public float state(String name, float fallback) {
        return state.getOrDefault(name, fallback);
    }

    public Measures withAxis(String name, float value) {
        return new Measures(with(axes, name, value), work, attuned, state, slots);
    }

    public Measures withAxes(Map<String, Float> axes) {
        return new Measures(axes, work, attuned, state, slots);
    }

    public Measures withWork(float work) {
        return new Measures(axes, work, attuned, state, slots);
    }

    public Measures withAttuned(String key, float level) {
        return new Measures(axes, work, with(attuned, key, level), state, slots);
    }

    public Measures withAttuned(Map<String, Float> attuned) {
        return new Measures(axes, work, attuned, state, slots);
    }

    public Measures withState(String name, float value) {
        return new Measures(axes, work, attuned, with(state, name, value), slots);
    }

    public Measures withState(Map<String, Float> state) {
        return new Measures(axes, work, attuned, state, slots);
    }

    public Measures withoutAttuned() {
        return new Measures(axes, work, Map.of(), state, slots);
    }

    private static Map<String, Float> with(Map<String, Float> in, String key, float value) {
        Map<String, Float> out = new HashMap<>(in);
        out.put(key, value);
        return out;
    }

    private static final Codec<Map<String, Float>> FLOATS = Codec.unboundedMap(Codec.STRING, Codec.FLOAT);
    private static final StreamCodec<ByteBuf, Map<String, Float>> FLOATS_STREAM =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT);

    public static final Codec<Measures> CODEC = RecordCodecBuilder.create(i -> i.group(
            FLOATS.optionalFieldOf("axes", Map.of()).forGetter(Measures::axes),
            Codec.FLOAT.optionalFieldOf("work", 1f).forGetter(Measures::work),
            FLOATS.optionalFieldOf("attuned", Map.of()).forGetter(Measures::attuned),
            FLOATS.optionalFieldOf("state", Map.of()).forGetter(Measures::state),
            Codec.unboundedMap(Codec.STRING, Slot.CODEC).optionalFieldOf("slots", Map.of()).forGetter(Measures::slots)
    ).apply(i, Measures::new));

    public static final StreamCodec<ByteBuf, Measures> STREAM_CODEC = StreamCodec.composite(
            FLOATS_STREAM, Measures::axes,
            ByteBufCodecs.FLOAT, Measures::work,
            FLOATS_STREAM, Measures::attuned,
            FLOATS_STREAM, Measures::state,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, Slot.STREAM), Measures::slots,
            Measures::new);
}
