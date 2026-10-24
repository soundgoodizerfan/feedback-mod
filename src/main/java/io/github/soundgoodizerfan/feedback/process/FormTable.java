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
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Loads every {@link FormStep} from {@code data/<namespace>/form/*.json} and compiles them, with
 * {@link MaterialTable}, into the {@link Deformation}s and {@link Removal}s the machines read.
 *
 * <h2>Compiled lazily, because reload order is not something to lean on</h2>
 * Steps and materials are separate listeners and either may apply first. Each marks this dirty
 * and the first reader after both have landed does the compile, so neither has to know the other
 * exists. {@link DeformationTable} and {@link RemovalTable} are views over the result; nothing
 * downstream of them (machines, sync, JEI) knows recipes are no longer authored.
 *
 * <h2>Where the removal swarf comes from</h2>
 * A removal entry throws off the material's {@code swarf} form if it has one. A step does not
 * name it: what a cut sheds is a property of what was cut.
 */
public class FormTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "form";
    private static final String SWARF = "swarf";

    private static final FormTable INSTANCE = new FormTable();

    private Map<ResourceLocation, FormStep> steps = Map.of();
    private List<Deformation> deformations = List.of();
    private List<Removal> removals = List.of();
    private boolean dirty = true;

    private FormTable() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static FormTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, FormStep> loaded = new TreeMap<>();
        json.forEach((id, element) -> FormStep.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad form step {}: {}", id, error))
                .ifPresent(step -> loaded.put(id, step)));
        steps = Map.copyOf(loaded);
        invalidate();
    }

    void invalidate() {
        dirty = true;
    }

    public List<Deformation> deformations() {
        compile();
        return deformations;
    }

    public List<Removal> removals() {
        compile();
        return removals;
    }

    private void compile() {
        if (!dirty)
            return;
        dirty = false;
        List<Deformation> deformed = new ArrayList<>();
        List<Removal> removed = new ArrayList<>();
        Map<ResourceLocation, FormStep> ordered = new TreeMap<>(steps);
        MaterialTable.get().materials().forEach((materialId, material) -> ordered.forEach((stepId, step) -> {
            Map<String, Item> items = material.items(materialId);
            Ingredient from = material.ingredient(materialId, step.from()).orElse(null);
            ItemStack to = material.stack(materialId, step.to()).orElse(null);
            if (from == null || to == null)
                return;
            int work = step.work(material.hardness().value());
            if (step.operation().removesMaterial()) {
                Item swarf = items.get(SWARF);
                removed.add(new Removal(from, work, material.hardness(), to,
                        step.operation(), swarf == null ? ItemStack.EMPTY : new ItemStack(swarf), step.finer()));
            } else {
                deformed.add(new Deformation(from, work, material.hardness(), to,
                        material.minTemperature(), material.maxTemperature(), step.operation()));
            }
        }));
        deformations = List.copyOf(deformed);
        removals = List.copyOf(removed);
        Feedback.LOGGER.info("Compiled {} deformation and {} removal entries from {} steps",
                deformations.size(), removals.size(), steps.size());
    }
}
