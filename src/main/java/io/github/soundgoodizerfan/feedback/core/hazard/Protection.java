// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * What a player has between their hands and the heat or the wire, gathered from both hands and
 * the Curios {@code hands} slot.
 *
 * <p>Sums are maxima, not totals: two pairs of tongs are not twice as safe. Gloves are worn, so
 * they are always in effect; tongs only count while they are in a hand -- which is what makes them
 * a tool you pick up for the job and not an armour piece.
 */
public final class Protection {

    private Protection() {
    }

    /** The hottest thing this player may hold or touch unhurt right now. */
    public static float handlesUpToTu(Player player) {
        Level level = player.level();
        // Callused Hands (Proficiency): bare skin takes the scald band.
        float best = io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(player, "callused_hands")
                ? FTuning.CONTACT_BURN_TU.value() : FTuning.CONTACT_PAIN_TU.value();
        // Smelter's blood (potion): a ceiling raise on bare skin only; a hotter piece still burns.
        if (player.hasEffect(io.github.soundgoodizerfan.feedback.registry.FEffects.SMELTERS_BLOOD))
            best += io.github.soundgoodizerfan.feedback.potion.PotionTuning.SMELTERS_BLOOD_TU;
        for (ItemStack stack : carried(player))
            if (stack.getItem() instanceof Insulator in)
                best = Math.max(best, in.handlesUpToTu(stack, level));
        return best;
    }

    /** The highest voltage of a bare conductor this player is insulated from, or {@code 0}. */
    public static float shockProofVolts(Player player) {
        Level level = player.level();
        float best = 0f;
        for (ItemStack stack : carried(player))
            if (stack.getItem() instanceof Insulator in)
                best = Math.max(best, in.shockProofVolts(stack, level));
        return best;
    }

    private static java.util.List<ItemStack> carried(Player player) {
        java.util.List<ItemStack> out = new java.util.ArrayList<>(4);
        out.add(player.getItemInHand(InteractionHand.MAIN_HAND));
        out.add(player.getItemInHand(InteractionHand.OFF_HAND));
        CuriosApi.getCuriosInventory(player).ifPresent(inv ->
                inv.findCurios(s -> s.getItem() instanceof Insulator)
                        .forEach(r -> out.add(r.stack())));
        return out;
    }
}
