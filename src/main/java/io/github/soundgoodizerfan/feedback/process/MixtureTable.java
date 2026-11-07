// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

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

/** Loads every {@link Mixture} from {@code data/<namespace>/mixture/*.json}, synced to the client only for the recipe browser. */
public class MixtureTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "mixture";

    private static final MixtureTable INSTANCE = new MixtureTable();

    private List<Mixture> entries = List.of();

    private MixtureTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static MixtureTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Mixture> loaded = new ArrayList<>();
        json.forEach((id, element) -> Mixture.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad mixture row {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} mixture rows", entries.size());
    }

    public List<Mixture> entries() {
        return entries;
    }
}
