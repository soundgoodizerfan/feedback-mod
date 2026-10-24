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
 * Assets under src/main/resources are NOT covered by this licence.
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

/** Loads every {@link PhaseChange} from {@code data/<namespace>/phase_change/*.json}; synced for the recipe browser. */
public class PhaseChangeTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "phase_change";

    private static final PhaseChangeTable INSTANCE = new PhaseChangeTable();

    private List<PhaseChange> entries = List.of();

    private PhaseChangeTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static PhaseChangeTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<PhaseChange> loaded = new ArrayList<>();
        json.forEach((id, element) -> PhaseChange.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad phase change {}: {}", id, error))
                .ifPresent(loaded::add));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} phase changes", entries.size());
    }

    public List<PhaseChange> entries() {
        return entries;
    }

    /** What {@code liquid} boils into, if anything. */
    public Optional<PhaseChange> boiling(FluidStack liquid) {
        return liquid.isEmpty() ? Optional.empty() : entries.stream().filter(e -> e.boils(liquid)).findFirst();
    }

    /** What {@code gas} condenses into, if anything. */
    public Optional<PhaseChange> condensing(FluidStack gas) {
        return gas.isEmpty() ? Optional.empty() : entries.stream().filter(e -> e.condenses(gas)).findFirst();
    }
}
