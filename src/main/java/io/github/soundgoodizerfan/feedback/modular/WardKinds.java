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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.world.item.ItemStack;

/**
 * The runtime kinds of the Ward effect ({@code docs/feedback_enchant_roster.md}). Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
final class WardKinds {

    private WardKinds() {
    }

    /**
     * What a use costs a Ward-soaked tool: each point of wear is turned away with chance {@code n / (n + 1)}, {@code n}
     * being the Ward levels across its parts (Unbreaking's odds). Called from {@link ModularStacks#cap}, so it only
     * ever shortens wear a use or a misuse already caused.
     */
    static int soak(ItemStack stack, int amount) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals("soak"))
                n += a.level();
        if (n == 0)
            return amount;
        int kept = 0;
        for (int i = 0; i < amount; i++)
            if (java.util.concurrent.ThreadLocalRandom.current().nextInt(n + 1) == 0)
                kept++;
        return kept;
    }

    static void register() {
    }
}
