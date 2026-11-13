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
import io.github.soundgoodizerfan.feedback.core.EventTrigger;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The ages ({@link Age}, {@code data/<ns>/age/*.json}), and the moment a player enters one: taking a made tool or part out of a craft,
 * a bench or the Engineering Table fires the age advancement of each material it is made of. Found gear never passes through here, so it
 * does not count. Display only: nothing reads an age to allow or refuse a thing. The machine age is entered by an act, not a material
 * ({@link #MACHINE}). Synced to the client so a tooltip can name an age.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class Ages extends SimpleJsonResourceReloadListener {

    /** The machine age's event: a shop machine cut a part to a measured size (a lathe, drill press or grinding wheel). */
    public static final String MACHINE = "progress/age/machine";

    private static final Ages INSTANCE = new Ages();

    private List<Age> ages = List.of();

    private Ages() {
        super(new GsonBuilder().create(), "age");
    }

    public static Ages get() {
        return INSTANCE;
    }

    public List<Age> all() {
        return ages;
    }

    /** Client side: the server's ages. */
    public void set(List<Age> incoming) {
        ages = List.copyOf(incoming);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<Age> loaded = new ArrayList<>();
        json.forEach((id, element) -> {
            if (element.isJsonObject())
                element.getAsJsonObject().addProperty("id", id.toString());
            Age.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad age {}: {}", id, error))
                    .ifPresent(loaded::add);
        });
        ages = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} ages", ages.size());
    }

    /** The age whose specimen yields this treatise volume ({@code school} id, from 1), if one lists it. */
    public Optional<Age> ofVolume(String school, int volume) {
        String key = school + ":" + volume;
        return ages.stream().filter(a -> a.volumes().contains(key)).findFirst();
    }

    /** The player made {@code stack}: enter the age of each material it is made of. Server only. */
    public static void made(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer) || stack.isEmpty() || INSTANCE.ages.isEmpty())
            return;
        Map<ResourceLocation, Material> table = MaterialTable.get().materials();
        for (ResourceLocation id : materialsOf(stack)) {
            Material m = table.get(id);
            if (m != null)
                for (Age a : INSTANCE.ages)
                    if (a.has(id, m))
                        EventTrigger.fire(player, a.event());
        }
    }

    /** Every material a made part or built tool carries, main and extra slots; none for stock and found vanilla items. */
    static List<ResourceLocation> materialsOf(ItemStack stack) {
        List<ResourceLocation> out = new ArrayList<>();
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            add(out, made);
        ModularDesign design = ModularStacks.design(stack);
        if (design != null)
            for (ModularDesign.Part p : design.parts()) {
                out.add(p.material());
                p.measures().slots().values().forEach(s -> out.add(s.material()));
            }
        return out;
    }

    private static void add(List<ResourceLocation> out, MadeOf made) {
        out.add(made.material());
        made.measures().slots().values().forEach(s -> out.add(s.material()));
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        made(event.getEntity(), event.getCrafting());
    }
}
