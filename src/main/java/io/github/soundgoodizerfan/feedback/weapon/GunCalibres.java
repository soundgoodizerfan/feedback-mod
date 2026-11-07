// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import java.util.Comparator;
import java.util.List;

/**
 * The calibre classes (spec 8.3): bore is a short list of named classes, not a free number (philosophy 3). They are
 * the gun type's barrel variants, each with its {@code bore} in whole millimetres, read from data so a pack adds a
 * class by adding a variant. A round is seated for one class's bore and a barrel is made for one; how loosely the
 * round sits in the bore the lathe and drill really left is still in the figures ({@link GunSpec}).
 */
public final class GunCalibres {

    private GunCalibres() {
    }

    /** One class: its name ({@code modular.feedback.variant.<name>}) and its bore. */
    public record Calibre(String name, int boreMm) {
    }

    private static final List<Calibre> CLASSES = List.of(new Calibre("calibre_rimfire", 5), new Calibre("calibre_pistol", 9),
            new Calibre("calibre_intermediate", 12), new Calibre("calibre_magnum", 20));

    /** The named classes, smallest bore first. A bore is any whole number of mm; these are the names it is called by, the nearest class. */
    public static List<Calibre> classes() {
        return CLASSES;
    }

    /** The class whose bore is nearest {@code boreMm}. */
    public static Calibre nearest(int boreMm) {
        Calibre best = null;
        for (Calibre c : classes())
            if (best == null || Math.abs(c.boreMm() - boreMm) < Math.abs(best.boreMm() - boreMm))
                best = c;
        return best;
    }

    /** The class one step up ({@code +1}) or down from the one nearest {@code boreMm}, held at the ends. */
    public static Calibre step(int boreMm, int by) {
        List<Calibre> all = classes();
        return all.get(Math.max(0, Math.min(all.size() - 1, all.indexOf(nearest(boreMm)) + by)));
    }
}
