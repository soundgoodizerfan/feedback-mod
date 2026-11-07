// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

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

/**
 * Loads every {@link OreVein} from {@code data/<namespace>/ore_vein/*.json} (mechanics §9.2).
 * Server-only: worldgen runs on the server, so nothing is synced to the client.
 */
public class OreVeinTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "ore_vein";

    private static final OreVeinTable INSTANCE = new OreVeinTable();

    private List<OreVein> entries = List.of();

    private OreVeinTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static OreVeinTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<OreVein> loaded = new ArrayList<>();
        new java.util.TreeMap<>(json).forEach((id, element) -> OreVein.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad ore vein {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} ore veins", entries.size());
    }

    public List<OreVein> entries() {
        return entries;
    }
}
