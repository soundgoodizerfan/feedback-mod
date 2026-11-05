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
package io.github.soundgoodizerfan.feedback.machine.induction;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.ItemStack;

/** What a changing field can heat: a conductor (a metal charge) or a ferromagnet (cookware). Read from material figures, never item ids. */
final class Induction {

    private Induction() {
    }

    /** Whether the stack's material is a metal. Walks the material table, so callers cache. */
    static boolean isMetal(ItemStack stack) {
        return !stack.isEmpty() && Material.find(MaterialTable.get().materials(), stack)
                .filter(found -> found.material().kind().filter(k -> k == Material.Kind.METAL).isPresent()).isPresent();
    }

    /** Whether the material filed under {@code id} is magnetic (the figure the Electromagnet reads). */
    static boolean isMagnetic(net.minecraft.resources.ResourceLocation id) {
        Material material = id == null ? null : MaterialTable.get().materials().get(id);
        return material != null && material.separation().magnetic();
    }
}
