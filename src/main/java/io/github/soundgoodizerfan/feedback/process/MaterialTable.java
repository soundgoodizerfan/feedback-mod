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
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
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
        Map<ResourceLocation, Material> loaded = new TreeMap<>();
        json.forEach((id, element) -> Material.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad material {}: {}", id, error))
                .ifPresent(material -> loaded.put(id, material)));
        materials = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} materials", materials.size());
        FormTable.get().invalidate();
    }

    /** Sorted by id, so compiled entries come out in the same order every reload. */
    /** Bumped on every reload, so a cache over the table knows when it is stale. */
    private static int generation;

    public static int generation() {
        return generation;
    }

    public Map<ResourceLocation, Material> materials() {
        return new TreeMap<>(materials);
    }
}
