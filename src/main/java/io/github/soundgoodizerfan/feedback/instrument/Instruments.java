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
package io.github.soundgoodizerfan.feedback.instrument;

import io.github.soundgoodizerfan.feedback.item.InstrumentBagItem;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Finds what the player can measure with. */
public final class Instruments {

    private Instruments() {
    }

    /**
     * The finest instrument the player is carrying that can read this quantity, or null.
     *
     * <h3>Carried, not held</h3>
     * Reading is passive and costs no hands: you look at something and either own the means to put
     * a number on it or you do not. Requiring an instrument in hand would mean swapping tools to
     * look at things, which is friction with no decision inside it (§3). Holding is reserved for
     * <em>acting</em> on the world. What you can perceive is a property of your kit; what you can
     * do is a property of your hands.
     */
    @Nullable
    public static Instrument best(@Nullable Player player, Quantity quantity) {
        if (player == null)
            return null;

        Inventory inventory = player.getInventory();
        Instrument best = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            // A bag's contents count as carried: the bag only changes where the kit is kept.
            if (stack.getItem() instanceof InstrumentBagItem) {
                for (ItemStack inside : InstrumentBagItem.contents(stack))
                    best = finer(best, inside, quantity);
            } else {
                best = finer(best, stack, quantity);
            }
        }
        return best;
    }

    @Nullable
    private static Instrument finer(@Nullable Instrument best, ItemStack stack, Quantity quantity) {
        if (!(stack.getItem() instanceof Instrument candidate) || !candidate.canRead(quantity))
            return best;
        return best == null || candidate.resolution(quantity) < best.resolution(quantity) ? candidate : best;
    }

    /** Whether the player reads this quantity at the truth tier ({@link Instrument#TRUTH}). */
    public static boolean exact(@Nullable Player player, Quantity quantity) {
        Instrument best = best(player, quantity);
        return best != null && best.resolution(quantity) <= Instrument.TRUTH;
    }

    /**
     * {@code value} to {@code digits} significant figures, no trailing zeros. A float holds about
     * seven, so six is still the held figure; beyond it is rounding noise ({@code 0.19999999}).
     */
    public static String sig(float value, int digits) {
        if (value == 0 || !Float.isFinite(value))
            return "0";
        return new java.math.BigDecimal(Float.toString(value)).round(new java.math.MathContext(digits))
                .stripTrailingZeros().toPlainString();
    }

    /**
     * A figure as finely as {@code resolution} stands behind and no finer (philosophy 8:
     * instruments never lie): quantised, with as many decimal places as the step has. At the
     * truth tier it is the held float, exactly.
     */
    public static String format(float value, float resolution) {
        if (resolution <= Instrument.TRUTH)
            return sig(value, 6);
        int places = Math.max(0, (int) Math.ceil(-Math.log10(resolution) - 1e-6));
        return String.format(java.util.Locale.ROOT, "%." + places + "f", quantise(value, resolution));
    }

    /**
     * A figure with the instrument that took it on the front: {@code Calipers: 9 / 14 Fu}.
     *
     * <h3>Why the wording lives here and not in the readout</h3>
     * Philosophy 8 promises instruments never lie, not that they are accurate, so every figure the
     * mod shows is attributed -- and that is one rule, which means one place. It sits in the
     * common package rather than the client one because a reading taken by right-clicking a
     * machine is composed on the server, where the true figure is: condition syncs only when the
     * adjective changes, so the client's copy is stale by up to a whole band and is not fit to
     * measure.
     */
    public static Component signed(Instrument instrument, Component figure) {
        return Component.translatable("feedback.readout.attributed", instrument.label(), figure)
                .withStyle(ChatFormatting.AQUA);
    }

    /**
     * Round a true value to what an instrument can actually distinguish.
     * <p>
     * This is the whole of what a tier buys. The world is never consulted differently and the
     * reading is never wrong — it is simply coarser, which is why a poor instrument costs
     * reproducibility rather than success (§8).
     */
    public static float quantise(float value, float resolution) {
        if (resolution <= 0)
            return value;
        return Math.round(value / resolution) * resolution;
    }
}
