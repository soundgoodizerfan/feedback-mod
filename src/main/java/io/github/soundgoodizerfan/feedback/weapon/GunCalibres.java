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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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

    /** The classes, smallest bore first; the reference 9 mm when the gun type is not loaded. */
    public static List<Calibre> classes() {
        ModularType gun = ModularTypes.known().get(Feedback.id("gun"));
        ModularType.Role barrel = gun == null ? null : gun.roles().get("B");
        List<Calibre> out = barrel == null || barrel.part().isEmpty() ? List.of()
                : barrel.part().get().variants().stream()
                .filter(v -> v.params().containsKey("bore"))
                .map(v -> new Calibre(v.name(), Math.round(v.params().get("bore")))).sorted(Comparator.comparingInt(Calibre::boreMm)).toList();
        return out.isEmpty() ? List.of(new Calibre("calibre_pistol", 9)) : out;
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
