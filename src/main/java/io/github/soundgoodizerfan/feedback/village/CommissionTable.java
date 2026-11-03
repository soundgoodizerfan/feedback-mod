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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.HashMap;
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
 * Loads every {@link CommissionProfile} from {@code data/<namespace>/commission_profile/*.json}.
 * Server only: the client is sent each order already worded, never the spans behind it.
 */
public class CommissionTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "commission_profile";

    private static final CommissionTable INSTANCE = new CommissionTable();

    private Map<ResourceLocation, List<CommissionProfile.Entry>> byProfession = Map.of();

    private CommissionTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static CommissionTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, List<CommissionProfile.Entry>> loaded = new HashMap<>();
        json.forEach((id, element) -> CommissionProfile.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad commission profile {}: {}", id, error))
                .ifPresent(p -> loaded.computeIfAbsent(p.profession(), k -> new ArrayList<>()).addAll(p.orders())));
        byProfession = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded commission profiles for {} professions", byProfession.size());
    }

    /** What {@code profession} commissions, in file order; empty for a profession that commissions nothing. */
    public List<CommissionProfile.Entry> forProfession(ResourceLocation profession) {
        return byProfession.getOrDefault(profession, List.of());
    }
}
