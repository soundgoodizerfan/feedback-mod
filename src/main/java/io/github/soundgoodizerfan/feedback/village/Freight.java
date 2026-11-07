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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Freight's clock: on a market day's morning every linked depot sends its convoy, and the couriers that left in view are taken off the road. */
public final class Freight {

    private Freight() {
    }

    /** Couriers on the road and the game tick each is removed at. Runtime only: a courier is a sight, not state. */
    private static final Map<UUID, Long> COURIERS = new HashMap<>();

    static void courier(Entity donkey, long until) {
        COURIERS.put(donkey.getUUID(), until);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        if (level.getDayTime() % VillageTuning.DAY_TICKS == 1500L) {
            long day = level.getDayTime() / VillageTuning.DAY_TICKS;
            for (DepotBlockEntity depot : new ArrayList<>(DepotBlockEntity.INDEX.in(level)))
                if (!depot.isRemoved())
                    depot.depart(level, day);
        }
        if (!COURIERS.isEmpty() && level.getGameTime() % 20 == 0) {
            var it = COURIERS.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                Entity donkey = level.getEntity(e.getKey());
                if (donkey == null) {
                    if (e.getValue() < level.getGameTime())
                        it.remove();
                } else if (e.getValue() < level.getGameTime()) {
                    donkey.discard();
                    it.remove();
                }
            }
        }
    }
}
