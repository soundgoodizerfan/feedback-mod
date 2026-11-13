// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.EventTrigger;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Advancement bookkeeping for Read the draw: a player who owns it and had a creature aim at them (the same test the
 * cue on their screen uses, with a clear line of sight) for a moment, and was not hurt by that creature by the time it stopped, has read the
 * draw and got clear. The cue itself is client-only; this is the server-side witness.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class DrawDodge {

    private static final double RANGE = 48;
    private static final int MIN_AIM = 10;
    /** Player id -> (shooter id -> game tick the aim began). */
    private static final Map<UUID, Map<UUID, Long>> AIMS = new HashMap<>();

    private DrawDodge() {
    }

    private static boolean aiming(LivingEntity e, ServerPlayer at) {
        if (!at.hasLineOfSight(e))
            return false;
        if (e.isUsingItem() && e.getUseItem().getItem() instanceof ProjectileWeaponItem)
            return e instanceof Mob m && m.getTarget() == at;
        return e instanceof Mob m && m.isAggressive() && m.getTarget() == at && e.getMainHandItem().getItem() instanceof GunItem;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0)
            return;
        Map<UUID, Long> mine = AIMS.get(p.getUUID());
        if (!Skills.unlocked(p, "read_the_draw")) {
            if (mine != null)
                AIMS.remove(p.getUUID());
            return;
        }
        long now = p.level().getGameTime();
        Map<UUID, Long> seen = new HashMap<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE), e -> e != p && e.isAlive() && aiming(e, p)))
            seen.put(e.getUUID(), mine != null && mine.containsKey(e.getUUID()) ? mine.get(e.getUUID()) : now);
        if (mine != null)
            for (var was : mine.entrySet()) {
                if (seen.containsKey(was.getKey()) || now - was.getValue() < MIN_AIM)
                    continue;
                // It stopped aiming: unhurt by anything since it began means the shot missed or never came.
                if (p.getLastHurtByMob() == null || p.getLastHurtByMobTimestamp() < p.tickCount - (int) (now - was.getValue()))
                    EventTrigger.fire(p, "progress/combat/read_the_draw");
            }
        if (seen.isEmpty())
            AIMS.remove(p.getUUID());
        else
            AIMS.put(p.getUUID(), seen);
    }
}
