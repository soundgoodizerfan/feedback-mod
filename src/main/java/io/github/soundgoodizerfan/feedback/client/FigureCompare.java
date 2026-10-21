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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Two sets of figures side by side, for swapping or re-forging a part (spec 9: Truly Modular's
 * before/after comparison, idea only). Figures that changed are grouped raised and lowered, the
 * largest change first, and printed at the truth tier; the caller decides whether the player may
 * read figures at all (paper may, an item needs the truth tier). It judges nothing better or worse:
 * a lighter piece swings quicker and hits softer, and which is wanted is the player's.
 */
public final class FigureCompare {

    /** Significant figures printed: a comparison reads at a glance, the exact figure is on the item. */
    private static final int SHOWN = 4;

    private FigureCompare() {
    }

    /** The changes from {@code before} to {@code after}; empty if nothing moved. */
    public static List<Component> lines(ModularFigures before, ModularFigures after) {
        List<Fig> changed = new ArrayList<>();
        for (Fig fig : Fig.values())
            if (before.get(fig) != after.get(fig))
                changed.add(fig);
        changed.sort(Comparator.comparingDouble((Fig f) -> -Math.abs(share(before.get(f), after.get(f)))));
        List<Component> out = new ArrayList<>();
        for (boolean raised : new boolean[]{true, false}) {
            List<Fig> group = changed.stream().filter(f -> (after.get(f) > before.get(f)) == raised).toList();
            if (group.isEmpty())
                continue;
            ChatFormatting colour = raised ? ChatFormatting.GREEN : ChatFormatting.RED;
            out.add(Component.translatable(raised ? "feedback.compare.raised" : "feedback.compare.lowered")
                    .withStyle(colour));
            for (Fig fig : group)
                out.add(line(fig, before.get(fig), after.get(fig)).copy().withStyle(colour));
        }
        return out;
    }

    /** One figure's change: {@code Mass: 3.2 to 4.1 (+28%)}. */
    public static Component line(Fig fig, float before, float after) {
        Component label = Component.translatable("modular.feedback.fig." + fig.name().toLowerCase(Locale.ROOT));
        label = Component.literal(label.getString().split(",", 2)[0]);
        String was = Instruments.sig(before, SHOWN), now = Instruments.sig(after, SHOWN);
        if (before == 0 || after == 0)
            return Component.translatable("feedback.compare.line_bare", label, was, now);
        float pct = share(before, after) * 100;
        return Component.translatable("feedback.compare.line", label, was, now,
                change(pct));
    }

    /** A percentage to one decimal, signed; a change under a twentieth of a percent reads as none. */
    private static String change(float pct) {
        String text = Readout.number(Math.abs(pct) < 0.05f ? 0 : pct);
        return pct > 0 && !text.equals("0") ? "+" + text : text;
    }

    /** The change as a share of the old figure, signed. */
    private static float share(float before, float after) {
        return before == 0 ? (after == 0 ? 0 : Math.signum(after) * Float.MAX_VALUE) : (after - before) / Math.abs(before);
    }
}
