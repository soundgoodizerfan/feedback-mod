// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

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
 * Loads every pool of things villagers buy from {@code data/<namespace>/buys/*.json}, flattened to
 * {@link Want} lines in file order. Synced to the client so the recipe browser can print it.
 */
public class BuysTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "buys";

    private static final BuysTable INSTANCE = new BuysTable();

    private List<Want> entries = List.of();

    private BuysTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static BuysTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Want> loaded = new ArrayList<>();
        json.forEach((id, element) -> Want.File.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad buys file {}: {}", id, error))
                .ifPresent(file -> file.wants().forEach(l -> {
                    if (l.food() == (l.item() != net.minecraft.world.item.Items.AIR))
                        Feedback.LOGGER.error("Buys line in {} needs exactly one of item or food", id);
                    else
                        loaded.add(new Want(file.profession(), l.item(), l.food(), l.price(), l.max(), l.regional()));
                })));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} wants", entries.size());
    }

    public List<Want> entries() {
        return entries;
    }
}
