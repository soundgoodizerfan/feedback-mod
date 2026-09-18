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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;

/**
 * Loads every {@link AlloyRecipe} from {@code data/<namespace>/alloy/*.json}. Same standing as
 * {@link CastingTable} -- server-only, no JEI card yet.
 */
public class AlloyTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "alloy";

    private static final AlloyTable INSTANCE = new AlloyTable();

    private List<AlloyRecipe> entries = List.of();

    private AlloyTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static AlloyTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<AlloyRecipe> loaded = new ArrayList<>();
        json.forEach((id, element) -> AlloyRecipe.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad alloy entry {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} alloy entries", entries.size());
    }

    /** The alloy a crucible's current mix satisfies, if any. */
    public Optional<Fluid> find(Map<Fluid, Integer> content, int total) {
        return entries.stream()
                .filter(entry -> entry.matches(content, total))
                .map(AlloyRecipe::result)
                .findFirst();
    }
}
