// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.geology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads every {@link Basin} from {@code data/<ns>/basin/*.json}. Server-only, like {@code OreVeinTable}. */
public class BasinTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "basin";

    private static final BasinTable INSTANCE = new BasinTable();

    private List<Basin> entries = List.of();

    private BasinTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static BasinTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Basin> loaded = new ArrayList<>();
        new java.util.TreeMap<>(json).forEach((id, element) -> Basin.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad basin {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} basins", entries.size());
    }

    public List<Basin> entries() {
        return entries;
    }
}
