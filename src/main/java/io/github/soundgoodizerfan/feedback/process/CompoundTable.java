// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads every {@link Compound} from {@code data/<namespace>/compound/*.json}, keyed by file name (the name a food's {@code compounds} uses). Meal effects are applied on the server; the direction, taste and pull ship to the client ({@code CompoundSyncPayload}) for the brewing preview. */
public class CompoundTable extends SimpleJsonResourceReloadListener {

    private static final CompoundTable INSTANCE = new CompoundTable();

    private Map<String, Compound> compounds = Map.of();
    private static Map<String, Compound> client = Map.of();

    private CompoundTable() {
        super(new com.google.gson.GsonBuilder().create(), "compound");
    }

    public static CompoundTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<String, Compound> loaded = new TreeMap<>();
        json.forEach((id, element) -> Compound.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad compound {}: {}", id, error))
                .ifPresent(c -> loaded.put(id.getPath(), c)));
        compounds = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} compounds", compounds.size());
    }

    /** The compound a name stands for: the client's copy when it has one (the brewing screen previews with it), else the server's. */
    public Optional<Compound> find(String name) {
        return Optional.ofNullable((client.isEmpty() ? compounds : client).get(name));
    }

    public Map<String, Compound> all() {
        return client.isEmpty() ? compounds : client;
    }

    public static void setClient(Map<String, Compound> incoming) {
        client = Map.copyOf(incoming);
    }
}
