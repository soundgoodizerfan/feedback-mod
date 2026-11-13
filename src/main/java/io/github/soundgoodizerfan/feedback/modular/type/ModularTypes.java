// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.type;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.ModularAssembly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * Loads every {@link ModularType} from {@code data/<namespace>/modular_type/*.json}, and keeps the
 * client's copy ({@code ModularTypeSyncPayload}) beside it: the Tool Table asks on both sides
 * whether it holds a pickaxe, exactly as the gun recipe asks the material table.
 *
 * <p>A file may name a {@code parent}; it then starts from the parent's file and replaces whatever
 * keys it writes (Silent Gear's gear-type tree, flattened to one level of override).
 */
public class ModularTypes extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "modular_type";

    private static final ModularTypes INSTANCE = new ModularTypes();

    private static Map<ResourceLocation, ModularType> client = Map.of();
    private Map<ResourceLocation, ModularType> types = Map.of();
    private static int generation;

    private ModularTypes() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static ModularTypes get() {
        return INSTANCE;
    }

    private static Runnable listener = () -> {
    };

    public static void setClient(Map<ResourceLocation, ModularType> incoming) {
        client = Map.copyOf(incoming);
        generation++;
        listener.run();
    }

    /** Called whenever the client's copy is replaced. One listener is enough; only JEI wants this. */
    public static void onChanged(Runnable onChanged) {
        listener = onChanged;
        onChanged.run();
    }

    /** Bumped on every reload, so a cache over the table knows it is stale. */
    public static int generation() {
        return generation;
    }

    public Map<ResourceLocation, ModularType> types() {
        return new TreeMap<>(types);
    }

    /** The types this JVM knows: the client's copy when it has one, else the server's -- for a tooltip or a name. */
    public static Map<ResourceLocation, ModularType> known() {
        return client.isEmpty() ? INSTANCE.types() : new TreeMap<>(client);
    }

    /** One type as {@link #known()} has it, without copying the table: for a hot path that asks about one stack. */
    public static ModularType known(ResourceLocation id) {
        return client.isEmpty() ? INSTANCE.types.get(id) : client.get(id);
    }

    /** The types as {@code level}'s side knows them. */
    public static Map<ResourceLocation, ModularType> on(Level level) {
        return level.isClientSide ? new TreeMap<>(client) : INSTANCE.types();
    }

    public static Optional<ModularType> find(Level level, ResourceLocation id) {
        return Optional.ofNullable(on(level).get(id));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, ModularType> loaded = new TreeMap<>();
        json.forEach((id, element) -> ModularType.CODEC.parse(JsonOps.INSTANCE, resolve(json, element.getAsJsonObject(), 0))
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad modular type {}: {}", id, error))
                .ifPresent(type -> loaded.put(id, type)));
        types = Map.copyOf(loaded);
        generation++;
        // The Tool Table infers the type from the parts, so two types must never take the same parts.
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        long auditStart = System.nanoTime();
        (materials.isEmpty() ? ModularAssembly.audit(loaded) : ModularAssembly.audit(loaded, materials)).forEach(problem -> Feedback.LOGGER.error("Modular types: {}", problem));
        Feedback.LOGGER.info("Modular type audit took {} ms", (System.nanoTime() - auditStart) / 1_000_000);
        // The Blueprint Table's part list shows this line on hover; a role without one shows nothing.
        var lang = net.minecraft.locale.Language.getInstance();
        loaded.forEach((id, type) -> type.roles().values().forEach(role -> {
            if (!lang.has("modular.feedback.role." + role.name() + ".tip"))
                Feedback.LOGGER.warn("Modular type {}: role '{}' has no modular.feedback.role.{}.tip description", id, role.name(), role.name());
        }));
        Feedback.LOGGER.info("Loaded {} modular types", types.size());
    }

    private static JsonObject resolve(Map<ResourceLocation, JsonElement> all, JsonObject own, int depth) {
        if (!own.has("parent") || depth > 4)
            return own;
        ResourceLocation parentId = ResourceLocation.parse(own.get("parent").getAsString());
        JsonElement parent = all.get(parentId);
        if (parent == null) {
            Feedback.LOGGER.error("Modular type names a missing parent {}", parentId);
            return own;
        }
        JsonObject merged = resolve(all, parent.getAsJsonObject(), depth + 1).deepCopy();
        own.entrySet().forEach(e -> {
            if (!e.getKey().equals("parent"))
                merged.add(e.getKey(), e.getValue());
        });
        return merged;
    }
}
