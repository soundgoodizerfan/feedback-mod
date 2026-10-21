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
package io.github.soundgoodizerfan.feedback.weapon;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Loads every {@link GunOption} from {@code data/<namespace>/gun_option/*.json}, and keeps the
 * client's copy ({@code GunOptionSyncPayload}) beside it: the bake runs on both sides (the
 * Stand, the Gun Table and a tooltip all ask what a gun does).
 */
public class GunOptions extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "gun_option";

    private static final GunOptions INSTANCE = new GunOptions();

    private static Map<ResourceLocation, GunOption> client = Map.of();
    private Map<ResourceLocation, GunOption> options = Map.of();

    private GunOptions() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static GunOptions get() {
        return INSTANCE;
    }

    public static void setClient(Map<ResourceLocation, GunOption> incoming) {
        client = Map.copyOf(incoming);
    }

    public Map<ResourceLocation, GunOption> options() {
        return new TreeMap<>(options);
    }

    /** The options this JVM knows: the client's copy when it has one, else the server's. */
    public static Map<ResourceLocation, GunOption> known() {
        return client.isEmpty() ? INSTANCE.options : client;
    }

    public static Optional<GunOption> find(ResourceLocation id) {
        return Optional.ofNullable(known().get(id));
    }

    /** The option machined as {@code form}, if any. */
    public static Optional<GunOption> ofForm(String form) {
        return known().values().stream().filter(o -> o.form().equals(form)).findFirst();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, GunOption> loaded = new TreeMap<>();
        json.forEach((id, element) -> {
            var object = element.getAsJsonObject();
            object.addProperty("id", id.toString());
            GunOption.CODEC.parse(JsonOps.INSTANCE, object)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad gun option {}: {}", id, error))
                    .ifPresent(option -> loaded.put(id, option));
        });
        options = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} gun options", options.size());
    }
}
