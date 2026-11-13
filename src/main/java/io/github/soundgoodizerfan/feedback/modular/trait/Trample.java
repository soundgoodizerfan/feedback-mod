// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.combat.cast.LandOn;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.combat.Smash;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Sabatons (spec 5.4, armour): the landing, made heavy. Landing from a fall of {@code min_fall} blocks or more
 * lands on every creature within {@code radius} of the wearer ({@link LandOn#land}: damage by mass ratio, with
 * {@code weight} Mu of shell added to the wearer), halves the wearer's own fall damage if anything was struck, and
 * throws the mace's ring of shove, sound and dust {@code shove} times as hard ({@link Smash#shockwave}). Landing on
 * a soft block (wool, snow or leaves) costs the wearer no fall damage at all. It wears the piece by {@code wear};
 * the cost beyond that is mass, the heavy six-cell shell. Worn: counts once.
 */
public final class Trample implements ModularTrait {

    @Override
    public void onWornFall(LivingFallEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        var below = wearer.getBlockStateOn();
        if (below.is(BlockTags.WOOL) || below.is(BlockTags.SNOW) || below.is(BlockTags.LEAVES))
            event.setDamageMultiplier(0f);
        float fall = event.getDistance();
        if (fall < params.get("min_fall", 3f))
            return;
        if (LandOn.land(wearer, fall, params.get("radius", 1.5f), params.get("weight", 40f)))
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.5f);
        Smash.shockwave(wearer.level(), wearer, wearer.position(), wearer.getOnPos(), Smash.RADIUS, params.get("shove", 1f),
                fall > Smash.HEAVY_FALL);
        piece.hurtAndBreak(ModularStacks.cap(piece, (int) params.get("wear", 1f)), wearer, ((ArmorItem) piece.getItem()).getEquipmentSlot());
    }
}
