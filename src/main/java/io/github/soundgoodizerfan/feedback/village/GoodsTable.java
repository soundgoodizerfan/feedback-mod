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
 * Loads every shop from {@code data/<namespace>/goods/*.json}, flattened to {@link Good} lines in
 * file order. Synced to the client so the recipe browser can print it.
 */
public class GoodsTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "goods";

    private static final GoodsTable INSTANCE = new GoodsTable();

    private List<Good> entries = List.of();

    private GoodsTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static GoodsTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Good> loaded = new ArrayList<>();
        json.forEach((id, element) -> Good.File.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad goods file {}: {}", id, error))
                .ifPresent(file -> file.goods().forEach(l -> loaded.add(new Good(file.profession(), l.level(), l.item(), l.count(),
                        l.price(), l.stock(), l.standing(), l.enchantedBook(), l.blueprint())))));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} goods", entries.size());
    }

    public List<Good> entries() {
        return entries;
    }

    /** The shop of {@code profession} in file order. */
    public List<Good> forProfession(ResourceLocation profession) {
        return entries.stream().filter(g -> g.profession().equals(profession)).toList();
    }
}
