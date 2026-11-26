// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.lang.reflect.RecordComponent;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * An item told apart by one data component, as a material file's {@code accepts} writes it:
 * {@code thaumaturge:essentia_crystal[thaumaturge:crystal_aspect={"aspect":"thaumaturge:aer"}]}. A stack matches when each named field of
 * the component's value is the named id; other fields (an amount) are not asked. The component is read by id, so no type of the other mod
 * is named here.
 */
public record ComponentItem(ResourceLocation item, ResourceLocation component, JsonObject fields) {

    /** The entry {@code text} writes, or empty for a plain item id or tag (or text that is neither). */
    public static Optional<ComponentItem> parse(String text) {
        int open = text.indexOf('[');
        int equals = text.indexOf('=', open + 1);
        if (open < 0 || equals < 0 || !text.endsWith("]"))
            return Optional.empty();
        ResourceLocation item = ResourceLocation.tryParse(text.substring(0, open));
        ResourceLocation component = ResourceLocation.tryParse(text.substring(open + 1, equals));
        if (item == null || component == null)
            return Optional.empty();
        try {
            return Optional.of(new ComponentItem(item, component,
                    JsonParser.parseString(text.substring(equals + 1, text.length() - 1)).getAsJsonObject()));
        } catch (JsonParseException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    /** Whether {@code stack}, already known to be {@link #item}, carries the named fields. */
    public boolean matches(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(component);
        Object value = type == null ? null : stack.get(type);
        if (value == null || !value.getClass().isRecord())
            return false;
        return fields.entrySet().stream().allMatch(e -> e.getValue().getAsString().equals(field(value, e.getKey())));
    }

    private static String field(Object record, String name) {
        for (RecordComponent part : record.getClass().getRecordComponents()) {
            if (!part.getName().equals(name))
                continue;
            try {
                Object v = part.getAccessor().invoke(record);
                if (v instanceof Holder<?> holder)
                    return holder.unwrapKey().map(k -> k.location().toString()).orElse("");
                return String.valueOf(v);
            } catch (ReflectiveOperationException e) {
                return "";
            }
        }
        return "";
    }

    /** One stack carrying the component, for a recipe browser; empty when the item, component or the registries it names are not there. */
    public Optional<ItemStack> stack() {
        var found = BuiltInRegistries.ITEM.getOptional(item);
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(component);
        return found.flatMap(i -> type == null ? Optional.empty() : set(new ItemStack(i), type));
    }

    private <T> Optional<ItemStack> set(ItemStack stack, DataComponentType<T> type) {
        if (type.codec() == null)
            return Optional.empty();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
            @Override
            public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> key) {
                @SuppressWarnings("unchecked")
                var registry = (ResourceKey<Registry<E>>) (ResourceKey<?>) key;
                return Optional.ofNullable(CommonHooks.resolveLookup(registry))
                        .map(r -> new RegistryOps.RegistryInfo<>(r, r, r.registryLifecycle()));
            }
        });
        return type.codec().parse(ops, fields).result().map(value -> {
            stack.set(type, value);
            return stack;
        });
    }
}
