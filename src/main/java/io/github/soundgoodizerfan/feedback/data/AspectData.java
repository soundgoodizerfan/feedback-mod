// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspects for the Thaumaturge mod (optional; the file loads only when it is present).
 *
 * <p>Thaumaturge reads a NeoForge data map, {@code data/thaumaturge/data_maps/item/base_aspects.json}, keyed by item
 * id or {@code #tag}, one list of aspects per key that replaces any other for the same key. A pack cannot split it,
 * so this one provider writes the whole file: a baseline for every generated material-form item (and every item a
 * material lists under a form), worked out from the material's own figures, then the hand-written pins of
 * {@code data/feedback/aspect_pins.json} laid over it, a pin replacing the whole entry for its key.
 *
 * <p>Taken from Thaumaturge (source-available, publishing barred, dormancy converts it to MIT): the data-map file format and the
 * aspect ids, as our own JSON it reads. Rejected: its code, assets and any of its values. Items with neither a
 * baseline nor a pin are left to Thaumaturge's own recipe derivation.
 */
public final class AspectData implements DataProvider {

    private static final Logger LOG = LoggerFactory.getLogger("feedback/aspects");

    /** Thaumaturge's own cap per aspect per item. */
    private static final int MAX_AMOUNT = 500;

    private static final float CONDUCTOR = 0.3f;
    private static final float REFLECTIVE = 0.7f;
    private static final float TOXIC = 0.5f;
    private static final float HARD = 6f;
    /** An animal fibre lighter than this (Mu/mB) is down, not hide or wool. */
    private static final float LIGHT_FIBRE = 0.1f;
    /** A fluid boiling below this is a gas at the world's ambient temperature (Tu, as written in the material files). */
    private static final float GAS_BOILING = 20f;

    /** The pinned keys of the current run, for the review's list of bare items. */
    private static java.util.Set<String> PINNED = java.util.Set.of();
    /** Food perk id to the aspects it adds (base weight), from {@code food_perks} in the pin file. */
    private static Map<String, List<Aspect>> FOOD_PERKS = Map.of();

    private final PackOutput output;

    public AspectData(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        FOOD_PERKS = table("food_perks");
        Map<ResourceLocation, List<Aspect>> entries = baseline();
        // A group's items first, then explicit pins over them.
        Map<String, List<Aspect>> pins = new TreeMap<>(groups());
        pins.putAll(table("pins"));
        PINNED = pins.keySet();
        // Engineered items with neither a baseline nor a pin: what their recipe is made of (Thaumaturge derives from
        // vanilla recipe types only, and never reads feedback:engineering).
        engineered(entries, pins.keySet());
        review(entries);
        // A pin naming an item that does not exist is a typo: it would ship as a silent no-op.
        pins.keySet().stream().filter(k -> !k.startsWith("#") && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(k)))
                .forEach(k -> LOG.error("aspect pin names an unknown item: {}", k));

        JsonObject values = new JsonObject();
        // Sorted, so the file is stable between runs; a pin replaces the baseline entry for its key.
        Map<String, List<Aspect>> merged = new TreeMap<>();
        entries.forEach((id, list) -> merged.put(id.toString(), list));
        merged.putAll(pins);
        merged.forEach((key, list) -> values.add(key, toJson(list)));

        JsonObject root = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject loaded = new JsonObject();
        loaded.addProperty("type", "neoforge:mod_loaded");
        loaded.addProperty("modid", "thaumaturge");
        conditions.add(loaded);
        root.add("neoforge:conditions", conditions);
        root.add("values", values);

        Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("thaumaturge/data_maps/item/base_aspects.json");

        // The same format keyed by entity type: what a scan of one of our entities yields (the "entities" table of the pin file).
        Map<String, List<Aspect>> entities = new TreeMap<>(table("entities"));
        entities.keySet().stream().filter(k -> !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(k)))
                .forEach(k -> LOG.error("entity aspect names an unknown entity type: {}", k));
        JsonObject entityValues = new JsonObject();
        entities.forEach((key, list) -> entityValues.add(key, toJson(list)));
        JsonObject entityRoot = new JsonObject();
        entityRoot.add("neoforge:conditions", conditions);
        entityRoot.add("values", entityValues);
        Path entityPath = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("thaumaturge/data_maps/entity_type/entity_aspects.json");
        return CompletableFuture.allOf(DataProvider.saveStable(cache, root, path), DataProvider.saveStable(cache, entityRoot, entityPath));
    }

    /** An aspect and how much of it a form carries. */
    private record Aspect(String id, int amount) {}

    private static JsonArray toJson(List<Aspect> list) {
        JsonArray array = new JsonArray();
        for (Aspect a : list) {
            JsonObject o = new JsonObject();
            o.addProperty("aspect", a.id());
            o.addProperty("amount", a.amount());
            array.add(o);
        }
        return array;
    }

    // ------------------------------------------------------------------ baseline

    private static Map<ResourceLocation, List<Aspect>> baseline() {
        Map<ResourceLocation, List<Aspect>> out = new LinkedHashMap<>();
        for (FormItems.Generated g : FormItems.all()) {
            Material material = FormItems.materials().get(g.material());
            if (material == null)
                continue;
            // No aspects worked out (a plastic, a fibre): no entry, so Thaumaturge derives it from the recipe instead of
            // an empty list replacing that.
            List<Aspect> list = forForm(material, g.form());
            if (!list.isEmpty())
                out.put(BuiltInRegistries.ITEM.getKey(g.item().get()), list);
        }
        // Items a material lists under a form by id (hand-registered or vanilla). Shared part items carry their
        // material on the stack, which Thaumaturge cannot see, so they are skipped.
        FormItems.materials().forEach((matId, material) -> material.forms().forEach((form, id) -> {
            FormType type = FormItems.types().get(form);
            // "raw" is the material as it comes (a loaf, an onion, a lump of rock): no form type, but a whole item.
            if ((type == null && !form.equals("raw")) || (type != null && type.part()) || out.containsKey(id)
                    || !BuiltInRegistries.ITEM.containsKey(id))
                return;
            List<Aspect> list = forForm(material, form);
            if (!list.isEmpty())
                out.put(id, list);
        }));
        return out;
    }

    private static List<Aspect> forForm(Material material, String form) {
        float volume = (float) FormItems.volume(form) / FormType.DEFAULT_MB;
        List<Aspect> list = new ArrayList<>();
        aspects(material, form).forEach((id, perIngot) -> list.add(new Aspect("thaumaturge:" + id,
                Math.max(1, Math.min(MAX_AMOUNT, Math.round(perIngot * volume))))));
        return list;
    }

    /**
     * What the material is made of, by its own figures, each aspect weighted by the figure that earned it (so two
     * metals never carry the same numbers): the amount an ingot-sized form carries. The form only adds the powder aspect.
     */
    public static Map<String, Float> aspects(Material material, String form) {
        Map<String, Float> out = new LinkedHashMap<>();
        Material.Kind kind = material.kind().orElse(null);
        float density = material.density().muPerMb();
        float hardness = material.hardness().value();
        if (kind != null) {
            switch (kind) {
                case METAL, METALLOID -> out.put("metallum", clamp(2f + density / 2.5f));
                case MINERAL, CERAMIC, GRANULAR -> out.put("terra", clamp(2f + density));
                case GLASS, CRYSTAL -> out.put("vitreus", clamp(3f + hardness / 12f));
                case FLUID -> out.put(material.thermal().boilingPoint().map(t -> t.value() < GAS_BOILING).orElse(false)
                        ? "aer" : "aqua", clamp(2f + density * 2f));
                case FLOWER, HERB, FUNGUS, GRAIN, VEGETABLE, FRUIT, DOUGH -> out.put("herba", clamp(2f + density * 2f));
                case ANIMAL, SEAFOOD -> {
                    out.put("bestia", clamp(2f + density * 2f));
                    out.put("victus", clamp(2f + density));
                }
                default -> { }
            }
        }
        if (material.traits().contains(Material.Trait.GEMSTONE) || material.traits().contains(Material.Trait.TRANSPARENT))
            out.putIfAbsent("vitreus", clamp(3f + hardness / 12f));
        float conductivity = material.electrical().conductivity().orElse(0f);
        if (conductivity > CONDUCTOR)
            out.put("potentia", clamp(2f + conductivity * 5f));
        float reflectivity = material.separation().reflectivity().orElse(0f);
        if (reflectivity > REFLECTIVE)
            out.put("lux", clamp(2f + (reflectivity - REFLECTIVE) * 20f));
        // A fluid's combustion entry is often what it oxidises with (air, oxygen), so only the flammable trait counts there.
        if (material.traits().contains(Material.Trait.FLAMMABLE) || (material.combustion().isPresent() && kind != Material.Kind.FLUID))
            out.put("ignis", clamp(2f + material.combustion().flatMap(Material.Combustion::flameTemperature)
                    .map(t -> t.value() / 400f).orElse(1.5f)));
        if (material.traits().contains(Material.Trait.WOODY))
            out.putIfAbsent("herba", clamp(2f + density * 3f));
        float toxicity = material.toxicity().orElse(0f);
        if (toxicity > TOXIC)
            out.put("mortuus", clamp(2f + toxicity * 2f));
        if (hardness > HARD || material.traits().contains(Material.Trait.ALLOY))
            out.put("ordo", clamp(2f + hardness / 4f));
        if (material.separation().magnetic() || material.traits().contains(Material.Trait.LUMINOUS))
            out.put("motus", clamp(2f + (material.separation().magnetic() ? 1.5f : 0f) + material.separation().diamagnetism()));
        // Made, not found: a polymer or composite is craft; one that insulates also wards, an elastic one (moulded into
        // a tyre) moves, and a bonded composite holds its order.
        if (kind == Material.Kind.POLYMER || kind == Material.Kind.COMPOSITE) {
            out.put("fabrico", clamp(3f + hardness / 3f));
            if (material.electrical().insulation().isPresent())
                out.put("praemunio", clamp(2f + hardness / 6f));
            if (material.traits().contains(Material.Trait.TIRE))
                out.put("motus", clamp(3f + Math.max(0f, 1f - hardness)));
            if (kind == Material.Kind.COMPOSITE)
                out.putIfAbsent("ordo", clamp(2f + hardness / 4f));
        }
        // Food is life; what it does to the eater (its perks) lays aspects over that, by the hand-edited
        // food_perks table of the pin file, stronger perks heavier.
        material.food().ifPresent(food -> {
            out.put("victus", clamp(2f + (food.fat() + food.fibre()) * 6f));
            food.perks().forEach((perk, strength) -> FOOD_PERKS.getOrDefault(perk, List.of())
                    .forEach(a -> out.merge(a.id().replace("thaumaturge:", ""), a.amount() + strength * 8f, (x, y) -> clamp(x + y))));
        });
        // Light fibre that holds air (feather down) lifts: flight.
        if (kind == Material.Kind.ANIMAL && material.traits().contains(Material.Trait.FIBROUS) && density < LIGHT_FIBRE)
            out.put("volatus", clamp(4f + (LIGHT_FIBRE - density) * 30f));
        if (form.equals("dust") || form.equals("crushed") || kind == Material.Kind.GRANULAR)
            out.putIfAbsent("perditio", clamp(2f + hardness / 20f));
        return out;
    }

    /** An ingot-sized weight stays within Thaumaturge's own habit (a few to about ten). */
    private static float clamp(float weight) {
        return Math.max(1f, Math.min(10f, weight));
    }

    // ------------------------------------------------------------------ engineered items

    /** The craft an assembly adds to what it is made of. */
    private static final int ASSEMBLY_FABRICO = 2;

    /**
     * Gives every {@code feedback:engineering} result that has no entry and no pin the sum of its item ingredients' aspects
     * (the baseline and earlier results, counted by how many cells carry them), divided by the result count, plus a little
     * {@code fabrico} for the assembly. Tag and form-matching ingredients, fasteners and tools carry no aspects of their own
     * here, so a recipe made only of those gets no entry and is left to the pins. Recipes are read from the source tree
     * beside the output folder, so a run needs no loaded recipe manager.
     */
    private void engineered(Map<ResourceLocation, List<Aspect>> entries, java.util.Set<String> pinned) {
        Path recipes = output.getOutputFolder().getParent().getParent()
                .resolve("main/resources/data/feedback/recipe");
        if (!java.nio.file.Files.isDirectory(recipes))
            return;
        // Results of earlier recipes feed later ones (a housing inside a machine), so repeat until nothing new settles.
        Map<ResourceLocation, JsonObject> pending = new TreeMap<>();
        try (var files = java.nio.file.Files.list(recipes)) {
            for (Path file : (Iterable<Path>) files.sorted()::iterator) {
                if (!file.toString().endsWith(".json"))
                    continue;
                JsonObject recipe = JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
                if (!"feedback:engineering".equals(recipe.get("type").getAsString()))
                    continue;
                ResourceLocation result = ResourceLocation.parse(recipe.getAsJsonObject("result").get("id").getAsString());
                if (entries.containsKey(result) || pinned.contains(result.toString()) || !BuiltInRegistries.ITEM.containsKey(result))
                    continue;
                pending.put(result, recipe);
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("engineering recipes are unreadable", ex);
        }
        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                List<Aspect> derived = fromRecipe(e.getValue(), entries);
                if (derived.isEmpty())
                    continue;
                entries.put(e.getKey(), derived);
                it.remove();
                progress = true;
            }
        }
    }

    private static List<Aspect> fromRecipe(JsonObject recipe, Map<ResourceLocation, List<Aspect>> entries) {
        JsonObject key = recipe.getAsJsonObject("key");
        Map<String, Integer> cells = new TreeMap<>();
        for (JsonElement row : recipe.getAsJsonArray("pattern"))
            for (char c : row.getAsString().toCharArray())
                if (c != ' ')
                    cells.merge(String.valueOf(c), 1, Integer::sum);
        Map<String, Integer> sum = new TreeMap<>();
        cells.forEach((symbol, count) -> {
            JsonObject ingredient = key.has(symbol) && key.get(symbol).isJsonObject() ? key.getAsJsonObject(symbol) : null;
            if (ingredient == null || !ingredient.has("item"))
                return;
            List<Aspect> aspects = entries.get(ResourceLocation.parse(ingredient.get("item").getAsString()));
            if (aspects != null)
                aspects.forEach(a -> sum.merge(a.id(), a.amount() * count, Integer::sum));
        });
        if (sum.isEmpty())
            return List.of();
        int made = Math.max(1, recipe.getAsJsonObject("result").has("count")
                ? recipe.getAsJsonObject("result").get("count").getAsInt() : 1);
        sum.merge("thaumaturge:fabrico", ASSEMBLY_FABRICO * made, Integer::sum);
        List<Aspect> out = new ArrayList<>();
        sum.forEach((id, total) -> out.add(new Aspect(id, Math.max(1, Math.min(MAX_AMOUNT, Math.round((float) total / made))))));
        return out;
    }

    // ------------------------------------------------------------------ pins

    /**
     * A section of {@code data/feedback/aspect_pins.json}: {@code {"pins": {"<item id or #tag>": [{"aspect": "thaumaturge:machina", "amount": 5}]}}}
     * (a pin per item) and {@code "food_perks"} (the same shape, keyed by food perk id).
     */
    private static Map<String, List<Aspect>> table(String section) {
        Map<String, List<Aspect>> out = new TreeMap<>();
        try (InputStream in = AspectData.class.getResourceAsStream("/data/feedback/aspect_pins.json")) {
            if (in == null)
                return out;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject(section).entrySet()) {
                List<Aspect> list = new ArrayList<>();
                for (JsonElement a : e.getValue().getAsJsonArray()) {
                    JsonObject o = a.getAsJsonObject();
                    list.add(new Aspect(o.get("aspect").getAsString(), o.has("amount") ? o.get("amount").getAsInt() : 1));
                }
                out.put(e.getKey(), list);
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("aspect_pins.json is unreadable", ex);
        }
        return out;
    }

    /**
     * {@code "groups"} in the pin file: {@code {"aspects": {"machina": 5}, "spread": 2, "items": ["gear_set", ...]}}. The group says
     * what a function is made of; each item's amount for each aspect is the group's figure moved by up to {@code spread} either
     * way, by a fixed hash of the item and aspect, so no two items share all their numbers and a regenerate changes nothing.
     * An item without a namespace is Feedback's.
     */
    private static Map<String, List<Aspect>> groups() {
        Map<String, List<Aspect>> out = new TreeMap<>();
        try (InputStream in = AspectData.class.getResourceAsStream("/data/feedback/aspect_pins.json")) {
            if (in == null)
                return out;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("groups"))
                return out;
            for (JsonElement g : root.getAsJsonArray("groups")) {
                JsonObject group = g.getAsJsonObject();
                int spread = group.has("spread") ? group.get("spread").getAsInt() : 0;
                for (JsonElement item : group.getAsJsonArray("items")) {
                    String key = item.getAsString().contains(":") ? item.getAsString() : "feedback:" + item.getAsString();
                    List<Aspect> list = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> a : group.getAsJsonObject("aspects").entrySet()) {
                        String id = "thaumaturge:" + a.getKey();
                        int offset = spread == 0 ? 0 : Math.floorMod((key + id).hashCode(), 2 * spread + 1) - spread;
                        list.add(new Aspect(id, Math.max(1, a.getValue().getAsInt() + offset)));
                    }
                    if (out.put(key, list) != null)
                        LOG.warn("aspect group item listed twice: {}", key);
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("aspect_pins.json is unreadable", ex);
        }
        return out;
    }

    // ------------------------------------------------------------------ review

    /** One sorted line per material to the log, so wrong calls show before anyone plays. Not shipped. */
    private static void review(Map<ResourceLocation, List<Aspect>> entries) {
        Map<String, String> lines = new TreeMap<>();
        FormItems.materials().forEach((matId, material) -> {
            StringBuilder names = new StringBuilder();
            aspects(material, "ingot").forEach((id, w) -> names.append(names.isEmpty() ? "" : ",").append(id).append(Math.round(w)));
            lines.put(matId.toString(), names.toString());
        });
        // What Thaumaturge will have to derive from recipes (or leave bare), so gaps are visible.
        List<String> bare = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals("feedback") && !entries.containsKey(id) && !PINNED.contains(id.toString()))
                .map(ResourceLocation::getPath).sorted().toList();
        LOG.info("Feedback items with no aspect entry ({}): {}", bare.size(), String.join(" ", bare));
        LOG.info("Thaumaturge aspect baseline: {} items, {} materials", entries.size(), lines.size());
        lines.forEach((mat, names) -> LOG.info("  {} -> {}", mat, names.isEmpty() ? "(none)" : names));
    }

    @Override
    public String getName() {
        return "Thaumaturge aspects";
    }
}
