// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Which of Feedback's menus are storage and which are roles: the one declaration every sorter adapter reads.
 *
 * <p>A sort rewrites every slot of a container by item, so a screen whose slot positions are roles (an input here, a fuel there, a result in the
 * corner, a recipe grid) must be left alone. Storage screens (packs, depots) stay sortable. A menu is a role menu unless it is named in
 * {@link #STORAGE}, so a new screen is safe by default.
 *
 * <p>Prior art: Inventory Profiles Next (AGPL-3.0), read for design only with the author's permission, nothing copied. Taken: a per-menu
 * declaration of storage against everything else, and a thin adapter per sorter over it. Rejected: its default (an unknown menu is sortable
 * storage; ours is the reverse), class-name JSON as the primary mechanism (here it is only generated output) and its hint table for other mods.
 */
public final class MenuRoles {

    /** Storage menus: registered id to menu class (binary name). Everything else Feedback registers is a role menu. */
    public static final Map<String, String> STORAGE = Map.of(
            "pack", "io.github.soundgoodizerfan.feedback.modular.part.PackMenu",
            "depot", "io.github.soundgoodizerfan.feedback.village.DepotMenu");

    private MenuRoles() {
    }

    /** The registered ids of the role menus. */
    public static List<ResourceLocation> roleIds() {
        return FMenus.MENUS.getEntries().stream().map(h -> h.getId()).filter(id -> !STORAGE.containsKey(id.getPath())).toList();
    }

    /**
     * The classes of every menu in this mod's jar (a class that, through the mod's own classes, extends a container menu, chest menu
     * included) that is not a storage menu, as binary class names: found by the mod file's class scan, so a new menu class is a role menu
     * without being listed anywhere. Abstract bases are listed too; a hint for one is harmless.
     */
    public static List<String> roleClasses() {
        var file = ModList.get().getModFileById(Feedback.MOD_ID);
        if (file == null)
            return List.of();
        Map<String, String> parent = new HashMap<>();
        file.getFile().getScanResult().getClasses().forEach(c -> parent.put(c.clazz().getClassName(), c.parent().getClassName()));
        Set<String> roots = Set.of("net.minecraft.world.inventory.AbstractContainerMenu", "net.minecraft.world.inventory.ChestMenu");
        Set<String> out = new HashSet<>();
        for (String name : parent.keySet()) {
            String up = parent.get(name);
            for (int depth = 0; up != null && depth < 16; depth++) {
                if (roots.contains(up)) {
                    if (!STORAGE.containsValue(name))
                        out.add(name);
                    break;
                }
                up = parent.get(up);
            }
        }
        return out.stream().sorted().toList();
    }
}
