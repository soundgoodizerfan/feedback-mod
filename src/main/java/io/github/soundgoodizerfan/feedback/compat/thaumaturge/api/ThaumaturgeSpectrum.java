// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge.api;

import java.util.ArrayList;
import java.util.List;

import com.leclowndu93150.thaumaturge.api.aspect.AspectComponents;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectKnowledgeAccess;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The aspects of a sample that the reader has discovered, as lines for the spectrometer's tooltip. Reached by reflection from {@code SpectrumTooltip}, client side only. */
public final class ThaumaturgeSpectrum {

    private ThaumaturgeSpectrum() {
    }

    /** {@code Ignis 4}, one per aspect of {@code stack} the reader knows; aspects not yet discovered are left out (the spectrometer does not unmask them). */
    public static List<Component> known(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        for (AspectInstance a : AspectIndexAccess.of(stack).entries())
            if (AspectKnowledgeAccess.isKnown(a.aspect()))
                out.add(Component.empty().append(AspectComponents.trueName(a.aspect())).append(" " + a.amount()));
        return out;
    }
}
