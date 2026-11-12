// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.DishBake;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularType.Designation;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.PrepWork;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * What a food is called: a rule over its facts, the way
 * {@code GunDesignation} names a gun. Rules are {@code data/<ns>/food_name/*.json}, tried in file-id order, first match
 * wins. Nothing in play depends on a name; hunger, hazards and buffs come from the state as before.
 *
 * <p>A rule with a {@code container} names a dish (traits required of the main, base and side parts, the least cut of the
 * main); any other names a single food (its material's traits, its cut, its doneness). {@link #stamp} writes
 * {@code ITEM_NAME} and {@code CUSTOM_MODEL_DATA}, and removes them again when nothing matches.
 */
public final class FoodNames extends SimpleJsonResourceReloadListener {

    /** A dish rule names a container or a dish type; any other names a single food. */
    private static boolean dish(Designation r) {
        return r.is().containsKey("container") || r.is().containsKey("type");
    }

    private static List<String> names(List<Material.Trait> traits) {
        return traits.stream().map(Material.Trait::getSerializedName).toList();
    }

    private static final FoodNames INSTANCE = new FoodNames();

    private List<Designation> rules = List.of();

    private FoodNames() {
        super(new com.google.gson.GsonBuilder().create(), "food_name");
    }

    public static FoodNames get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Designation> loaded = new TreeMap<>();
        json.forEach((id, element) -> Designation.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad food name {}: {}", id, error))
                .ifPresent(r -> loaded.put(id, r)));
        rules = List.copyOf(loaded.values());
        Feedback.LOGGER.info("Loaded {} food names", rules.size());
    }

    /** Name and sprite this stack from its facts, or restore the vanilla ones when no rule holds. Server side. */
    public static void stamp(ItemStack stack, Cooking.Entry entry, FoodState state) {
        Optional<Named> named = stack.has(FDataComponents.DISH_FIGURES.get()) ? dish(stack)
                : single(stack, entry, state).map(r -> new Named(r, Component.translatable("food.feedback.name." + r.name()), cutOf(stack).ragged(), -1,
                        cutOf(stack).level() > 0 && cutOf(stack).grade() == FoodCut.EVEN ? "hand_cut" : null));
        if (named.isPresent())
            stack.set(DataComponents.ITEM_NAME, prefixed(named.get(), state));
        else
            stack.remove(DataComponents.ITEM_NAME);
        if (named.isPresent() && named.get().rule().model() > 0)
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(named.get().rule().model()));
        else
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    private static FoodCut cutOf(ItemStack stack) {
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        return cut == null ? new FoodCut(0, FoodCut.EVEN) : cut;
    }

    /** The name with the one word its state earns: spoiled, stale, burnt, unbaked (a dish not yet baked), chunky the dough's knead (glossy, crumbly, tough), a clean pour (silky) or an even cut (hand-cut), in that order of say. */
    private static Component prefixed(Named named, FoodState state) {
        int spoilage = Cooking.spoilage(state);
        String word = spoilage == 2 ? "spoiled" : spoilage == 1 ? "stale" : state.charred() >= 1f ? "burnt"
                : dish(named.rule()) && state.cook() < 1f ? "unbaked" : named.ragged() ? "chunky"
                : named.knead() == FoodKnead.GLOSSY ? "glossy" : named.knead() == FoodKnead.CRUMBLY ? "crumbly" : named.knead() == FoodKnead.TOUGH ? "tough" : named.grade();
        return word == null ? named.name() : Component.translatable("food.feedback.prefix." + word, named.name());
    }

    /** A rule that held, with the name it gives (its lang key filled from the dish's parts); {@code knead} is the container dough's grade, -1 for none; {@code grade} the word a clean hand-work class earns (hand_cut, silky) or null. */
    private record Named(Designation rule, Component name, boolean ragged, int knead, String grade) {
    }

    private record Piece(List<Material.Trait> traits, Component name, int cut, boolean ragged, String form) {
    }

    /** Least share of a pot an ingredient needs to count toward its serving's name. */
    private static final float ING_SHARE = 0.15f;

    private static final List<Material.Trait> CUISINES = List.of(Material.Trait.ITALIAN, Material.Trait.EAST_ASIAN, Material.Trait.MEXICAN, Material.Trait.EASTERN_EUROPEAN);

    /**
     * The dish rule that holds of this dish, if any. Parts vote for a cuisine by the cuisine traits their materials carry
     * (a tie lets every leader's rules try, in file order); a rule's {@code args} are the roles whose material names fill its pattern.
     */
    private static Optional<Named> dish(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return Optional.empty();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        String container = "", shapeLaid = "";
        int kneadGrade = -1;
        boolean silky = false;
        Map<String, List<Piece>> roles = new java.util.HashMap<>();
        Map<Material.Trait, Integer> votes = new java.util.EnumMap<>(Material.Trait.class);
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null)
                continue;
            List<Material.Trait> traits = m == null ? List.of() : m.traits();
            Component pieceName = Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath());
            // A serving carries its pot's ingredient shares: the big ones lend their traits and cuisine, the biggest names it.
            if (p.measures().state().keySet().stream().anyMatch(k -> k.startsWith(DishBake.ING))) {
                traits = new ArrayList<>(traits);
                String top = "";
                float topShare = 0f;
                for (Map.Entry<String, Float> e : p.measures().state().entrySet()) {
                    if (!e.getKey().startsWith(DishBake.ING))
                        continue;
                    String path = e.getKey().substring(DishBake.ING.length());
                    Material im = materials.get(ResourceLocation.fromNamespaceAndPath("feedback", path));
                    if (e.getValue() > topShare) {
                        topShare = e.getValue();
                        top = path;
                    }
                    if (im == null || e.getValue() < ING_SHARE)
                        continue;
                    traits.addAll(im.traits());
                    for (Material.Trait t : CUISINES)
                        if (im.traits().contains(t))
                            votes.merge(t, 1, Integer::sum);
                }
                if (!top.isEmpty())
                    pieceName = Component.translatable("material.feedback." + top);
            }
            for (Material.Trait t : CUISINES)
                if (m != null && m.traits().contains(t))
                    votes.merge(t, 1, Integer::sum);
            if (role.after()) {
                // A runny finish poured to the line is silky (a shaken or placed one has no pour to judge).
                if (m != null && p.state("pour", -1f) >= 0f && PrepWork.mode(m) == PrepWork.POUR && Math.abs(p.state("pour", 0f) - 1f) <= FTuning.POUR_CLEAN)
                    silky = true;
                continue;
            }
            if (role.name().equals("container")) {
                container = p.kind().isEmpty() ? p.material().getPath() : p.kind();
                shapeLaid = p.variant();
                kneadGrade = Math.round(p.state(DishBake.KNEAD, -1f));
                continue;
            }
            roles.computeIfAbsent(role.name(), k -> new ArrayList<>()).add(new Piece(traits,
                    pieceName, Math.round(p.state("cut", 0f)),
                    p.state("cut", 0f) > 0f && Math.round(p.state("evenness", 0f)) == FoodCut.RAGGED, p.form()));
        }
        int best = votes.values().stream().max(Integer::compare).orElse(0);
        java.util.Set<Material.Trait> top = new java.util.HashSet<>();
        votes.forEach((t, n) -> {
            if (n == best)
                top.add(t);
        });
        boolean ragged = roles.values().stream().flatMap(List::stream).anyMatch(Piece::ragged);
        int cut = roles.values().stream().flatMap(List::stream).mapToInt(Piece::cut).max().orElse(0);
        final String boxed = container, shape = shapeLaid, typeId = design.type().getPath();
        final int knead = kneadGrade;
        List<Piece> extras = roles.values().stream().flatMap(List::stream).toList();
        Map<String, String> facts = new java.util.HashMap<>(Map.of("container", boxed, "type", typeId, "shape", shape));
        for (Designation r : INSTANCE.rules) {
            if (!dish(r) || cut < r.cut())
                continue;
            if (!r.adds().stream().allMatch(t -> extras.stream().anyMatch(pc -> names(pc.traits()).contains(t)))
                    || r.lacks().stream().anyMatch(t -> extras.stream().anyMatch(pc -> names(pc.traits()).contains(t))))
                continue;
            if (r.cuisine().isPresent() && top.stream().noneMatch(t -> t.getSerializedName().equals(r.cuisine().get())))
                continue;
            facts.remove("main_form");
            Map<String, Component> filled = new java.util.HashMap<>();
            boolean ok = true;
            for (String role : List.of("main", "base", "side")) {
                List<String> need = r.roles().getOrDefault(role, List.of());
                if (need.isEmpty() && !r.args().contains(role))
                    continue;
                Piece hit = roles.getOrDefault(role, List.of()).stream()
                        .filter(pc -> names(pc.traits()).containsAll(need) && r.without().stream().noneMatch(names(pc.traits())::contains)
                        ).findFirst().orElse(null);
                if (hit == null) {
                    ok = false;
                    break;
                }
                filled.put(role, hit.name());
                // The main part's form is a fact the rule may test (main_form).
                if (role.equals("main"))
                    facts.put("main_form", hit.form());
            }
            if (ok && r.isHolds(facts))
                return Optional.of(new Named(r, Component.translatable("food.feedback.name." + r.name(),
                        r.args().stream().map(filled::get).toArray()), ragged, knead,
                        silky ? "silky" : cut > 0 && !ragged ? "hand_cut" : null));
        }
        return Optional.empty();
    }

    private static Optional<Designation> single(ItemStack stack, Cooking.Entry entry, FoodState state) {
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        int level = cut == null ? 0 : cut.level();
        // Salt and smoke past half a coat are facts a rule may test (biltong, cured meat, pickles).
        Map<String, String> facts = new java.util.HashMap<>(Map.of("doneness", Cooking.doneness(entry, state).name().toLowerCase(java.util.Locale.ROOT)));
        if (state.salt() >= 0.5f)
            facts.put("salted", "yes");
        if (state.smoke() >= 0.5f)
            facts.put("smoked", "yes");
        List<String> have = names(entry.traits());
        for (Designation r : INSTANCE.rules)
            if (!dish(r) && level >= r.cut() && r.holds(facts, have))
                return Optional.of(r);
        return Optional.empty();
    }
}
