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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.parry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Last Stand (skill {@code last_stand}, spec 4.7): a hit that leaves the fighter at or under
 * {@link ParryTuning#LAST_STAND_HEALTH} hit points arms the technique for {@link ParryTuning#LAST_STAND_TICKS};
 * the next roll that swallows a blow then counts as a parry ({@link Parrying#freeParry}). "Low health" is a
 * named line, not a scalar that scales the effect. Its rearm is the clock ({@code TIME}), so it cannot be
 * farmed by standing at the line.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class LastStand {

    /** Game tick each fighter's armed window closes. Server only; stale entries are compared to the clock. */
    private static final Map<UUID, Long> ARMED = new HashMap<>();

    private LastStand() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0f
                || p.getHealth() > ParryTuning.LAST_STAND_HEALTH || !Skills.unlocked(p, "last_stand")
                || !Rearms.ready(p, "last_stand"))
            return;
        ARMED.put(p.getUUID(), p.level().getGameTime() + ParryTuning.LAST_STAND_TICKS);
    }

    /** A roll's i-frames just swallowed {@code source}: if armed, it was a parry. */
    public static void dodged(LivingEntity who, DamageSource source, float amount) {
        Long until = ARMED.get(who.getUUID());
        if (until == null || who.level().getGameTime() > until || !Rearms.ready(who, "last_stand"))
            return;
        ARMED.remove(who.getUUID());
        Rearms.spend(who, "last_stand");
        Parrying.freeParry(who, source, amount);
    }
}
