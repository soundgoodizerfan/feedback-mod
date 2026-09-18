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
 * Loads every {@link Deformation} from {@code data/<namespace>/deformation/*.json}.
 *
 * <h2>Why this is not a vanilla recipe type</h2>
 * Philosophy 15 wants cross-mod support authored as a table rather than as code, which a datapack
 * gives us. But vanilla's recipe machinery brings a recipe book, a crafting-grid shaped matching
 * API and automatic client sync, none of which apply: there is no grid, nothing is ever "crafted",
 * and only the server ever needs to know.
 * <p>
 * There is a pleasing consequence of staying out of the recipe system. A recipe browser genuinely
 * has nothing to show for the Mechanical Hammer -- which is correct, because the hammer does not
 * know how to make anything. It hits what is in front of it.
 */
public class DeformationTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "deformation";

    private static final DeformationTable INSTANCE = new DeformationTable();

    private List<Deformation> entries = List.of();

    private DeformationTable() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static DeformationTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Deformation> loaded = new ArrayList<>();
        json.forEach((id, element) -> Deformation.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad deformation {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} deformation entries", entries.size());
    }

    /** Every entry, in load order. Used to ship the table to the client for display. */
    public List<Deformation> entries() {
        return entries;
    }

    /** What this stack turns into under the given {@link Operation}, if anything does. */
    public Optional<Deformation> find(ItemStack stack, Operation operation) {
        for (Deformation entry : entries)
            if (entry.operation() == operation && entry.input().test(stack))
                return Optional.of(entry);
        return Optional.empty();
    }
}
