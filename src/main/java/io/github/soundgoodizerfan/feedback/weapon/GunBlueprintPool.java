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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The one pool of gun blueprints, from
 * {@code data/<namespace>/gun_blueprint_pool/*.json}: each file is one {@link ModularDesign}, the
 * same thing a held blueprint carries. A chest draws from all of it ({@link Loot}); a skeleton draws
 * from the entries that {@link #early} passes, so the line between them is a read of the design and
 * never a list of ids. An entry that is not a gun the Tool Table can make is logged and dropped.
 */
public final class GunBlueprintPool extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "gun_blueprint_pool";

    private static final GunBlueprintPool INSTANCE = new GunBlueprintPool();

    /** @param round the round a skeleton carrying this gun is given, present only if the design is an early gun. */
    public record Entry(ResourceLocation id, ModularDesign design, Optional<Cartridge> round) {
    }

    private List<Entry> entries = List.of();

    private GunBlueprintPool() {
        super(new com.google.gson.GsonBuilder().create(), DIRECTORY);
    }

    public static GunBlueprintPool get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        var types = ModularTypes.get().types();
        var materials = MaterialTable.get().materials();
        List<Entry> loaded = new ArrayList<>();
        new TreeMap<>(json).forEach((id, element) -> ModularDesign.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad gun blueprint {}: {}", id, error))
                .ifPresent(design -> {
                    Optional<GunSpec> spec = GunSpec.of(design, types.get(design.type()), materials);
                    if (spec.isEmpty() || ToolDesigns.make(design, types, materials).isEmpty())
                        Feedback.LOGGER.error("Gun blueprint {} is not a gun the Tool Table makes", id);
                    else
                        loaded.add(new Entry(id, design, early(spec.get(), materials)));
                }));
        entries = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} gun blueprints ({} early)", entries.size(), entries.stream().filter(e -> e.round().isPresent()).count());
    }

    /**
     * The round a skeleton may carry for {@code spec}, if it is an early gun: single-shot, or worked by a
     * lever or a bolt, and its figures with the nominal black-powder round (the bore's own) peak no higher
     * than black powder's pressure. Read from the design and the material table.
     */
    public static Optional<Cartridge> early(GunSpec spec, Map<ResourceLocation, Material> materials) {
        GunLayout.Action action = spec.layout().action();
        boolean manual = action == GunLayout.Action.BOLT || action == GunLayout.Action.LEVER;
        if (!manual && spec.internalCapacity().orElse(0) != 1)
            return Optional.empty();
        Material powder = materials.get(Feedback.id("gunpowder"));
        if (powder == null || powder.propellant().isEmpty())
            return Optional.empty();
        float ceiling = powder.propellant().get().peakPressure().value();
        Cartridge round = Cartridge.reference(spec.boreMm());
        return spec.bake(materials, round)
                .filter(f -> f.peakPressure().value() <= ceiling * 1.001f)
                .map(f -> round);
    }

    /** A random blueprint from the whole pool, or empty if the pool is. */
    public Optional<ItemStack> draw(RandomSource random) {
        return entries.isEmpty() ? Optional.empty() : Optional.of(blueprint(entries.get(random.nextInt(entries.size())).design()));
    }

    /** A random entry a skeleton may carry, or empty if none is. */
    public Optional<Entry> drawEarly(RandomSource random) {
        List<Entry> early = entries.stream().filter(e -> e.round().isPresent()).toList();
        return early.isEmpty() ? Optional.empty() : Optional.of(early.get(random.nextInt(early.size())));
    }

    /** The early entries' ids, for a command to suggest. */
    public List<ResourceLocation> earlyIds() {
        return entries.stream().filter(e -> e.round().isPresent()).map(Entry::id).toList();
    }

    /** The early entry called {@code id}, if there is one. */
    public Optional<Entry> early(ResourceLocation id) {
        return entries.stream().filter(e -> e.round().isPresent() && e.id().equals(id)).findFirst();
    }

    public static ItemStack blueprint(ModularDesign design) {
        ItemStack stack = new ItemStack(FModular.BLUEPRINT.get());
        stack.set(FModular.DESIGN.get(), design);
        return stack;
    }

    /** Adds one blueprint from the pool to a chest's loot; the chest tables and the chance are the modifier's conditions. */
    public static class Loot extends LootModifier {

        public static final MapCodec<Loot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, Loot::new));

        public Loot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            INSTANCE.draw(context.getRandom()).ifPresent(loot::add);
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
