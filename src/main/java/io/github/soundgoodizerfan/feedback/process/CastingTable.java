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
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Loads every {@link Casting} from {@code data/<namespace>/casting/*.json}.
 * {@link io.github.soundgoodizerfan.feedback.item.MoldItem} reads it server-side; the client's
 * copy ({@code net/CastingSyncPayload}) is only for the recipe browser.
 */
public class CastingTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "casting";

    private static final CastingTable INSTANCE = new CastingTable();

    private List<Casting> entries = List.of();

    private CastingTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static CastingTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Casting> loaded = new ArrayList<>();
        json.forEach((id, element) -> Casting.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad casting entry {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} casting entries", entries.size());
    }

    public List<Casting> entries() {
        return entries;
    }

    /** The casting entry a mold's current contents satisfy, if any. */
    public Optional<Casting> find(FluidStack contents, String mold) {
        if (contents.isEmpty())
            return Optional.empty();
        return entries.stream().filter(entry -> entry.mold().equals(mold) && entry.matches(contents)).findFirst();
    }
}
