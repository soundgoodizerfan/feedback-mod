// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
