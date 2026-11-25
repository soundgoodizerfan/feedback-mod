// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What the spectrometer reads on the tooltip of any sample: an alloy's constituents and their shares, an ore's metals and how much of each it
 * bears, each told as finely as the instrument resolves ({@link Readout#figure}); and, with Thaumaturge present, the aspects of the sample the reader has
 * discovered (the compat class names only what the reader's knowledge unmasks). Without the instrument a tooltip shows none of it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SpectrumTooltip {

    private SpectrumTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!Readout.instrumented(Quantity.COMPOSITION) || Minecraft.getInstance().level == null)
            return;
        var materials = ClientMaterials.on(Minecraft.getInstance().level);
        Material.find(materials, event.getItemStack()).ifPresent(found -> {
            Material m = found.material();
            List<Component> lines = new ArrayList<>();
            if (!m.composition().isEmpty())
                lines.add(Readout.reading(Quantity.COMPOSITION, "feedback.readout.makeup", shares(m.composition(), materials)));
            else if (!m.bears().isEmpty())
                lines.add(Readout.reading(Quantity.COMPOSITION, "feedback.readout.bears", shares(m.bears(), materials)));
            lines.forEach(l -> event.getToolTip().add(l.copy().withStyle(ChatFormatting.GRAY)));
        });
        if (ModList.get().isLoaded("thaumaturge")) {
            try {
                @SuppressWarnings("unchecked")
                List<Component> aspects = (List<Component>) Class.forName("io.github.soundgoodizerfan.feedback.compat.thaumaturge.api.ThaumaturgeSpectrum")
                        .getMethod("known", net.minecraft.world.item.ItemStack.class).invoke(null, event.getItemStack());
                if (!aspects.isEmpty())
                    event.getToolTip().add(Readout.reading(Quantity.COMPOSITION, "feedback.readout.aspects",
                            aspects.stream().reduce((a, b) -> a.copy().append(", ").append(b)).orElse(Component.empty())).copy().withStyle(ChatFormatting.GRAY));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Thaumaturge's API changed shape: the spectrometer reads makeup only.
            }
        }
    }

    /** {@code Copper 90%, Tin 10%}: shares of the whole, in the material's own names, to the instrument's resolution. */
    private static Component shares(Map<ResourceLocation, Float> parts, Map<ResourceLocation, Material> materials) {
        float total = 0f;
        for (float v : parts.values())
            total += v;
        Component out = Component.empty();
        boolean first = true;
        for (var e : parts.entrySet()) {
            if (!first)
                out = out.copy().append(", ");
            first = false;
            String shown = Readout.figure(Quantity.COMPOSITION, total <= 0f ? 0f : 100f * e.getValue() / total);
            out = out.copy().append(Component.translatable("material." + e.getKey().getNamespace() + "." + e.getKey().getPath())).append(" " + shown + "%");
        }
        return out;
    }
}
