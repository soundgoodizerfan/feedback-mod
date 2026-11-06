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
package io.github.soundgoodizerfan.feedback.control;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Redstone always works on a {@link Switchable} (author ruling, 2026-11-05): when a vanilla redstone signal reaching a block that holds a
 * switchable rises, the switchable engages; when it falls, it disengages. No machine needs code for it and no machine can opt out.
 * Only the edge acts, so a controller that drives the same switchable is never overridden by an unrelated neighbour update.
 */
@EventBusSubscriber(modid = "feedback")
public final class RedstoneDrive {

    /** The last signal seen at each switchable that was ever powered, so only a change acts. */
    private static final Map<GlobalPos, Boolean> LAST = new ConcurrentHashMap<>();

    private RedstoneDrive() {
    }

    @SubscribeEvent
    public static void onNeighbour(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof Switchable target))
            return;
        // A block that makes redstone is the source, not a receiver of its own signal.
        if (level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.redstone.RedstoneEmitterBlockEntity)
            return;
        boolean powered = level.hasNeighborSignal(pos);
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        Boolean was = LAST.get(key);
        if (was == null && !powered)
            return;
        if (was != null && was == powered)
            return;
        LAST.put(key, powered);
        if (!powered)
            LAST.remove(key);
        target.setEngaged(powered);
    }
}
