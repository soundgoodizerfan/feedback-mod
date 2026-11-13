// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.prep;

import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.modular.bench.ModularAssembly;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Prep Table's menu: {@link PartBenchMenu} for dishes. Parts shapes the containers (bowl, plate, skewer) with the
 * knife; Assemble builds the dish type the picker names; the third tab is Prepare, the Cut minigame's place. A knife takes the hammer's slot and loses a point per container or dish; there is no rework (a dish is
 * eaten, not taken apart) and its plan slot is the seasoning. Each assembly slot holds one food, since a role's count here is one.
 */
public class PrepTableMenu extends PartBenchMenu {

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public PrepTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(FMenus.PREP_TABLE.get(), ModularAssembly.Kind.DISH, id, inventory, new SimpleContainer(PartBenchBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public PrepTableMenu(int id, Inventory inventory, PrepTableBlockEntity table) {
        super(FMenus.PREP_TABLE.get(), ModularAssembly.Kind.DISH, id, inventory, table, table.getBlockPos());
    }
}
