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
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * What a gripping material can stand, read from its file.
 *
 * <h2>One figure, and no per-item rating</h2
 * A glove or a pair of tongs has no "protects to 600 Tu" stamped on it. It has a material, and
 * the material has a temperature it holds to ({@code service_temperature}, else the point it
 * burns at, {@code oxidation_temperature}). How hot a piece may be before that material is
 * hurt follows from how much of the piece's excess over the room reaches the grip -- the item's
 * exposure -- so a thicker glove or a longer arm is a physical change that moves the answer, and
 * a better grip material moves it without anything being re-tuned.
 */
public final class Grip {

    private Grip() {
    }

    /** The material's file, as this side of the connection knows it. */
    public static Optional<Material> material(Level level, ResourceLocation id) {
        return Optional.ofNullable(ClientMaterials.on(level).get(id));
    }

    /**
     * Whether it keeps heat off the hand at all. A figure for conductivity decides it when the
     * file has one; where nobody measured, an organic is taken to insulate and anything else is
     * not. A property again, never a name: a metal grip is not one, whichever metal.
     */
    public static boolean insulates(Material material) {
        return material.thermal().conductivity()
                .map(c -> c.workPerTickPerTu() < FTuning.GRIP_MAX_CONDUCTIVITY)
                .orElseGet(() -> material.kind().map(k -> k == Material.Kind.ORGANIC).orElse(false));
    }

    /** The hottest this material may run before it is hurt, if its file says. */
    public static Optional<Float> stands(Material material) {
        return material.thermal().serviceTemperature().or(() -> material.thermal().oxidationTemperature())
                .map(tu -> tu.value());
    }

    /**
     * The hottest piece, in Tu, a hand may hold through this material at this exposure: the
     * temperature at which {@code ambient + exposure x (T - ambient)} just reaches what the
     * material stands. A material that does not insulate, or has no figure, holds nothing back.
     */
    public static float safeUpTo(Level level, ResourceLocation id, float exposure) {
        float floor = FTuning.CONTACT_PAIN_TU.value();
        Optional<Material> material = material(level, id);
        if (material.isEmpty() || !insulates(material.get()))
            return floor;
        Optional<Float> stands = stands(material.get());
        if (stands.isEmpty())
            return floor;
        float ambient = AmbientTable.at(level).value();
        if (stands.get() <= ambient)
            return floor;
        return Math.max(floor, ambient + (stands.get() - ambient) / exposure);
    }
}
