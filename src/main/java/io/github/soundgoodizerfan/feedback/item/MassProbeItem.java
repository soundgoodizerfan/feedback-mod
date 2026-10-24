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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.Cut;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A crystal plate on a metal backing: a quartz-crystal microbalance (mechanics 6.17). Hung on the
 * side of an electrolytic cell like any plated piece, it takes the coat the cell is laying down,
 * and the coat loads the crystal and lowers its ring -- so the shift is the deposited Qu.
 *
 * <h2>The cut is the instrument tier</h2>
 * A thin cut reads fine and not far; a thick one reads far and coarse. The cut is a component on
 * the plate ({@link Cut}), copied onto the probe when it is crafted; range and step come from it.
 *
 * <h2>Overrun</h2>
 * A coat past the range damps the crystal and the reading sits at the ceiling, exact and wrong,
 * while the coat keeps growing under it. The cell's spall rule is unchanged, so a probe trusted
 * past its range comes out bare. The reading never feeds back into plating (philosophy 8).
 */
public class MassProbeItem extends Item implements Instrument {

    public MassProbeItem(Properties properties) {
        super(properties);
    }

    /** How sound the crystal is, 1 as new. Over-drive in the cell lowers it. */
    public static float condition(ItemStack stack) {
        Float c = stack.get(FDataComponents.PROBE_CONDITION.get());
        return c == null ? 1f : c;
    }

    /**
     * What the probe shows for {@code coat} Qu on it. At or past the range it is the ceiling and
     * nothing else; under it, the coat plus noise, quantised to the step. Noise has a floor that
     * no cut escapes and grows as the crystal cracks.
     */
    public float read(ItemStack probe, float coat, RandomSource random) {
        Cut cut = Cut.of(probe);
        float condition = condition(probe);
        if (coat >= cut.range)
            return cut.range;
        float sigma = cut.step * (FTuning.MASS_PROBE_NOISE_STEPS
                + (1f - condition) / (1f - FTuning.MASS_PROBE_CONDITION_FLOOR) * FTuning.MASS_PROBE_CRACKED_NOISE_STEPS);
        float noisy = coat + (float) random.nextGaussian() * sigma;
        return Math.min(cut.range, Math.max(0f, Instruments.quantise(noisy, cut.step)));
    }

    /** A shown figure, signed by this probe. */
    public Component signed(ItemStack probe, float shown) {
        return Instruments.signed(this, Component.translatable("feedback.readout.deposit",
                Instruments.format(shown, Cut.of(probe).step)));
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.DEPOSIT;
    }

    @Override
    public float resolution(Quantity quantity) {
        // shortcut: Instrument has no stack, so this is the finest cut's step; the reading uses the plate's own cut. Upgrade if a second reader of DEPOSIT appears.
        return Cut.FINE.step;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.mass_probe");
    }
}
