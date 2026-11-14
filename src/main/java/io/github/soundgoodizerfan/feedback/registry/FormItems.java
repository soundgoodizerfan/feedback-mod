// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
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
            materials = parse("material", io.github.soundgoodizerfan.feedback.process.MaterialTable.resolved(readJson("material")), Material.CODEC);
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
                    DeferredBlock<FormBlock> block = blocks.register(path, () -> form.equals("pipe")
                            ? new io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlock(id) : new FormBlock(id, form, material.food().isPresent()));
                    DeferredItem<BlockItem> item = items.register(path,
                            () -> new FormBlock.FormBlockItem(block.get(), proof(edible(new Item.Properties(), material, form), material)));
                    ALL.add(new Generated(id, form, item, block));
                } else {
                    DeferredItem<Item> item = items.registerItem(path, props -> new FormItem(proof(edible(props, material, form), material), id, form));
                    ALL.add(new Generated(id, form, item, null));
                }
            }
        });
        Feedback.LOGGER.info("Generated {} form items for {} materials", ALL.size(), materials.size());
    }

    /**
     * A foodstuff's block or log is food: its raw item's hunger, saturation and effects, scaled by how many raw items' worth of
     * volume it holds (a block is nine). Anything else is returned unchanged.
     */
    /** An item made of stuff that survives lava (its failure temperature is over lava's fire) is fire resistant, by vanilla's own component. */
    private static Item.Properties proof(Item.Properties props, Material material) {
        return material.survivesLava() ? props.fireResistant() : props;
    }

    private static Item.Properties edible(Item.Properties props, Material material, String form) {
        ResourceLocation raw = material.food().isPresent() ? material.forms().get("raw") : null;
        FoodProperties base = raw == null ? null : BuiltInRegistries.ITEM.getOptional(raw).map(i -> i.components().get(DataComponents.FOOD)).orElse(null);
        if (base == null)
            return props;
        float units = volume(form) / (float) FormType.DEFAULT_MB;
        return props.food(new FoodProperties(Math.round(base.nutrition() * units), base.saturation() * units, base.canAlwaysEat(),
                Math.min(6f, base.eatSeconds() * units), base.usingConvertsTo(), base.effects()));
    }

    private static <T> Map<ResourceLocation, T> read(String directory, Codec<T> codec) {
        return parse(directory, readJson(directory), codec);
    }

    private static <T> Map<ResourceLocation, T> parse(String directory, Map<ResourceLocation, com.google.gson.JsonElement> json, Codec<T> codec) {
        Map<ResourceLocation, T> out = new TreeMap<>();
        json.forEach((id, element) -> codec.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad {} {}: {}", directory, id, error))
                .ifPresent(value -> out.put(id, value)));
        return out;
    }

    private static Map<ResourceLocation, com.google.gson.JsonElement> readJson(String directory) {
        Map<ResourceLocation, com.google.gson.JsonElement> out = new TreeMap<>();
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
                    out.put(id, JsonParser.parseReader(reader));
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
