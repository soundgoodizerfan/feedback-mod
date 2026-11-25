// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.ItemStack;

/** What the warehouse asks of a stack: what it weighs, and whether it pours. Never an item id. */
public final class Goods {

    /** Forms that pour whatever they are made of. */
    private static final Set<String> LOOSE_FORMS = Set.of("dust", "crushed", "concentrate", "calcine", "tailings", "swarf",
            "shaving", "loose_rock", "ore", "deepslate_ore");

    private Goods() {
    }

    /** Mass in Mu of one item of {@code stack}, a loaded rack's contents included. */
    public static float unitMass(ItemStack stack) {
        return ModularBackpack.massOf(stack.getItem()) + RackStock.mass(stack);
    }

    /**
     * Whether {@code stack} pours into a bulk hopper: a granular or grain material, or any material in a loose form, and
     * nothing of its own on the stack (a heap has no per-piece state).
     */
    public static boolean loose(ItemStack stack) {
        if (!stack.getComponentsPatch().isEmpty())
            return false;
        return Material.find(MaterialTable.get().materials(), stack.getItem()).map(l -> {
            Material.Kind kind = l.material().kind().orElse(null);
            return LOOSE_FORMS.contains(l.form()) || kind == Material.Kind.GRANULAR || kind == Material.Kind.GRAIN;
        }).orElse(false);
    }
}
