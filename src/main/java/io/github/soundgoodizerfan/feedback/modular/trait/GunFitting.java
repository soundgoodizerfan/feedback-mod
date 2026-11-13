// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** The material of a fitting on a built gun, found by the form it was made as: fittings whose effect is the stuff itself read it here. */
final class GunFitting {

    private GunFitting() {
    }

    @Nullable
    static Material of(ItemStack gun, String form) {
        var design = ModularStacks.design(gun);
        if (design == null)
            return null;
        for (var p : design.parts())
            if (p.kind().equals(form))
                return MaterialTable.get().materials().get(p.material());
        return null;
    }

    /** The material in {@code slot} of the part made as {@code form}: its own slot material, else the part's main one; null if no such part. */
    @Nullable
    static Material slot(ItemStack gun, String form, String slot) {
        var design = ModularStacks.design(gun);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known(design.type());
        if (type == null)
            return null;
        var materials = MaterialTable.get().materials();
        for (var p : design.parts())
            if (p.kind().equals(form)) {
                var role = type.roles().get(p.role());
                Material m = role == null ? null : io.github.soundgoodizerfan.feedback.modular.bake.ModularBake.slotMaterial(role, p, slot, materials);
                return m != null ? m : materials.get(p.material());
            }
        return null;
    }
}
