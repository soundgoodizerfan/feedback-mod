// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads every {@link Material} from {@code data/<namespace>/material/*.json}. */
public class MaterialTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "material";

    private static final MaterialTable INSTANCE = new MaterialTable();

    private Map<ResourceLocation, Material> materials = Map.of();

    private MaterialTable() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static MaterialTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        generation++;
        TreeMap<ResourceLocation, Material> loaded = new TreeMap<>();
        json.forEach((id, element) -> Material.CODEC.parse(JsonOps.INSTANCE, resolve(id, element, json, new HashSet<>()))
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad material {}: {}", id, error))
                .ifPresent(material -> loaded.put(id, material)));
        materials = java.util.Collections.unmodifiableSortedMap(loaded);
        Feedback.LOGGER.info("Loaded {} materials", materials.size());
        FormTable.get().invalidate();
        ThermalProcessTable.get().invalidate();
        CastingTable.get().invalidate();
    }

    /**
     * A file may name a {@code "parent"} material and override only what differs: objects merge
     * key by key down the chain, anything else (a number, a list) replaces the parent's whole value, and {@code null} removes the key.
     * A missing parent or a cycle is logged and the file loads as written.
     */
    private static JsonElement resolve(ResourceLocation id, JsonElement element, Map<ResourceLocation, JsonElement> all,
            Set<ResourceLocation> seen) {
        if (!element.isJsonObject() || !element.getAsJsonObject().has("parent")) {
            return element;
        }
        JsonObject child = element.getAsJsonObject().deepCopy();
        String name = child.remove("parent").getAsString();
        ResourceLocation parentId = ResourceLocation.tryParse(name.contains(":") ? name : id.getNamespace() + ":" + name);
        if (parentId == null || !all.containsKey(parentId) || !seen.add(id)) {
            Feedback.LOGGER.error("Material {} has unusable parent '{}'", id, name);
            return child;
        }
        JsonElement parent = resolve(parentId, all.get(parentId), all, seen);
        return parent.isJsonObject() ? merge(parent.getAsJsonObject(), child) : child;
    }

    private static JsonObject merge(JsonObject base, JsonObject over) {
        JsonObject out = base.deepCopy();
        over.entrySet().forEach(e -> {
            JsonElement old = out.get(e.getKey());
            if (e.getValue().isJsonNull()) {
                out.remove(e.getKey());
            } else if (old != null && old.isJsonObject() && e.getValue().isJsonObject()) {
                out.add(e.getKey(), merge(old.getAsJsonObject(), e.getValue().getAsJsonObject()));
            } else {
                out.add(e.getKey(), e.getValue());
            }
        });
        return out;
    }

    /** Bumped on every reload, so a cache over the table knows when it is stale. */
    private static int generation;

    public static int generation() {
        return generation;
    }

    /**
     * The loaded table, sorted by id, read-only, and the same instance until the next reload. Callers memoise on its identity
     * ({@link MaterialIngredient#locate}): a fresh copy per call once emptied that cache on every lookup, and a vessel's tick spent
     * most of the server's time re-walking every material.
     */
    public Map<ResourceLocation, Material> materials() {
        return materials;
    }
}
