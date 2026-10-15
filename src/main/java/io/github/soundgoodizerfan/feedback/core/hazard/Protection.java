/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
        float best = FTuning.CONTACT_PAIN_TU.value();
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
