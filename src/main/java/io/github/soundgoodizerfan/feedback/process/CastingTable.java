// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a mold makes of a cooling melt: no casting is authored. A mold item names a shape ({@code data/<namespace>/mold/<shape>.json}:
 * the form it makes and how many a fill gives), and every metal or glass with a molten fluid freezes into that form of itself
 * ({@link Material#items}, or the shared part item carrying the material for a tool head), at the material's own melting point.
 * {@link io.github.soundgoodizerfan.feedback.item.MoldItem} and the basin read {@link #find}; the client's copy
 * ({@code net/CastingSyncPayload}) is only for the recipe browser.
 */
public class CastingTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "mold";

    /**
     * @param quenched the pour sets only once it is cold ({@link io.github.soundgoodizerfan.feedback.core.FTuning#WARM_TU}), never at the
     *                 melt's own freezing point: a spray shattered by water, which is how a melt becomes dust (atomising). Left to cool slowly
     *                 it is simply a long wait; douse it in the basin and it is dust at once.
     */
    private record Shape(String form, int count, boolean quenched) {
        static final com.mojang.serialization.Codec<Shape> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.STRING.fieldOf("form").forGetter(Shape::form),
                com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Shape::count),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("quenched", false).forGetter(Shape::quenched)
        ).apply(i, Shape::new));
    }

    private static final CastingTable INSTANCE = new CastingTable();

    private Map<String, Shape> shapes = Map.of();
    private List<Casting> entries = List.of();
    private boolean dirty = true;

    private CastingTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static CastingTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<String, Shape> loaded = new java.util.TreeMap<>();
        json.forEach((id, element) -> Shape.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad mold shape {}: {}", id, error))
                .ifPresent(shape -> loaded.put(id.getPath(), shape)));
        shapes = Map.copyOf(loaded);
        dirty = true;
        Feedback.LOGGER.info("Loaded {} mold shapes", shapes.size());
    }

    void invalidate() {
        dirty = true;
    }

    /** Every melt freezing in every shape it has a form for: the recipe browser's list, and what {@link #find} searches. */
    public List<Casting> entries() {
        if (dirty) {
            dirty = false;
            List<Casting> all = new ArrayList<>();
            MaterialTable.get().materials().forEach((id, material) -> {
                var melt = material.thermal().meltingPoint();
                var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID
                        .getOptional(id.withPath("molten_" + id.getPath()))
                        // polymers: soul_resin -> soul_resin_melt
                        .or(() -> net.minecraft.core.registries.BuiltInRegistries.FLUID
                                .getOptional(id.withPath(id.getPath() + "_melt")));
                if (melt.isEmpty() || fluid.isEmpty())
                    return;
                var items = material.items(id);
                shapes.forEach((mold, shape) -> {
                    net.minecraft.world.item.ItemStack result = null;
                    net.minecraft.world.item.Item form = items.get(shape.form());
                    if (form != null)
                        result = new net.minecraft.world.item.ItemStack(form, shape.count());
                    else if (items.containsKey("ingot") && material.kind().filter(k -> k == Material.Kind.METAL).isPresent())
                        result = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Feedback.id(shape.form()))
                                .filter(i -> i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)
                                .map(part -> {
                                    var stack = new net.minecraft.world.item.ItemStack(part, shape.count());
                                    stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), new MadeOf(id, "ingot", 1));
                                    return stack;
                                }).orElse(null);
                    if (result != null)
                        all.add(new Casting(new net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient(
                                net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(fluid.get()), FormType.DEFAULT_MB),
                                shape.quenched() ? io.github.soundgoodizerfan.feedback.core.FTuning.WARM_TU : melt.get(), result, mold));
                });
            });
            entries = List.copyOf(all);
        }
        return entries;
    }

    /** The casting entry a mold's current contents satisfy, if any. */
    public Optional<Casting> find(FluidStack contents, String mold) {
        if (contents.isEmpty())
            return Optional.empty();
        return entries().stream().filter(entry -> entry.mold().equals(mold) && entry.matches(contents)).findFirst();
    }
}
