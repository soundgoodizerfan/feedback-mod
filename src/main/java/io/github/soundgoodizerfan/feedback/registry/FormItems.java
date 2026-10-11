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
package io.github.soundgoodizerfan.feedback.registry;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.FormItem;
import io.github.soundgoodizerfan.feedback.process.FormStep;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers one item for every material and form that has no item yet -- GregTech's
 * "(prefix, material) pair per item", with absence rather than a flag as the switch.
 *
 * <h2>Why this reads the jar and not the datapack</h2>
 * Items must exist before any datapack loads, so the material and form files are read from the
 * mod's own jar at registration. The same files are then loaded again as data for the figures;
 * the two cannot disagree because they are one file. The cost is that a datapack can retune a
 * material but cannot add items to it.
 *
 * <h2>What is generated</h2>
 * Every form named by any step, for every material, minus the forms the material lists (those
 * items already exist) and the ones it excludes. Generated items are named
 * {@code <namespace>:<material>_<form>}; nothing already registered is renamed.
 */
public final class FormItems {

    /** One generated item, kept for the creative tab and datagen. */
    public record Generated(ResourceLocation material, String form, DeferredItem<Item> item) {}

    private static final List<Generated> ALL = new ArrayList<>();

    private FormItems() {}

    public static List<Generated> all() {
        return List.copyOf(ALL);
    }

    public static ResourceLocation itemId(ResourceLocation material, String form) {
        return ResourceLocation.fromNamespaceAndPath(material.getNamespace(), material.getPath() + "_" + form);
    }

    static void registerAll(DeferredRegister.Items items) {
        Map<ResourceLocation, Material> materials = read("material", Material.CODEC);
        Set<String> universe = new TreeSet<>();
        read("form", FormStep.CODEC).values().forEach(step -> {
            universe.add(step.from());
            universe.add(step.to());
        });
        materials.forEach((id, material) -> {
            for (String form : material.generated(universe)) {
                ResourceLocation itemId = itemId(id, form);
                DeferredItem<Item> item = items.registerItem(itemId.getPath(),
                        props -> new FormItem(props, id, form));
                ALL.add(new Generated(id, form, item));
            }
        });
        Feedback.LOGGER.info("Generated {} form items for {} materials", ALL.size(), materials.size());
    }

    private static <T> Map<ResourceLocation, T> read(String directory, Codec<T> codec) {
        Map<ResourceLocation, T> out = new TreeMap<>();
        Path dir = ModList.get().getModFileById(Feedback.MOD_ID).getFile()
                .findResource("data", Feedback.MOD_ID, directory);
        if (!Files.isDirectory(dir))
            return out;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(p -> {
                String name = p.getFileName().toString();
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID,
                        name.substring(0, name.length() - ".json".length()));
                try (Reader reader = Files.newBufferedReader(p)) {
                    codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                            .resultOrPartial(error -> Feedback.LOGGER.error("Bad {} {}: {}", directory, id, error))
                            .ifPresent(value -> out.put(id, value));
                } catch (IOException e) {
                    Feedback.LOGGER.error("Could not read {}", p, e);
                }
            });
        } catch (IOException e) {
            Feedback.LOGGER.error("Could not list {}", dir, e);
        }
        return out;
    }
}
