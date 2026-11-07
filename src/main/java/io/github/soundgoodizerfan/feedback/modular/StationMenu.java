// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The part-making benches' menu: {@link ToolTableMenu} on the Parts tab alone (assembly and rework stay at the Tool Table), with
 * its {@link Station} filtering the stock. The stonecutter adds the vanilla cut as a second tab.
 */
public class StationMenu extends ToolTableMenu {

    /** Client-side open: the position comes in the open packet, and the block there says which bench it is. */
    public StationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

    private StationMenu(int id, Inventory inventory, BlockPos pos) {
        super(FMenus.STATION.get(), ToolTableAssembly.Kind.TOOL, id, inventory, new SimpleContainer(ToolTableBlockEntity.SIZE), pos,
                inventory.player.level().getBlockState(pos).getBlock() instanceof StationBlock b ? b.station : Station.TOOL);
    }

    /** Server-side open. */
    public StationMenu(int id, Inventory inventory, StationBlockEntity table, Station station) {
        super(FMenus.STATION.get(), ToolTableAssembly.Kind.TOOL, id, inventory, table, table.getBlockPos(), station);
    }
}
