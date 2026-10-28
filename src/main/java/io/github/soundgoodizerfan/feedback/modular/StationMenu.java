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
