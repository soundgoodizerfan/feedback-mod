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
import io.github.soundgoodizerfan.feedback.process.Circuit;

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
        int steps = io.github.soundgoodizerfan.feedback.combat.skill.Skills.fineEyeSteps(player);
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
        // Fine eye: the trained eye halves the step per bought node; the truth tier (0) stays 0.
        if (best != null && steps != 0)
            best = new Keen(best, (float) Math.pow(0.5, steps));
        float lens = lensFactor(inventory);
        return best == null || lens >= 1 ? best : new Lensed(best, lens);
    }

    /** What reads by looking: a lens helps with these and nothing else. */
    private static boolean optical(Quantity quantity) {
        return quantity == Quantity.LENGTH || quantity == Quantity.CONDITION
                || quantity == Quantity.HARDNESS || quantity == Quantity.ORE;
    }

    /**
     * The step factor of the clearest lens carried: a clearer glass (lower reflectivity) narrows the
     * step more, 0.65 for the clearest and 1 (no help) for a murky one. Found by the lens form of a
     * glass material, not by which glass it is.
     */
    private static float lensFactor(Inventory inventory) {
        float best = 1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!(inventory.getItem(slot).getItem() instanceof io.github.soundgoodizerfan.feedback.item.FormItem form)
                    || !form.form().equals("lens"))
                continue;
            var material = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(form.material());
            if (material != null)
                best = Math.min(best, Math.min(1f, 0.5f + 5f * material.separation().reflectivity().orElse(0.2f)));
        }
        return best;
    }

    /**
     * What the player reads with: {@link #best}, or, under the Assay effect, the truth tier. For displays only
     * (readouts, Jade); anything an instrument <em>does</em> besides reading still calls {@link #best}, so the
     * potion is a consumable look and never a key (instrument is a yield technology). The world never sees it.
     */
    @Nullable
    public static Instrument reading(@Nullable Player player, Quantity quantity) {
        Instrument best = best(player, quantity);
        if (player != null && player.hasEffect(io.github.soundgoodizerfan.feedback.registry.FEffects.ASSAY)
                && (best == null || best.resolution(quantity) > Instrument.TRUTH))
            return ASSAYED;
        return best;
    }

    private static final Instrument ASSAYED = new Instrument() {
        @Override
        public boolean canRead(Quantity quantity) {
            return true;
        }

        @Override
        public float resolution(Quantity quantity) {
            return Instrument.TRUTH;
        }

        @Override
        public Component label() {
            return Component.translatable("feedback.instrument.assay");
        }
    };

    @Nullable
    private static Instrument finer(@Nullable Instrument best, ItemStack stack, Quantity quantity) {
        if (!(stack.getItem() instanceof Instrument item) || !item.canRead(quantity))
            return best;
        Instrument candidate = fitted(stack, item);
        return best == null || candidate.resolution(quantity) < best.resolution(quantity) ? candidate : best;
    }

    /**
     * The instrument as this stack is: a fitted {@link Circuit} (put there by {@code InstrumentFitRecipe})
     * narrows every step by its {@link Circuit#stepFactor()}, and the label says so. Anything reading
     * through a stack -- carried or held -- goes through this, so resolution is looked up in one place.
     */
    public static Instrument fitted(ItemStack stack, Instrument item) {
        Circuit circuit = Circuit.of(stack);
        return circuit.live() ? new Fitted(item, circuit.stepFactor()) : item;
    }

    private record Fitted(Instrument base, float factor) implements Instrument {
        @Override
        public boolean canRead(Quantity quantity) {
            return base.canRead(quantity);
        }

        @Override
        public float resolution(Quantity quantity) {
            return base.resolution(quantity) * factor;
        }

        @Override
        public Component label() {
            return Component.translatable("feedback.instrument.fitted", base.label());
        }
    }

    /** An instrument in a trained eye's hands: the same device and label, a finer step. */
    private record Keen(Instrument base, float factor) implements Instrument {
        @Override
        public boolean canRead(Quantity quantity) {
            return base.canRead(quantity);
        }

        @Override
        public float resolution(Quantity quantity) {
            return base.resolution(quantity) * factor;
        }

        @Override
        public Component label() {
            return base.label();
        }
    }

    /** An instrument read through a lens: a finer step for the quantities the eye reads, the same device and label. */
    private record Lensed(Instrument base, float factor) implements Instrument {
        @Override
        public boolean canRead(Quantity quantity) {
            return base.canRead(quantity);
        }

        @Override
        public float resolution(Quantity quantity) {
            return base.resolution(quantity) * (optical(quantity) ? factor : 1);
        }

        @Override
        public Component label() {
            return base.label();
        }
    }

    /** Whether the player reads this quantity at the truth tier ({@link Instrument#TRUTH}). */
    public static boolean exact(@Nullable Player player, Quantity quantity) {
        Instrument best = reading(player, quantity);
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
