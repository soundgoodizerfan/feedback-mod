// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
