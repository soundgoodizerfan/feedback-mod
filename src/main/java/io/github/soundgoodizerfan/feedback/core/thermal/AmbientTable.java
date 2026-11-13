// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * The temperature everything decays towards, by dimension. Loaded from
 * {@code data/<namespace>/ambient_temperature/<path>.json}, where {@code <namespace>:<path>} is
 * the dimension's own id -- {@code data/minecraft/ambient_temperature/the_nether.json} is the
 * Nether -- holding {@code {"temperature": <Tu>}}.
 *
 * <h2>Why a dimension and not a biome</h2>
 * The first version of {@link ThermalTuning#AMBIENT_TU} said one figure everywhere, on the grounds that
 * a desert crucible two degrees hotter than a taiga one is a variable nobody can act on. That
 * argument was right about biomes and wrong about dimensions: siting the foundry in the Nether is
 * a decision the player can actually make, it costs something (the trip, the fuel, the ghasts),
 * and it buys a real if modest thing -- a vessel's equilibrium is a weighted mean of its fire and
 * its room, so a hotter room pulls it nearer the flame's ceiling and gets it there sooner. It
 * never lets it pass the flame, because that is a property of the difference-driven model and
 * not of this table. The second answer to "closer to the ceiling" beside insulating at home.
 *
 * <h2>Nothing listed means {@link ThermalTuning#AMBIENT_TU}</h2>
 * Keyed by dimension id and nothing else, so a modded dimension is a line of JSON and an
 * unlisted one is an ordinary room.
 *
 * <h2>The ceiling on a figure</h2>
 * An entry must not exceed {@link ThermalTuning#WARM_TU}. A workpiece's heat component is cleared when
 * it reads at or below that, and one whose room is warmer would never read cold: every ingot ever
 * heated there would carry a stamp forever and refuse to stack. Rejected at load, with the reason.
 *
 * <p>Synced to the client (see {@code AmbientSyncPayload}) because a tooltip reads a workpiece's
 * temperature there and its cooling floor is this figure; nothing on the client simulates with it.
 */
public class AmbientTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "ambient_temperature";

    private static final Codec<Float> ENTRY = Codec.floatRange(0f, ThermalTuning.WARM_TU.value())
            .fieldOf("temperature").codec();

    private static final AmbientTable INSTANCE = new AmbientTable();

    // Floats, not Tu, per the unit rule: a held wrapper outlives its method. Wrapped in of().
    private Map<ResourceLocation, Float> byDimension = Map.of();

    private AmbientTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static AmbientTable get() {
        return INSTANCE;
    }

    /** Ambient in this level's dimension. A null level -- a block entity not yet placed -- is the default. */
    public static Tu at(@Nullable Level level) {
        return level == null ? ThermalTuning.AMBIENT_TU : of(level.dimension().location());
    }

    public static Tu of(ResourceLocation dimension) {
        Float found = INSTANCE.byDimension.get(dimension);
        return found == null ? ThermalTuning.AMBIENT_TU : new Tu(found);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Float> loaded = new HashMap<>();
        json.forEach((id, element) -> ENTRY.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad ambient temperature {}: {}", id, error))
                .ifPresent(tu -> loaded.put(id, tu)));
        byDimension = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} ambient temperatures", byDimension.size());
    }

    public Map<ResourceLocation, Float> entries() {
        return byDimension;
    }

    /** Client side of the sync. */
    public void set(Map<ResourceLocation, Float> incoming) {
        byDimension = Map.copyOf(incoming);
    }
}
