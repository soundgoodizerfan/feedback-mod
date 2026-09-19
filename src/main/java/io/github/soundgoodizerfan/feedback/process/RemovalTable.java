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

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

/**
 * Loads every {@link Removal} from {@code data/<namespace>/removal/*.json} -- shaped exactly like
 * {@link DeformationTable} (a plain {@code SimpleJsonResourceReloadListener}, no vanilla {@code
 * RecipeType}), per the settled precedent in `CLAUDE.md` §3. Matches on the input item alone: a
 * Drill Press, a Lathe and a Grinding Wheel all read the same table, because turning, drilling
 * and grinding are one primitive rather than three competing actions on one input.
 */
public class RemovalTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "removal";

    private static final RemovalTable INSTANCE = new RemovalTable();

    private List<Removal> entries = List.of();

    private RemovalTable() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static RemovalTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Removal> loaded = new ArrayList<>();
        json.forEach((id, element) -> Removal.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad removal {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} removal entries", entries.size());
    }

    public List<Removal> entries() {
        return entries;
    }

    public Optional<Removal> find(ItemStack stack) {
        for (Removal entry : entries)
            if (entry.input().test(stack))
                return Optional.of(entry);
        return Optional.empty();
    }
}
