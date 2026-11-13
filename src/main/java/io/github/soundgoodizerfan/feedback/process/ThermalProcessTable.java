// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Loads every {@link ThermalProcess} from {@code data/<namespace>/thermal_process/*.json}.
 *
 * <h2>A second table, not a second recipe system</h2>
 * Deliberately shaped exactly like {@link DeformationTable} and deliberately separate from it.
 * Separate because they answer different questions about the same material -- what it does under
 * a hammer, and what it does in a fire -- and folding them into one "process" type would mean a
 * single record carrying every field either could want, most of them absent. Shaped the same
 * because the next one, whatever it is, should cost an afternoon.
 */
public class ThermalProcessTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "thermal_process";

    private static final ThermalProcessTable INSTANCE = new ThermalProcessTable();

    private List<ThermalProcess> authored = List.of();
    private List<PhaseChange> phases = List.of();
    private List<ThermalProcess> phaseRows = List.of();
    private List<ThermalProcess> quenchRows = List.of();
    private List<ThermalProcess> dipRows = List.of();
    private List<Dip> dips = List.of();
    private List<ThermalProcess> electrolysisRows = List.of();
    private List<Electrolysis> electrolyses = List.of();
    private List<ThermalProcess> entries = List.of();
    private boolean dirty = true;

    private ThermalProcessTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static ThermalProcessTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        // Most inputs first, then by id: find takes the first match, and contents satisfying a
        // two-input process (iron + coal) also satisfy any one-input process among them, so the
        // more specific must be tried first -- and the order must not depend on HashMap iteration.
        Map<ResourceLocation, ThermalProcess> loaded = new TreeMap<>();
        json.forEach((id, element) -> ThermalProcess.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad thermal process {}: {}", id, error))
                .ifPresent(process -> loaded.put(id, process)));
        List<PhaseChange> boils = new ArrayList<>();
        List<ThermalProcess> boilRows = new ArrayList<>();
        loaded.values().removeIf(row -> {
            row.phase().ifPresent(p -> {
                boils.add(PhaseChange.of(row, p));
                boilRows.add(row);
            });
            return row.phase().isPresent();
        });
        List<ThermalProcess> quenches = new ArrayList<>();
        loaded.values().removeIf(row -> {
            if (row.quench())
                quenches.add(row);
            return row.quench();
        });
        quenchRows = List.copyOf(quenches);
        List<ThermalProcess> dipping = new ArrayList<>();
        loaded.values().removeIf(row -> {
            if (row.dip().isPresent())
                dipping.add(row);
            return row.dip().isPresent();
        });
        dipRows = List.copyOf(dipping);
        dips = dipping.stream().map(r -> r.dip().get()).toList();
        List<ThermalProcess> splitting = new ArrayList<>();
        loaded.values().removeIf(row -> {
            if (row.electrolysis().isPresent())
                splitting.add(row);
            return row.electrolysis().isPresent();
        });
        electrolysisRows = List.copyOf(splitting);
        electrolyses = splitting.stream().map(r -> r.electrolysis().get()).toList();
        phases = List.copyOf(boils);
        phaseRows = List.copyOf(boilRows);
        List<ThermalProcess> sorted = new ArrayList<>(loaded.values());
        sorted.sort(Comparator.comparingInt((ThermalProcess p) -> p.inputs().size()).reversed());
        authored = List.copyOf(sorted);
        dirty = true;
        Feedback.LOGGER.info("Loaded {} thermal process entries", authored.size());
    }

    void invalidate() {
        dirty = true;
    }

    /** The authored processes, then a melt for every ore (a material that {@code bears} a metal) that none of them already covers. */
    public List<ThermalProcess> entries() {
        if (dirty) {
            dirty = false;
            List<ThermalProcess> all = new ArrayList<>(authored);
            all.addAll(bearerMelts(authored));
            all.addAll(metalMelts(authored));
            entries = List.copyOf(all);
        }
        return entries;
    }

    /**
     * An ore smelts into the metal it bears: every item the bearer is, a melt at
     * its own melting point if its file gives one, else the metal's, yielding the metal's molten
     * fluid by the largest share of {@code bears}. A bearer whose metal has no molten fluid yet
     * melts into nothing, and is logged once.
     */
    private static List<ThermalProcess> bearerMelts(List<ThermalProcess> authored) {
        List<ThermalProcess> out = new ArrayList<>();
        var materials = MaterialTable.get().materials();
        materials.forEach((id, material) -> {
            var best = material.bears().entrySet().stream().max(java.util.Map.Entry.comparingByValue());
            if (best.isEmpty())
                return;
            // A native-metal ore (it bears itself) keeps its authored melts for the raw rock; only the ladder's forms are derived.
            boolean selfBearing = best.get().getKey().equals(id);
            var metalId = best.get().getKey();
            var metal = materials.get(metalId);
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID
                    .getOptional(metalId.withPath("molten_" + metalId.getPath()));
            if (metal == null || fluid.isEmpty() || metal.thermal().meltingPoint().isEmpty()) {
                Feedback.LOGGER.warn("{} bears {} but that has no molten fluid or melting point; it will not smelt", id, metalId);
                return;
            }
            // One melt per rung of the ore ladder (OreLadder.recovery): a crushed or separated form pays more metal than the raw
            // rock, tailings less. Every form of a rung is one form-agnostic row, so a finer form melts sooner; a form some
            // authored row already takes keeps to that row.
            java.util.Map<Float, List<String>> rungs = new java.util.TreeMap<>();
            material.items(id).forEach((form, item) -> {
                if (selfBearing && !OreLadder.LADDER_FORMS.contains(form) || !isRock(form))
                    return;
                // Only a row that takes the item on its own covers it: a bloom needs charcoal beside the ore, and the bare ore still melts.
                if (authored.stream().noneMatch(p -> p.inputs().size() == 1 && p.inputs().get(0).test(new ItemStack(item))))
                    rungs.computeIfAbsent(OreLadder.recovery(form), k -> new ArrayList<>()).add(form);
            });
            Tu melt = material.thermal().meltingPoint().orElse(metal.thermal().meltingPoint().get());
            rungs.forEach((recovery, forms) -> out.add(new ThermalProcess(List.of(
                    new MaterialIngredient(Optional.empty(), Optional.empty(), Optional.empty(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                            Optional.of(id), forms.size() == material.items(id).size() ? List.of() : List.copyOf(forms)).toVanilla()),
                    melt, melt, new Tu(Float.MAX_VALUE), ProcessTuning.MELT_HOLD_TPU, new TuRate(Float.MAX_VALUE), id.getPath().equals("silicate_slag") ? ItemStack.EMPTY : OreLadder.slag(recovery, material.items(id)),
                    new Tu(Float.MAX_VALUE), ItemStack.EMPTY,
                    new net.neoforged.neoforge.fluids.FluidStack(fluid.get(),
                            Math.round(FormType.DEFAULT_MB * best.get().getValue() * OreLadder.ORE_CONTENT * recovery)),
                    false)));
        });
        return out;
    }

    /**
     * Whether a form of a bearer is the rock (as dug, crushed, ground or separated) and not stock made from the metal: a native metal
     * that bears another (iron bears pig iron) keeps its own ingots and plates out of the bearer melt.
     */
    private static boolean isRock(String form) {
        var type = io.github.soundgoodizerfan.feedback.registry.FormItems.types().get(form);
        return form.startsWith("raw") || form.endsWith("ore") || form.equals("dust") || OreLadder.LADDER_FORMS.contains(form)
                || type != null && type.bearing();
    }

    /**
     * A metal with a {@code molten_<id>} fluid melts from its ingot, scrap and swarf at its own melting point into
     * one ingot's worth, unless an authored process already takes that ingot. This is what lets a
     * metal be added without a melt file.
     */
    private static List<ThermalProcess> metalMelts(List<ThermalProcess> authored) {
        List<ThermalProcess> out = new ArrayList<>();
        MaterialTable.get().materials().forEach((id, material) -> {
            var melt = material.thermal().meltingPoint();
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID
                    .getOptional(id.withPath("molten_" + id.getPath()));
            if (melt.isEmpty() || fluid.isEmpty())
                return;
            // Scrap and swarf are the metal again, in the volume their form type names.
            for (String form : List.of("ingot", "scrap", "swarf", "dust")) {
                var item = material.items(id).get(form);
                if (item == null
                        || authored.stream().anyMatch(p -> p.inputs().size() == 1 && p.inputs().get(0).test(new ItemStack(item))))
                    continue;
                out.add(new ThermalProcess(List.of(Ingredient.of(item)),
                        melt.get(), melt.get(), new Tu(Float.MAX_VALUE), meltHold(form), new TuRate(Float.MAX_VALUE), ItemStack.EMPTY,
                        new Tu(Float.MAX_VALUE), ItemStack.EMPTY,
                        new net.neoforged.neoforge.fluids.FluidStack(fluid.get(), melted(form)),
                        false));
            }
            // A whole made part melts back too: the vessel takes the volume the part holds (ThermalVesselCore) less PART_REMELT_LOSS.
            List<String> partForms = material.items(id).keySet().stream()
                    .filter(f -> io.github.soundgoodizerfan.feedback.registry.FormItems.types().get(f) != null
                            && io.github.soundgoodizerfan.feedback.registry.FormItems.types().get(f).part()).toList();
            if (!partForms.isEmpty())
                out.add(new ThermalProcess(List.of(
                        new MaterialIngredient(Optional.empty(), Optional.empty(), Optional.empty(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                                Optional.of(id), partForms).toVanilla()),
                        melt.get(), melt.get(), new Tu(Float.MAX_VALUE), meltHold("ingot"), new TuRate(Float.MAX_VALUE), ItemStack.EMPTY,
                        new Tu(Float.MAX_VALUE), ItemStack.EMPTY,
                        new net.neoforged.neoforge.fluids.FluidStack(fluid.get(), Math.round(FormType.DEFAULT_MB
                                * (1f - io.github.soundgoodizerfan.feedback.modular.ModularTuning.PART_REMELT_LOSS))),
                        false));
        });
        return out;
    }

    /** The mB one item of {@code form} pours when melted: its volume, less the share swarf loses to oxide. */
    private static int melted(String form) {
        int mb = io.github.soundgoodizerfan.feedback.registry.FormItems.volume(form);
        return form.equals("swarf") ? Math.round(mb * (1f - io.github.soundgoodizerfan.feedback.modular.ModularTuning.SWARF_REMELT_LOSS)) : mb;
    }

    /**
     * The hold of a derived melt of one piece of {@code form}: {@link ProcessTuning#MELT_HOLD_TPU} for an ingot's volume, in proportion to the
     * piece's volume, shortened by the form's grain exactly as any form-agnostic row is ({@link FormType#holdFactor}): a block
     * takes nine ingots' time, a dust a fraction of one. At least one TPu, so a melt always takes a tick in the band.
     */
    static float meltHold(String form) {
        var type = io.github.soundgoodizerfan.feedback.registry.FormItems.types().get(form);
        float grain = type == null ? 1f : type.grain();
        float volume = io.github.soundgoodizerfan.feedback.registry.FormItems.volume(form) / (float) FormType.DEFAULT_MB;
        return Math.max(1f, ProcessTuning.MELT_HOLD_TPU * volume * FormType.holdFactor(grain));
    }

    /** What the client's recipe browser is sent: the rows of {@link #entries()} and the phase rows. */
    public List<ThermalProcess> syncEntries() {
        List<ThermalProcess> all = new ArrayList<>(entries());
        all.addAll(phaseRows);
        all.addAll(quenchRows);
        all.addAll(dipRows);
        all.addAll(electrolysisRows);
        CastingTable.get().entries().forEach(c -> all.add(ThermalProcess.freezing(c)));
        return all;
    }

    /**
     * The one-input row {@code stack} runs on its own in a vessel at {@code temperature}: an authored row if any takes it, else the cooking
     * recipes of the recipe manager as rows ({@link VanillaFallback#rowsFor}). Of several, the one this heat suits best, the shorter hold on a tie.
     */
    public Optional<ThermalProcess> findSingle(net.minecraft.world.level.Level level, ItemStack stack, Tu temperature, boolean airless) {
        if (stack.isEmpty())
            return Optional.empty();
        ThermalProcess best = null;
        float bestSuit = -1f;
        for (ThermalProcess row : entries())
            if (row.inputs().size() == 1 && !row.isReaction() && (airless || !row.airless()) && row.inputs().get(0).test(stack)) {
                float suit = row.suitability(temperature);
                if (best == null || suit > bestSuit) {
                    best = row;
                    bestSuit = suit;
                }
            }
        if (best != null)
            return Optional.of(best);
        for (ThermalProcess row : VanillaFallback.rowsFor(level, stack)) {
            float suit = row.suitability(temperature);
            if (best == null || suit > bestSuit || suit == bestSuit && row.requiredTpu() < best.requiredTpu()) {
                best = row;
                bestSuit = suit;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Every electrolysis: current through a liquid. */
    public List<Electrolysis> electrolyses() {
        return electrolyses;
    }

    /** The row that splits {@code liquid}, if any. */
    public Optional<Electrolysis> electrolysisOf(net.neoforged.neoforge.fluids.FluidStack liquid) {
        return liquid.isEmpty() ? Optional.empty() : electrolyses.stream().filter(e -> e.splits(liquid)).findFirst();
    }

    /** Every dip: an item meeting a bath. */
    public List<Dip> dips() {
        return dips;
    }

    /** The imbue entry that lets {@code held} soak in a potion, if any. */
    public Optional<Dip> findImbue(ItemStack held) {
        return held.isEmpty() ? Optional.empty() : dips.stream().filter(d -> d.imbue().isPresent() && d.input().test(held)).findFirst();
    }

    /** The dip {@code held} takes in a bath of {@code fluid} at {@code vesselTemperature}, if any. */
    public Optional<Dip> findDip(net.neoforged.neoforge.fluids.FluidStack fluid, ItemStack held, Tu vesselTemperature) {
        if (fluid.isEmpty() || held.isEmpty())
            return Optional.empty();
        return dips.stream().filter(entry -> entry.matches(fluid, held, vesselTemperature)).findFirst();
    }

    /** The quench an item at its current heat takes in water, if any: it must be at or above the row's {@code min_temperature}. */
    public Optional<ThermalProcess> findQuench(ItemStack stack) {
        return findQuench(stack, null);
    }

    /** The quench {@code stack} takes in {@code medium} (a fluid id; null is water). */
    public Optional<ThermalProcess> findQuench(ItemStack stack, net.minecraft.resources.ResourceLocation medium) {
        for (ThermalProcess row : quenchRows)
            if (java.util.Objects.equals(row.medium().orElse(null), medium) && row.inputs().stream().anyMatch(i -> i.test(stack)))
                return Optional.of(row);
        return Optional.empty();
    }

    /** Every phase row: a liquid boiling to its gas at the row's boiling point, the gas condensing back. */
    public List<PhaseChange> phases() {
        return phases;
    }

    /** What {@code liquid} boils into, if anything. */
    public Optional<PhaseChange> boiling(net.neoforged.neoforge.fluids.FluidStack liquid) {
        return liquid.isEmpty() ? Optional.empty() : phases.stream().filter(e -> e.boils(liquid)).findFirst();
    }

    /** What {@code gas} condenses into, if anything. */
    public Optional<PhaseChange> condensing(net.neoforged.neoforge.fluids.FluidStack gas) {
        return gas.isEmpty() ? Optional.empty() : phases.stream().filter(e -> e.condenses(gas)).findFirst();
    }

    /** Every reaction row (spec 7): the ones a reactor reads and a crucible never does. */
    public List<ThermalProcess> reactions() {
        return entries().stream().filter(ThermalProcess::isReaction).toList();
    }

    /** The process these contents satisfy, if any. */
    public Optional<ThermalProcess> find(List<ItemStack> contents, boolean airless) {
        for (ThermalProcess entry : entries())
            if (!entry.isReaction() && (airless || !entry.airless()) && match(entry.inputs(), contents) != null)
                return Optional.of(entry);
        return Optional.empty();
    }

    /**
     * Which slot satisfies which input, or null if the contents do not satisfy it.
     *
     * <h3>Why this backtracks rather than matching greedily</h3>
     * Greedy is correct for the slice -- iron and charcoal share no tag -- and wrong the first
     * time two inputs overlap, which is exactly the sort of thing a datapack author does without
     * warning. A process wanting "any ingot" and "any copper ingot" fed a copper ingot and an
     * iron one is solvable, and greedy solves it only if it happens to try them in the right
     * order. The search is over two or three items; correctness is free here and the bug would
     * not be.
     *
     * @return slot index per input, in the inputs' own order.
     */
    public static int[] match(List<Ingredient> inputs, List<ItemStack> contents) {
        int[] assignment = new int[inputs.size()];
        boolean[] taken = new boolean[contents.size()];
        return assign(inputs, contents, 0, assignment, taken) ? assignment : null;
    }

    private static boolean assign(List<Ingredient> inputs, List<ItemStack> contents, int index,
                                  int[] assignment, boolean[] taken) {
        if (index == inputs.size())
            return true;
        for (int slot = 0; slot < contents.size(); slot++) {
            if (taken[slot] || contents.get(slot).isEmpty() || !inputs.get(index).test(contents.get(slot)))
                continue;
            taken[slot] = true;
            assignment[index] = slot;
            if (assign(inputs, contents, index + 1, assignment, taken))
                return true;
            taken[slot] = false;
        }
        return false;
    }
}
