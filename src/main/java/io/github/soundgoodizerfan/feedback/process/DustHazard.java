// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Fumes;

import net.minecraft.world.item.ItemStack;

/**
 * What a fine form costs for being fine (ore processing spec 2.4): dust runs a row sooner ({@link FormType#holdFactor}), so it must
 * be worse to hold or there is no choice to make. Both costs grow with fineness, {@code 1/grain - 1}, which is 0 for an ingot.
 * Every figure is an invented placeholder in {@link FTuning}.
 *
 * <ul>
 *   <li><b>Burn-off.</b> A metal that melts in air above its {@code oxidation_temperature} scales: a share of a fine form's melt
 *       goes to oxide. A sealed vessel loses none. Dust through a roast is fast; dust through a bare-air melt is lossy.</li>
 *   <li><b>Flash.</b> A fine combustible non-metal past its ignition point goes up and lights the air above it ({@link #ignites}).</li>
 *   <li><b>Fume.</b> A fine form of a toxic material throws up its fume when a batch of it finishes open to the air.</li>
 * </ul>
 */
public final class DustHazard {

    private DustHazard() {
    }

    /** {@code 1/grain - 1}: how much finer than an ingot the stack is, 0 for anything as coarse or coarser. */
    public static float fineness(ItemStack stack) {
        return Math.max(0f, 1f / FormType.grainOf(stack) - 1f);
    }

    /** The share (0 to {@link FTuning#DUST_BURNOFF_MAX}) of a melt of {@code stack} lost to oxide at {@code temperature} Tu. */
    public static float burnOff(ItemStack stack, float temperature, boolean airless) {
        float fineness = fineness(stack);
        if (airless || fineness <= 0f)
            return 0f;
        var located = MaterialIngredient.locate(stack.getItem());
        if (located.isEmpty() || located.get().material().kind().filter(k -> k == Material.Kind.METAL).isEmpty())
            return 0f;
        var oxidises = located.get().material().thermal().oxidationTemperature();
        if (oxidises.isEmpty() || temperature <= oxidises.get().value())
            return 0f;
        return Math.min(FTuning.DUST_BURNOFF_MAX, fineness * FTuning.DUST_BURNOFF_PER_FINENESS);
    }

    /**
     * Whether a fine form of a combustible non-metal (coal, sulfur, black powder) held in an open vessel at {@code temperature} Tu is past
     * its {@code oxidation_temperature}, the material's own ignition point: it flashes. A metal only scales ({@link #burnOff}).
     */
    public static boolean ignites(ItemStack stack, float temperature, boolean airless) {
        if (airless || fineness(stack) <= 0f)
            return false;
        var located = MaterialIngredient.locate(stack.getItem());
        if (located.isEmpty())
            return false;
        Material m = located.get().material();
        return m.combustion().isPresent() && m.kind().filter(k -> k != Material.Kind.METAL).isPresent()
                && m.thermal().oxidationTemperature().filter(t -> temperature >= t.value()).isPresent();
    }

    /** The fume level (as {@link Fumes#level}) a batch of {@code stack} throws up open to the air; 0 for a coarse or harmless one. */
    public static int fume(ItemStack stack, boolean airless) {
        float fineness = fineness(stack);
        if (airless || fineness <= 0f)
            return 0;
        var located = MaterialIngredient.locate(stack.getItem());
        if (located.isEmpty())
            return 0;
        return Math.round(Fumes.level(located.get().material()) * Math.min(1f, fineness * FTuning.DUST_FUME_PER_FINENESS));
    }
}
