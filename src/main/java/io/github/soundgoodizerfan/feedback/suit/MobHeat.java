// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * How warm a living thing is by itself, by entity-type tag ({@code data/<ns>/body_heat/*.json}: {@code {"tag": "feedback:warm_blooded", "tu": 37}}).
 * Mobs carry no temperature in the simulation, so thermal vision reads this table: the hottest entry a mob matches wins, and a mob in
 * no listed tag reads {@link #DEFAULT_TU}. A modded creature is a line of JSON in a tag, and nothing names a mob. Server only: only the
 * server scans for the suit's thermal feed.
 */
public class MobHeat extends SimpleJsonResourceReloadListener {

    /** A mob in no listed class: a little above room, so it shows but is not mistaken for anything that is warm. */
    public static final float DEFAULT_TU = 24f;

    private record Entry(TagKey<EntityType<?>> tag, float tu) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                TagKey.codec(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(Entry::tag),
                Codec.FLOAT.fieldOf("tu").forGetter(Entry::tu)).apply(i, Entry::new));
    }

    private static final MobHeat INSTANCE = new MobHeat();

    private List<Entry> entries = List.of();

    private MobHeat() {
        super(new GsonBuilder().create(), "body_heat");
    }

    public static MobHeat get() {
        return INSTANCE;
    }

    /** The temperature {@code entity} reads at: the hottest listed class it is in, else {@link #DEFAULT_TU}. */
    public float tu(Entity entity) {
        float best = Float.NaN;
        for (Entry e : entries)
            if ((Float.isNaN(best) || e.tu() > best) && entity.getType().is(e.tag()))
                best = e.tu();
        return Float.isNaN(best) ? DEFAULT_TU : best;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Entry> loaded = new ArrayList<>();
        json.forEach((id, element) -> Entry.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad body heat {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} body heat classes", entries.size());
    }
}
