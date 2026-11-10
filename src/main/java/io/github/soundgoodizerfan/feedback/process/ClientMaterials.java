// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        materials = java.util.Collections.unmodifiableSortedMap(new TreeMap<>(incoming));
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
        return level.isClientSide ? materials : MaterialTable.get().materials();
    }
}
