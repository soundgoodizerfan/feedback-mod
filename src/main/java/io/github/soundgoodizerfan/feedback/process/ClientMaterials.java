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

    private static Runnable listener = () -> {
    };

    public static void set(Map<ResourceLocation, Material> incoming) {
        materials = Map.copyOf(incoming);
        listener.run();
    }

    /** The table as last received, for a recipe browser; empty until the server has sent it. */
    public static Map<ResourceLocation, Material> get() {
        return materials;
    }

    /** Called whenever the table is replaced. One listener is enough; only JEI wants this. */
    public static void onChanged(Runnable onChanged) {
        listener = onChanged;
        onChanged.run();
    }

    /** The materials as {@code level}'s side knows them, sorted by id: the server's table, or this copy. */
    public static Map<ResourceLocation, Material> on(Level level) {
        return level.isClientSide ? new TreeMap<>(materials) : MaterialTable.get().materials();
    }
}
