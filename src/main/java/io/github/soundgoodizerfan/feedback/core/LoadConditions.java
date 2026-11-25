// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * The {@code neoforge:conditions} array on our own data tables, which the stock reload listener ignores: a row or material
 * that names another mod's fluid or item carries {@code [{"type": "neoforge:mod_loaded", "modid": "thaumaturge"}]} and is
 * dropped before parsing when that mod is absent, so nothing logs a parse error for content that cannot exist.
 *
 * <p>Understood types: {@code neoforge:mod_loaded}, {@code neoforge:not}, {@code neoforge:and}, {@code neoforge:or},
 * {@code neoforge:true} and {@code neoforge:false}. Any other type fails the file, so a condition we cannot read never
 * silently admits content. Item and tag conditions need a loaded registry and are not offered here.
 */
public final class LoadConditions {

    public static final String KEY = "neoforge:conditions";

    /**
     * True in the data run ({@code -Dfeedback.datagen=true}, set by build.gradle). Datagen has no Thaumaturge or AE2 to load, yet the
     * data of a conditional material must still be generated, each file carrying its condition, so every {@code mod_loaded} holds there.
     */
    public static final boolean DATAGEN = Boolean.getBoolean("feedback.datagen");

    private LoadConditions() {
    }

    /** The files whose conditions hold, in the order given. A file with no conditions is kept. */
    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> json) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        json.forEach((id, element) -> {
            if (passes(element))
                out.put(id, element);
        });
        return out;
    }

    /** Whether a file's own conditions all hold. */
    public static boolean passes(JsonElement element) {
        if (!element.isJsonObject() || !element.getAsJsonObject().has(KEY))
            return true;
        for (JsonElement condition : element.getAsJsonObject().getAsJsonArray(KEY))
            if (!holds(condition))
                return false;
        return true;
    }

    private static boolean holds(JsonElement element) {
        if (!element.isJsonObject())
            return false;
        JsonObject o = element.getAsJsonObject();
        String type = o.has("type") ? o.get("type").getAsString() : "";
        switch (type) {
            case "neoforge:mod_loaded":
                return DATAGEN || ModList.get().isLoaded(o.get("modid").getAsString());
            case "neoforge:not":
                return !holds(o.get("value"));
            case "neoforge:and":
                for (JsonElement v : o.getAsJsonArray("values"))
                    if (!holds(v))
                        return false;
                return true;
            case "neoforge:or":
                for (JsonElement v : o.getAsJsonArray("values"))
                    if (holds(v))
                        return true;
                return false;
            case "neoforge:true":
                return true;
            default:
                return false;
        }
    }
}
