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
package io.github.soundgoodizerfan.feedback.process;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.TreeMap;

/** The client's copy of the material table -- {@link ClientDeformations}'s own reasoning. */
public final class ClientMaterials {

    private static Map<ResourceLocation, Material> materials = Map.of();

    private ClientMaterials() {
    }

    public static void set(Map<ResourceLocation, Material> incoming) {
        materials = Map.copyOf(incoming);
    }

    /** The materials as {@code level}'s side knows them, sorted by id: the server's table, or this copy. */
    public static Map<ResourceLocation, Material> on(Level level) {
        return level.isClientSide ? new TreeMap<>(materials) : MaterialTable.get().materials();
    }
}
