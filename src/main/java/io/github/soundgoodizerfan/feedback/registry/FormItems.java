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
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.FormItem;
import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers one item for every material and form type that applies to it and has no item yet --
 * GregTech's "(prefix, material) pair per item", with absence rather than a flag as the switch.
 *
 * <h2>Why this reads the jar and not the datapack</h2>
 * Items must exist before any datapack loads, so the material and form-type files are read from
 * the mod's own jar at registration. The same files are then loaded again as data for the figures;
 * the two cannot disagree because they are one file. The cost is that a datapack can retune a
 * material but cannot add items to it.
 *
 * <h2>What is generated</h2>
 * Every {@link FormType} whose kinds include the material's, minus the forms the material lists
 * (those items already exist) and the ones it excludes. A block form registers a {@link FormBlock}
 * and its item. Generated items are named {@code <namespace>:<material>_<form>}; nothing already
 * registered is renamed.
 */
public final class FormItems {

    /** One generated item, kept for the creative tab and datagen. {@code block} is null for an item form. */
    public record Generated(ResourceLocation material, String form, DeferredItem<? extends Item> item,
                            DeferredBlock<FormBlock> block) {}

    private static final List<Generated> ALL = new ArrayList<>();
    private static Map<String, FormType> types = Map.of();
    private static Map<ResourceLocation, Material> materials = Map.of();

    private FormItems() {}

    public static List<Generated> all() {
        return List.copyOf(ALL);
    }

    /** Form types as read from the jar at registration. */
    public static Map<String, FormType> types() {
        return types;
    }

    /** Millibuckets in one item of {@code form}: its form type's, else one ingot's worth. */
    public static int volume(String form) {
        FormType type = types.get(form);
        return type == null ? FormType.DEFAULT_MB : type.mb();
    }

    /** The jar's own materials as read at registration -- for datagen, which has no datapack. */
    public static Map<ResourceLocation, Material> materials() {
        return materials;
    }

    /** The id of the item that is {@code material} in {@code form}: the shared part item for a part, else the generated one. */
    public static ResourceLocation itemId(ResourceLocation material, String form) {
        if (types.containsKey(form) && types.get(form).part())
            return Feedback.id(form);
        return ResourceLocation.fromNamespaceAndPath(material.getNamespace(), material.getPath() + "_" + form);
    }

    /** The jar's materials, read once; the client reads them again for fluid tints. */
    static Map<ResourceLocation, Material> jarMaterials() {
        if (materials.isEmpty())
            materials = read("material", Material.CODEC);
        return materials;
    }

    static void registerAll(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        jarMaterials();
        types = read("form_type", FormType.CODEC).entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().getPath(), Map.Entry::getValue));
        materials.forEach((id, material) -> {
            for (String form : material.generated(types)) {
                if (types.get(form).part())
                    continue;
                String path = itemId(id, form).getPath();
                if (types.get(form).block()) {
                    DeferredBlock<FormBlock> block = blocks.register(path, () -> new FormBlock(id, form));
                    DeferredItem<BlockItem> item = items.register(path,
                            () -> new FormBlock.FormBlockItem(block.get(), new Item.Properties()));
                    ALL.add(new Generated(id, form, item, block));
                } else {
                    DeferredItem<Item> item = items.registerItem(path, props -> new FormItem(props, id, form));
                    ALL.add(new Generated(id, form, item, null));
                }
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
