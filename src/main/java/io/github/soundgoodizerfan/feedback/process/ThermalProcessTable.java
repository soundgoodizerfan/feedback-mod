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
            entries = List.copyOf(all);
        }
        return entries;
    }

    /**
     * An ore smelts into the metal it bears (mechanics 9.7): every item the bearer is, a melt at
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
            var metalId = best.get().getKey();
            var metal = materials.get(metalId);
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID
                    .getOptional(metalId.withPath("molten_" + metalId.getPath()));
            if (metal == null || fluid.isEmpty() || metal.thermal().meltingPoint().isEmpty()) {
                Feedback.LOGGER.warn("{} bears {} but that has no molten fluid or melting point; it will not smelt", id, metalId);
                return;
            }
            var items = material.items(id).values().stream()
                    .filter(item -> authored.stream().noneMatch(p -> p.inputs().stream().anyMatch(i -> i.test(new ItemStack(item)))))
                    .toArray(net.minecraft.world.level.ItemLike[]::new);
            if (items.length == 0)
                return;
            Tu melt = material.thermal().meltingPoint().orElse(metal.thermal().meltingPoint().get());
            out.add(new ThermalProcess(List.of(Ingredient.of(items)),
                    melt, melt, new Tu(Float.MAX_VALUE), 0f, new TuRate(Float.MAX_VALUE), ItemStack.EMPTY,
                    new Tu(Float.MAX_VALUE), ItemStack.EMPTY,
                    new net.neoforged.neoforge.fluids.FluidStack(fluid.get(), Math.round(FormType.DEFAULT_MB * best.get().getValue())),
                    false));
        });
        return out;
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
