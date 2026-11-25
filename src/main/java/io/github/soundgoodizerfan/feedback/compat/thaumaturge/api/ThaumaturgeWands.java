// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge.api;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandCap;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wand caps and rods made of Feedback's materials, for Thaumaturge wands. Thaumaturge's wand part registries are ordinary registries,
 * frozen once startup registration ends, so the parts are a fixed list registered here from Feedback's own material files read out of
 * its jar (a pack's later materials get none). A cap's cost modifier follows how well its metal conducts (the better the conductor, the
 * cheaper the vis), its craft cost how high it melts; a rod's capacity follows how well it insulates and how dense it is. Each part is also an
 * item (Thaumaturge's own item classes), which is all its wand assembly needs to offer the recipes. Loaded only through {@link #init},
 * which {@code Feedback} calls by reflection when Thaumaturge is present.
 */
public final class ThaumaturgeWands {

    private static final String PACK = "feedback";
    private static final String CONDUCTOR_REFERENCE = "silver";

    private record Part(String id, String standIn) {
    }

    /** Cap metals and the Thaumaturge cap texture each borrows until it has its own. */
    private static final List<Part> CAPS = List.of(new Part("steel", "iron"), new Part("bronze", "copper"), new Part("tin", "silver"),
            new Part("aluminium", "silver"), new Part("titanium", "silver"), new Part("platinum", "gold"));
    /** Rod materials and the rod texture each borrows. */
    private static final List<Part> RODS = List.of(new Part("bone", "bone"), new Part("bamboo", "reed"), new Part("amber", "silverwood"),
            new Part("quartz_glass", "quartz"));

    private ThaumaturgeWands() {
    }

    /** Registers the parts and their items on {@code modBus}. */
    public static void init(IEventBus modBus) {
        DeferredRegister<WandCap> caps = DeferredRegister.create(WandCap.REGISTRY_KEY, Feedback.MOD_ID);
        DeferredRegister<WandRod> rods = DeferredRegister.create(WandRod.REGISTRY_KEY, Feedback.MOD_ID);
        DeferredRegister.Items items = DeferredRegister.createItems(Feedback.MOD_ID);
        double reference = conductivity(CONDUCTOR_REFERENCE);
        for (Part p : CAPS) {
            JsonObject m = material(p.id());
            float conduct = (float) Math.min(1.0, conductivity(p.id()) / Math.max(1e-9, reference));
            float modifier = 1.2f - 0.4f * conduct;
            int cost = Math.max(1, Math.round(number(m, "thermal", "melting_point") / 600f));
            DeferredHolder<WandCap, WandCap> cap = caps.register(p.id(), () -> new WandCap(modifier, List.of(), 0f, cost,
                    ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/models/wand_cap_" + p.standIn() + ".png")));
            items.registerItem("wand_cap_" + p.id(), props -> new ItemWandCap(props.stacksTo(1), cap));
        }
        for (Part p : RODS) {
            JsonObject m = material(p.id());
            float insulation = 1f - (float) Math.min(1.0, conductivity(p.id()) / Math.max(1e-9, reference));
            float density = (float) Math.min(1.0, number(m, null, "density") / 3.0);
            int capacity = Math.round(15f + 45f * insulation + 25f * density);
            int cost = Math.max(1, 1 + Math.round(number(m, null, "hardness") / 50f));
            DeferredHolder<WandRod, WandRod> rod = rods.register(p.id(), () -> new WandRod(capacity, cost,
                    ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/models/wand_rod_" + p.standIn() + ".png"), null, false, false, false));
            items.registerItem("wand_rod_" + p.id(), props -> new ItemWandRod(props.stacksTo(1), rod));
        }
        caps.register(modBus);
        rods.register(modBus);
        items.register(modBus);
    }

    /** The wand part items, for the creative tab. */
    public static List<String> itemIds() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Part p : CAPS)
            out.add("wand_cap_" + p.id());
        for (Part p : RODS)
            out.add("wand_rod_" + p.id());
        return out;
    }

    private static JsonObject material(String id) {
        try (var in = ThaumaturgeWands.class.getResourceAsStream("/data/feedback/material/" + id + ".json")) {
            return in == null ? new JsonObject() : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static double conductivity(String id) {
        JsonObject e = material(id);
        return number(e, "electrical", "conductivity");
    }

    /** A figure of a material file, {@code group.key} (or top level when {@code group} is null), 0 when it states none (a non-conductor has no electrical block). */
    private static float number(JsonObject m, String group, String key) {
        JsonObject o = group == null ? m : m.has(group) && m.get(group).isJsonObject() ? m.getAsJsonObject(group) : null;
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsFloat() : 0f;
    }
}
