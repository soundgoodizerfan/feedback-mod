// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.combat;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * The fixed temperature of a thing that burns a shield, by damage-type tag
 * ({@code data/<ns>/shield_source/*.json}: {@code {"tag": "minecraft:is_fire", "tu": 760}}).
 * A shield's face is heated towards the hottest source a blow matches, and never past it (the
 * difference law, {@link ShieldHeat}). Keyed by tag so a modded fire is a line of JSON and nothing
 * names a mob (philosophy 12). Server only: only the server heats a face.
 */
public class ShieldSources extends SimpleJsonResourceReloadListener {

    private record Entry(TagKey<DamageType> tag, float tu) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                TagKey.codec(Registries.DAMAGE_TYPE).fieldOf("tag").forGetter(Entry::tag),
                Codec.FLOAT.fieldOf("tu").forGetter(Entry::tu)).apply(i, Entry::new));
    }

    private static final ShieldSources INSTANCE = new ShieldSources();

    private List<Entry> entries = List.of();

    private ShieldSources() {
        super(new GsonBuilder().create(), "shield_source");
    }

    public static ShieldSources get() {
        return INSTANCE;
    }

    /** The hottest source temperature {@code source} matches, or 0 for none. */
    public float tu(DamageSource source) {
        float best = 0f;
        for (Entry e : entries)
            if (e.tu() > best && source.is(e.tag()))
                best = e.tu();
        return best;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Entry> loaded = new ArrayList<>();
        json.forEach((id, element) -> Entry.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad shield source {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} shield heat sources", entries.size());
    }
}
