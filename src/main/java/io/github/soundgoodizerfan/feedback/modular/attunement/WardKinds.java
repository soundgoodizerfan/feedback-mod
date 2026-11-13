// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import net.minecraft.world.item.ItemStack;

/**
 * The runtime kinds of the Ward effect. Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
public final class WardKinds {

    private WardKinds() {
    }

    /**
     * What a use costs a Ward-soaked tool: each point of wear is turned away with chance {@code n / (n + 1)}, {@code n}
     * being the Ward levels across its parts (Unbreaking's odds). Called from {@link ModularStacks#cap}, so it only
     * ever shortens wear a use or a misuse already caused.
     */
    public static int soak(ItemStack stack, int amount) {
        int n = levels(stack);
        if (n == 0)
            return amount;
        int kept = 0;
        for (int i = 0; i < amount; i++)
            if (java.util.concurrent.ThreadLocalRandom.current().nextInt(n + 1) == 0)
                kept++;
        return kept;
    }

    /** The Ward levels across {@code stack}'s parts. A gun's bore wear is divided by {@code levels + 1}, the same odds as a tool's. */
    public static int levels(ItemStack stack) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals("soak"))
                n += a.level();
        return n;
    }

    /**
     * The share of a machine's wear that gets through its soaking parts: {@code 1 / (levels + 1)}, the mean of {@link #soak}'s odds,
     * for wear that is a fraction of condition and not whole points. {@code design} is what the machine was built from (null: none).
     * Misuse still wears; this only turns some of it away.
     */
    public static float wearShare(io.github.soundgoodizerfan.feedback.modular.type.ModularDesign design) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(design))
            if (a.kind().equals("soak"))
                n += a.level();
        return 1f / (n + 1);
    }

    public static void register() {
    }
}
