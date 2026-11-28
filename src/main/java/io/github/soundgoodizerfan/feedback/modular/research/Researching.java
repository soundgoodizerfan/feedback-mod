// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What the Research Table can research and what a run asks of the researcher. Three kinds of thing: a treatise volume, an attunement written
 * onto a rite, and an app written onto a blank disk. A volume wants only a blank treatise and the earlier treatise of its school (and any its plan requires) in hand. Each kind plays its own game
 * (the mode's game screen) over the plan authored for it ({@link ResearchPlan}).
 */
public final class Researching {

    private Researching() {
    }

    /** The volumes that can be researched, school by school, volume by volume. */
    public static List<int[]> volumes() {
        List<int[]> out = new ArrayList<>();
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++)
                out.add(new int[] {school.ordinal(), v});
        return out;
    }

    /** The school and volume of the {@code index}-th thing in volume mode. */
    public static int[] volume(int index) {
        List<int[]> all = volumes();
        return all.get(Math.floorMod(index, all.size()));
    }

    /** Every attunement, by id: any of them can be researched into a rite, again and again, for a better one. */
    public static List<Attunements.Effect> effects() {
        List<Attunements.Effect> out = new ArrayList<>(Attunements.known().values());
        out.sort(Comparator.comparing(e -> e.id().toString()));
        return out;
    }

    /** How many things there are to pick between in {@code mode} (0 volumes, 1 attunements, 2 apps). */
    public static int count(int mode) {
        return mode == 0 ? volumes().size() : mode == 1 ? effects().size() : ComputerApps.DISK_APPS.size();
    }

    /** The app the {@code index}-th entry of app mode names. */
    public static String app(int index) {
        return ComputerApps.DISK_APPS.get(Math.floorMod(index, ComputerApps.DISK_APPS.size()));
    }

    /** The plan for the {@code index}-th thing of {@code mode}, or null when there is none loaded. */
    public static ResearchPlan plan(int mode, int index) {
        if (mode == 1) {
            var list = effects();
            return list.isEmpty() ? null : ResearchPlans.get().attunement(list.get(Math.floorMod(index, list.size())).id().getPath());
        }
        if (mode == 2)
            return ResearchPlans.get().app(app(index));
        int[] v = volume(index);
        return ResearchPlans.get().volume(School.values()[v[0]], v[1]);
    }

    /** The figure of the material property called {@code key} that {@code m} states, if it does. */
    private static OptionalDouble figure(String key, io.github.soundgoodizerfan.feedback.process.Material m) {
        return switch (key) {
            case "hardness" -> OptionalDouble.of(m.hardness().value());
            case "toughness" -> m.mechanical().toughness().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "yield" -> m.mechanical().yieldStrength().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "density" -> OptionalDouble.of(m.density().muPerMb());
            case "melting_point" -> m.thermal().meltingPoint().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "conductivity" -> m.electrical().conductivity().map(x -> OptionalDouble.of(x)).orElse(OptionalDouble.empty());
            case "specific_heat" -> m.thermal().specificHeat().map(x -> OptionalDouble.of(x.workPerMuPerTu())).orElse(OptionalDouble.empty());
            default -> OptionalDouble.empty();
        };
    }

    /**
     * How far up the property {@code key} {@code stack}'s material ranks among all the solid materials that state it, 0 to 1 (the share of them it matches
     * or beats); -1 when the stack is no material or states none. A rare, hard, hot or dense thing ranks high, so the bar an attunement sets
     * ({@code Effect.target}) is a rank, never an item.
     */
    public static float rank(String key, ItemStack stack) {
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var found = io.github.soundgoodizerfan.feedback.process.Material.find(materials, stack);
        if (found.isEmpty())
            return -1f;
        OptionalDouble mine = figure(key, found.get().material());
        if (mine.isEmpty())
            return -1f;
        double[] sorted = solidFigures(materials, key);
        if (sorted.length == 0)
            return -1f;
        // upper bound: how many figures are at or below mine
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] <= mine.getAsDouble())
                lo = mid + 1;
            else
                hi = mid;
        }
        return lo / (float) sorted.length;
    }

    /** The sorted figures of every solid material that states {@code key}, worked out once per loaded table: a rank is then one binary search, not a walk over every material. */
    private static double[] solidFigures(java.util.Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials, String key) {
        SolidFigures cache;
        synchronized (SOLIDS) {
            cache = SOLIDS.get(materials);
            if (cache == null) {
                // Server and client each hold a table in single player; a leftover from a reload is dropped past a few.
                if (SOLIDS.size() > 3)
                    SOLIDS.clear();
                cache = new SolidFigures(materials, new java.util.concurrent.ConcurrentHashMap<>());
                SOLIDS.put(materials, cache);
            }
        }
        return cache.byKey.computeIfAbsent(key, k -> {
            var out = new java.util.ArrayList<Double>();
            for (var m : materials.values()) {
                // an offering is a solid thing in hand: fluids (water, hydrogen) would crowd the top of heat capacity and leave every solid low
                if (m.kind().filter(kind -> kind == io.github.soundgoodizerfan.feedback.process.Material.Kind.FLUID).isPresent())
                    continue;
                OptionalDouble v = figure(k, m);
                if (v.isPresent())
                    out.add(v.getAsDouble());
            }
            return out.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        });
    }

    private record SolidFigures(java.util.Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> table,
                                java.util.Map<String, double[]> byKey) {}

    private static final java.util.Map<java.util.Map<ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>, SolidFigures> SOLIDS = new java.util.IdentityHashMap<>();

    /** The ink an offering earns for {@code effect}: -1 when it does not clear the bar, else 0 to {@link ModularTuning#RESEARCH_OFFER_INK} by how far over it is. */
    public static int offeringInk(Attunements.Effect effect, ItemStack offering) {
        if (offering.isEmpty() || effect.offering().isEmpty())
            return -1;
        float rank = rank(effect.offering().get(), offering);
        if (rank < 0f || rank < effect.target())
            return -1;
        return Math.round(ModularTuning.RESEARCH_OFFER_INK * (rank - effect.target()) / Math.max(0.05f, 1f - effect.target()));
    }

    /** Whether {@code stack} is a brewed potion carrying the effect's reagent effect, still potent. */
    public static boolean reagentFits(Attunements.Effect effect, ItemStack stack, net.minecraft.world.level.Level level) {
        if (effect.reagent().isEmpty() || !(stack.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem))
            return false;
        var state = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
        return state != null && state.effect().filter(effect.reagent().get()::equals).isPresent() && state.amplifierNow(level) >= 0;
    }

    /**
     * Whether an attunement can be tried: an offering that clears the bar of the property the attunement is about, or the reagent potion it
     * names. The ink is a base, the offering's (more the further it is over the bar) and a flat bonus for the reagent.
     */
    public static int attunementInk(Attunements.Effect effect, ItemStack offering, ItemStack reagent, net.minecraft.world.level.Level level) {
        int offer = offeringInk(effect, offering);
        boolean potion = reagentFits(effect, reagent, level);
        if (offer < 0 && !potion)
            return -1;
        return ModularTuning.RESEARCH_BASE_INK + Math.max(0, offer) + (potion ? ModularTuning.RESEARCH_REAGENT_INK : 0);
    }

    /**
     * The rite game's figures for a run: the effect's aspect and stage count, murk that thins and pieces that tidy the further the offering ranks over
     * the bar (an offering that is only just enough, or none, gives the thickest murk and the most awkward tray), and whether a reagent leans the tray.
     */
    public static RitePurify.Params riteParams(Attunements.Effect effect, ItemStack offering, ItemStack reagent, int ink, net.minecraft.world.level.Level level) {
        float over = 0f;
        if (!offering.isEmpty() && effect.offering().isPresent()) {
            float rank = rank(effect.offering().get(), offering);
            if (rank >= effect.target())
                over = (rank - effect.target()) / Math.max(0.05f, 1f - effect.target());
        }
        int aspect = java.util.Arrays.asList(RitePurify.ELEMENT_NAMES).indexOf(effect.aspect());
        return new RitePurify.Params(Math.max(0, aspect), effect.stages(), 1f - ModularTuning.RITE_OFFER_THIN * over, 1f - over,
                reagentFits(effect, reagent, level), ink);
    }

    /**
     * The experience points a player has now, from their level and the bar. {@code totalExperience} is only a running score that a level set by
     * command or lost to a vanilla enchant never touches, so it is not what a level is worth; spending points ({@code giveExperiencePoints})
     * does take them from the bar and then from the level below.
     */
    public static long points(Player player) {
        long level = player.experienceLevel;
        long atLevel = level <= 16 ? level * level + 6 * level : level <= 31 ? (long) (2.5 * level * level - 40.5 * level + 360) : (long) (4.5 * level * level - 162.5 * level + 2220);
        return atLevel + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Whether {@code stack} is what the app's plan says the app is about: its subject item, or any item of its subject tag. */
    public static boolean subjectFits(ResearchPlan plan, ItemStack stack) {
        if (plan == null || stack.isEmpty() || plan.subject.isEmpty())
            return false;
        if (plan.subject.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(plan.subject.substring(1));
            return tag != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(plan.subject);
        return id != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    /** Whether {@code stack} is a Floppy Disk with nothing on it: no app, no program. */
    public static boolean blankDisk(ItemStack stack) {
        return stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.FLOPPY_DISK.get())
                && !stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.APP.get())
                && !stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH.get());
    }

    /** Whether the earlier volume a volume builds on is in hand and every volume its plan {@code requires} of other schools (a first volume and an attunement need no earlier one). */
    public static boolean builtOn(Player player, net.minecraft.world.inventory.ContainerLevelAccess access, int mode, int index) {
        if (mode >= 1)
            return true;
        int[] v = volume(index);
        ResearchPlan plan = ResearchPlans.get().volume(School.values()[v[0]], v[1]);
        if (plan != null && !School.met(io.github.soundgoodizerfan.feedback.modular.treatise.Treatises.mask(player, access), String.join("+", plan.requires)))
            return false;
        return v[1] <= 1 || io.github.soundgoodizerfan.feedback.modular.treatise.Treatises.has(player, access, School.values()[v[0]], v[1] - 1);
    }

    /** The ink a treatise's or an app's run starts with: the base, and the plan's eureka bonus (what the researcher has done with the subject). It sets the hands of {@link TreatiseHand} and the tries and brackets of {@link AppHack}. */
    public static int ink(ServerPlayer player, ResearchPlan plan) {
        return ModularTuning.RESEARCH_BASE_INK + eurekaInk(player, plan);
    }

    private static int eurekaInk(ServerPlayer player, ResearchPlan plan) {
        var e = plan.eureka;
        if (e == null)
            return 0;
        ResourceLocation id = ResourceLocation.tryParse(e.item());
        var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null)
            return 0;
        int done = switch (e.kind()) {
            case "crafted" -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
            case "mined" -> player.getStats().getValue(Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Block.byItem(item)));
            default -> player.getStats().getValue(Stats.ITEM_USED.get(item));
        };
        return done >= e.threshold() ? e.ink() : 0;
    }
}
