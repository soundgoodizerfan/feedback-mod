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
 * What a food is called ({@code docs/feedback_modular_foods_spec.md} 2): a rule over its facts, the way
 * {@code GunDesignation} names a gun. Rules are {@code data/<ns>/food_name/*.json}, tried in file-id order, first match
 * wins. Nothing in play depends on a name; hunger, hazards and buffs come from the state as before.
 *
 * <p>A rule with a {@code container} names a dish (traits required of the main, base and side parts, the least cut of the
 * main); any other names a single food (its material's traits, its cut, its doneness). {@link #stamp} writes
 * {@code ITEM_NAME} and {@code CUSTOM_MODEL_DATA}, and removes them again when nothing matches.
 */
public final class FoodNames extends SimpleJsonResourceReloadListener {

    /**
     * @param name      lang key tail, {@code food.feedback.name.<name>}
     * @param traits    traits the food (single) or the part (dish) must all carry
     * @param without   traits it must carry none of
     * @param cut       least cut level (of the food; of the main part for a dish)
     * @param doneness  one of these ({@link Cooking.Doneness} names, lowercase); empty is any
     * @param model     {@code custom_model_data} for a sprite of its own, 0 for none
     * @param container a dish's container kind, which makes this a dish rule
     * @param main      traits the dish's main part carries
     * @param base      traits the dish's base part carries
     * @param side      traits a further part carries (any one)
     * @param cuisine   the cuisine the dish's parts must vote for
     * @param args      roles ({@code main}, {@code base}, {@code side}) whose material names fill the name's {@code %s} slots, in order
     * @param type      a dish rule keyed on the dish's type ({@code bread}) and not its container: the container is then any
     * @param shape     for a type rule: the shape the container part was laid in ({@code flat}); empty is any
     * @param adds      for a type rule: traits each of which some part other than the container carries (an enriching one)
     * @param lacks     for a type rule: traits no part other than the container may carry
     */
    public record Rule(String name, List<Material.Trait> traits, List<Material.Trait> without, int cut, List<String> doneness, int model,
                       Optional<String> container, List<Material.Trait> main, List<Material.Trait> base, List<Material.Trait> side,
                       Optional<Material.Trait> cuisine, List<String> args,
                       Optional<String> type, Optional<String> shape, List<Material.Trait> adds, List<Material.Trait> lacks) {
        static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Rule::name),
                Material.Trait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Rule::traits),
                Material.Trait.CODEC.listOf().optionalFieldOf("without", List.of()).forGetter(Rule::without),
                Codec.INT.optionalFieldOf("cut", 0).forGetter(Rule::cut),
                Codec.STRING.listOf().optionalFieldOf("doneness", List.of()).forGetter(Rule::doneness),
                Codec.INT.optionalFieldOf("model", 0).forGetter(Rule::model),
                Codec.STRING.optionalFieldOf("container").forGetter(Rule::container),
                Material.Trait.CODEC.listOf().optionalFieldOf("main", List.of()).forGetter(Rule::main),
                Material.Trait.CODEC.listOf().optionalFieldOf("base", List.of()).forGetter(Rule::base),
                Material.Trait.CODEC.listOf().optionalFieldOf("side", List.of()).forGetter(Rule::side),
                Material.Trait.CODEC.optionalFieldOf("cuisine").forGetter(Rule::cuisine),
                Codec.STRING.listOf().optionalFieldOf("args", List.of()).forGetter(Rule::args),
                Codec.STRING.optionalFieldOf("type").forGetter(Rule::type),
                Codec.STRING.optionalFieldOf("shape").forGetter(Rule::shape),
                Material.Trait.CODEC.listOf().optionalFieldOf("adds", List.of()).forGetter(Rule::adds),
                Material.Trait.CODEC.listOf().optionalFieldOf("lacks", List.of()).forGetter(Rule::lacks)
        ).apply(i, Rule::new));

        boolean dish() {
            return container.isPresent() || type.isPresent();
        }

        private static boolean has(List<Material.Trait> have, List<Material.Trait> need, List<Material.Trait> not) {
            return have.containsAll(need) && not.stream().noneMatch(have::contains);
        }
    }

    private static final FoodNames INSTANCE = new FoodNames();

    private List<Rule> rules = List.of();

    private FoodNames() {
        super(new com.google.gson.GsonBuilder().create(), "food_name");
    }

    public static FoodNames get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Rule> loaded = new TreeMap<>();
        json.forEach((id, element) -> Rule.CODEC.parse(JsonOps.INSTANCE, element)
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
                : named.rule().dish() && state.cook() < 1f ? "unbaked" : named.ragged() ? "chunky"
                : named.knead() == FoodKnead.GLOSSY ? "glossy" : named.knead() == FoodKnead.CRUMBLY ? "crumbly" : named.knead() == FoodKnead.TOUGH ? "tough" : named.grade();
        return word == null ? named.name() : Component.translatable("food.feedback.prefix." + word, named.name());
    }

    /** A rule that held, with the name it gives (its lang key filled from the dish's parts); {@code knead} is the container dough's grade, -1 for none; {@code grade} the word a clean hand-work class earns (hand_cut, silky) or null. */
    private record Named(Rule rule, Component name, boolean ragged, int knead, String grade) {
    }

    private record Piece(List<Material.Trait> traits, Component name, int cut, boolean ragged) {
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
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
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
            if (p.dims().keySet().stream().anyMatch(k -> k.startsWith(DishBake.ING))) {
                traits = new ArrayList<>(traits);
                String top = "";
                float topShare = 0f;
                for (Map.Entry<String, Float> e : p.dims().entrySet()) {
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
                if (m != null && p.dim("pour", -1f) >= 0f && PrepWork.mode(m) == PrepWork.POUR && Math.abs(p.dim("pour", 0f) - 1f) <= FTuning.POUR_CLEAN)
                    silky = true;
                continue;
            }
            if (role.name().equals("container")) {
                container = p.kind().isEmpty() ? p.material().getPath() : p.kind();
                shapeLaid = p.variant();
                kneadGrade = Math.round(p.dim(DishBake.KNEAD, -1f));
                continue;
            }
            roles.computeIfAbsent(role.name(), k -> new ArrayList<>()).add(new Piece(traits,
                    pieceName, Math.round(p.dim("cut", 0f)),
                    p.dim("cut", 0f) > 0f && Math.round(p.dim("evenness", 0f)) == FoodCut.RAGGED));
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
        for (Rule r : INSTANCE.rules) {
            if (!r.dish() || r.container().filter(c -> !c.equals(boxed)).isPresent() || r.type().filter(t -> !t.equals(typeId)).isPresent()
                    || r.shape().filter(sh -> !sh.equals(shape)).isPresent() || cut < r.cut())
                continue;
            if (!r.adds().stream().allMatch(t -> extras.stream().anyMatch(pc -> pc.traits().contains(t)))
                    || r.lacks().stream().anyMatch(t -> extras.stream().anyMatch(pc -> pc.traits().contains(t))))
                continue;
            if (r.cuisine().isPresent() && !top.contains(r.cuisine().get()))
                continue;
            Map<String, List<Material.Trait>> need = Map.of("main", r.main(), "base", r.base(), "side", r.side());
            Map<String, Component> filled = new java.util.HashMap<>();
            boolean ok = true;
            for (Map.Entry<String, List<Material.Trait>> n : need.entrySet()) {
                List<Piece> have = roles.getOrDefault(n.getKey(), List.of());
                if (n.getValue().isEmpty() && !r.args().contains(n.getKey()))
                    continue;
                Piece hit = have.stream().filter(pc -> Rule.has(pc.traits(), n.getValue(), r.without())).findFirst().orElse(null);
                if (hit == null) {
                    ok = false;
                    break;
                }
                filled.put(n.getKey(), hit.name());
            }
            if (ok)
                return Optional.of(new Named(r, Component.translatable("food.feedback.name." + r.name(),
                        r.args().stream().map(filled::get).toArray()), ragged, knead,
                        silky ? "silky" : cut > 0 && !ragged ? "hand_cut" : null));
        }
        return Optional.empty();
    }

    private static Optional<Rule> single(ItemStack stack, Cooking.Entry entry, FoodState state) {
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        int level = cut == null ? 0 : cut.level();
        String doneness = Cooking.doneness(entry, state).name().toLowerCase(java.util.Locale.ROOT);
        for (Rule r : INSTANCE.rules)
            if (!r.dish() && level >= r.cut() && (r.doneness().isEmpty() || r.doneness().contains(doneness))
                    && Rule.has(entry.traits(), r.traits(), r.without()))
                return Optional.of(r);
        return Optional.empty();
    }
}
