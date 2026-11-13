// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * Collar (spec 5.3, armour addon on a helmet or a chestplate): a ring about the neck takes a share of a critical hit's
 * extra damage and of a backstab's. A stiff collar (plate) takes {@code stiff_share}, a flexible one (chain, leather,
 * fibre) {@code flex_share}. Worn: counts once.
 */
public final class Collar implements ModularTrait {

    /** The collar role's letter in the helmet and chestplate type files. */
    private static final String ROLE = "C";

    /** The share of the bonus the collar of {@code piece} takes: by its material, stiff or flexible. */
    static float share(ItemStack piece, Params params) {
        ModularDesign design = ModularStacks.design(piece);
        ModularDesign.Part collar = design == null ? null : design.part(ROLE);
        if (collar == null)
            return 0f;
        Material m = MaterialTable.get().materials().get(collar.material());
        boolean flexible = collar.form().equals("chain") || collar.form().equals("fibre") || collar.form().equals("leather")
                || (m != null && m.traits().contains(Material.Trait.FIBROUS));
        return params.get(flexible ? "flex_share" : "stiff_share", flexible ? 0.2f : 0.5f);
    }

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (Backstab.lastTarget != wearer || Backstab.lastMult <= 1f)
            return;
        float mult = Backstab.lastMult;
        Backstab.lastTarget = null;
        float base = event.getAmount() / mult;
        event.setAmount(base + (event.getAmount() - base) * (1f - share(piece, params)));
    }

    @Override
    public void onWornCrit(CriticalHitEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        float m = event.getDamageMultiplier();
        if (m > 1f)
            event.setDamageMultiplier(1f + (m - 1f) * (1f - share(piece, params)));
    }
}
