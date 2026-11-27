// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IItemEssentia;

import io.github.soundgoodizerfan.feedback.data.AspectData;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * What a modular item scans as: the aspects of the materials it is built from, by how much of each went in, so an iron-headed and a
 * bronze-headed pickaxe differ. Thaumaturge's index keys aspects by item id, but for a stack it first asks the stack's
 * {@code EssentiaCapabilities.CONTAINER} (an item capability) and uses that view's aspects when it has any; every modular item and part item
 * answers that here. Loaded only through {@link #init}.
 *
 * <p>Taken from Thaumaturge (source-available, publishing barred, dormancy converts it to MIT): the public item capability
 * {@code EssentiaCapabilities.CONTAINER} and the interface {@code IItemEssentia} (and that the index reads it for a stack), {@code AspectList},
 * {@code AspectInstance} and {@code Aspects.resolve}, as a reader of its source. Rejected: its code, its values, and treating the item as an
 * essentia container (the view takes nothing and stores nothing; {@code setAspects} does nothing).
 */
public final class ThaumaturgeModularAspects {

    /** Thaumaturge's own cap per aspect per item. */
    private static final int MAX_AMOUNT = 500;
    /** The craft an assembly adds, as {@code AspectData} gives an engineered item. */
    private static final int ASSEMBLY_FABRICO = 2;

    /** The registries of each side, kept from the last tag update: an aspect is a dynamic registry entry, and a capability gets no level. */
    private static volatile HolderLookup.Provider serverRegistries;
    private static volatile HolderLookup.Provider clientRegistries;
    /** A material's aspects for an ingot-sized piece, so a tooltip does not rework them every frame; cleared whenever data reloads. */
    private static final Map<ResourceLocation, Map<String, Float>> PER_INGOT = new ConcurrentHashMap<>();

    private ThaumaturgeModularAspects() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ThaumaturgeModularAspects::capabilities);
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> {
            PER_INGOT.clear();
            if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
                serverRegistries = e.getRegistryAccess();
            else
                clientRegistries = e.getRegistryAccess();
        });
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (Item item : BuiltInRegistries.ITEM)
            if (item instanceof Modular || item instanceof ModularPartItem)
                event.registerItem(EssentiaCapabilities.CONTAINER, (stack, ctx) -> view(stack), item);
    }

    /** The view for a stack that is built of something, else none (so Thaumaturge falls back to the item's own entry). */
    private static IItemEssentia view(ItemStack stack) {
        AspectList list = aspects(stack);
        // One line per item the first time it scans, to check in run/logs/latest.log that Thaumaturge asked and what it was told.
        if (LOGGED.add(stack.getItem()))
            LOG.info("modular aspects for {}: {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), list.isEmpty() ? "none (registries or parts missing)" : list);
        return list.isEmpty() ? null : new View(list);
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("feedback/aspects");
    private static final java.util.Set<Item> LOGGED = ConcurrentHashMap.newKeySet();

    private static AspectList aspects(ItemStack stack) {
        Map<String, Float> sum = new LinkedHashMap<>();
        gather(stack, sum, 0);
        if (sum.isEmpty())
            return AspectList.EMPTY;
        sum.merge("fabrico", (float) ASSEMBLY_FABRICO, Float::sum);
        HolderLookup.Provider registries = registries();
        if (registries == null)
            return AspectList.EMPTY;
        AspectList out = AspectList.EMPTY;
        for (Map.Entry<String, Float> e : sum.entrySet()) {
            var holder = Aspects.resolve(registries, ResourceKey.<IAspect>create(IAspect.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("thaumaturge", e.getKey())));
            if (holder != null)
                out = out.add(new AspectInstance(holder, Math.max(1, Math.min(MAX_AMOUNT, Math.round(e.getValue())))));
        }
        return out;
    }

    /** Adds what the stack is made of to {@code sum}; a part nested in a design (a suit's module) is counted in turn. */
    private static void gather(ItemStack stack, Map<String, Float> sum, int depth) {
        if (depth > 4)
            return;
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            add(sum, made.material(), made.stock(), made.cells());
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return;
        for (ModularDesign.Part part : design.parts()) {
            if (part.sub() != null && !part.sub().isEmpty())
                gather(part.sub(), sum, depth + 1);
            else
                add(sum, part.material(), part.form(), part.count());
        }
    }

    private static void add(Map<String, Float> sum, ResourceLocation id, String form, int count) {
        Material material = id == null ? null : MaterialTable.get().materials().get(id);
        if (material == null)
            return;
        float volume = (float) FormItems.volume(form) / FormType.DEFAULT_MB * Math.max(1, count);
        PER_INGOT.computeIfAbsent(id, k -> AspectData.aspects(material, ""))
                .forEach((aspect, perIngot) -> sum.merge(aspect, perIngot * volume, Float::sum));
    }

    /** The server's registries on the server thread, the client's otherwise (a dedicated server has no client's). */
    private static HolderLookup.Provider registries() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread() || clientRegistries == null)
            return serverRegistries;
        return clientRegistries;
    }

    /** A read-only view: the item is not a container, it only says what it is made of. */
    private record View(AspectList aspects) implements IItemEssentia {
        @Override
        public AspectList getAspects() {
            return aspects;
        }

        @Override
        public void setAspects(AspectList ignored) {
        }
    }
}
