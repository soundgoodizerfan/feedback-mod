// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * Air density by dimension, relative to the overworld's (1.0). Loaded from
 * {@code data/<namespace>/air_density/<path>.json}, {@code <namespace>:<path>} being the dimension's
 * own id, holding {@code {"density": <number>, "oxygen": <bool>}}. Density sets drag and lift; zero is
 * vacuum. Oxygen (default true) is whether an air-breathing thruster has anything to burn with, so a
 * thin atmosphere with no oxygen (the End) still drags and lifts a little. An unlisted dimension is an
 * ordinary atmosphere (1.0, oxygen), so a modded dimension is a line of JSON. Server only: the physics step and the thruster are the readers.
 */
public class AirDensityTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "air_density";

    private record Entry(float density, boolean oxygen) {
    }

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0f, 100f).fieldOf("density").forGetter(Entry::density),
            Codec.BOOL.optionalFieldOf("oxygen", true).forGetter(Entry::oxygen)).apply(i, Entry::new));
    private static final Entry ORDINARY = new Entry(1f, true);
    private static final AirDensityTable INSTANCE = new AirDensityTable();

    // Read on the physics thread, replaced on reload.
    private volatile Map<ResourceLocation, Entry> byDimension = Map.of();

    private AirDensityTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static AirDensityTable get() {
        return INSTANCE;
    }

    public static float at(Level level) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY).density();
    }

    /** Whether the air here carries oxygen to burn. */
    public static boolean oxygen(Level level) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY).oxygen();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Entry> loaded = new HashMap<>();
        json.forEach((id, element) -> ENTRY.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad air density {}: {}", id, error))
                .ifPresent(d -> loaded.put(id, d)));
        byDimension = Map.copyOf(loaded);
    }
}
