// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.induction;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.ItemStack;

/** What a changing field can heat: a conductor (a metal charge) or a ferromagnet (cookware). Read from material figures, never item ids. */
final class Induction {

    private Induction() {
    }

    /** Whether the stack's material is a metal. Walks the material table, so callers cache. */
    static boolean isMetal(ItemStack stack) {
        return !stack.isEmpty() && Material.find(MaterialTable.get().materials(), stack)
                .filter(found -> found.material().kind().filter(k -> k == Material.Kind.METAL).isPresent()).isPresent();
    }

    /** Whether the material filed under {@code id} is magnetic (the figure the Electromagnet reads). */
    static boolean isMagnetic(net.minecraft.resources.ResourceLocation id) {
        Material material = id == null ? null : MaterialTable.get().materials().get(id);
        return material != null && material.separation().magnetic();
    }
}
