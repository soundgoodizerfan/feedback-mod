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
package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The stamina meter, the one meter in the combat kit (user ruling 2026-10-24): casts spend it, and it comes back
 * once the fighter has gone {@link #DELAY_TICKS} without spending, paid for in hunger as it refills. Exact value server-side; the attachment holds
 * whole points, written only when they change.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Stamina {

    public static final int MAX = 100;
    /** Ticks after a spend before it refills, and points regained per tick after that. */
    public static final int DELAY_TICKS = 25;
    public static final float REGEN = 0.6f;
    /** STAND-IN: below this the bar counts as low, and Adrenaline speeds its regen. */
    public static final float LOW = 30f;
    /** Hunger paid per point regained, vanilla exhaustion (4 is one hunger point): a full bar refilled costs 3. */
    public static final float EXHAUSTION_PER_POINT = 0.03f;

    private record Level(float value, long lastSpent) {
    }

    private static final Map<UUID, Level> LEVELS = new HashMap<>();

    private Stamina() {
    }

    public static float get(ServerPlayer p) {
        Level l = LEVELS.get(p.getUUID());
        return l == null ? CastState.of(p).stamina() : l.value();
    }

    /** Takes {@code cost} if there is that much. @return whether it did. */
    public static boolean spend(ServerPlayer p, float cost) {
        cost = io.github.soundgoodizerfan.feedback.potion.Adrenaline.cost(p, cost);
        float have = get(p);
        if (have < cost)
            return false;
        put(p, have - cost, p.level().getGameTime());
        return true;
    }

    /** Refills stamina to {@code to} (capped at {@link #MAX}), as a rest does: the regen pause starts again. */
    public static void restore(ServerPlayer p, float to) {
        put(p, Math.max(get(p), Math.min(MAX, to)), p.level().getGameTime());
    }

    /** Adds {@code amount} (capped at {@link #MAX}) without pausing regen: a gift, not a rest. */
    public static void gain(ServerPlayer p, float amount) {
        Level l = LEVELS.get(p.getUUID());
        put(p, Math.min(MAX, get(p) + amount), l == null ? 0 : l.lastSpent());
    }

    private static void put(ServerPlayer p, float value, long lastSpent) {
        LEVELS.put(p.getUUID(), new Level(value, lastSpent));
        int whole = Math.round(value);
        CastState s = CastState.of(p);
        if (s.stamina() != whole)
            CastState.set(p, s.withStamina(whole));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Level l = LEVELS.get(p.getUUID());
        if (l == null) {
            LEVELS.put(p.getUUID(), new Level(CastState.of(p).stamina(), 0));
            return;
        }
        if (l.value() < MAX && p.level().getGameTime() - l.lastSpent() > io.github.soundgoodizerfan.feedback.potion.SecondWind.delay(p, DELAY_TICKS)) {
            float regen = (l.value() < LOW ? REGEN * Skills.lowStaminaRegen(p) : REGEN) * (1f + io.github.soundgoodizerfan.feedback.process.FoodPerk.trait(p, "stamina_regen"))
                    * io.github.soundgoodizerfan.feedback.potion.SecondWind.regenFactor(p)
                    * io.github.soundgoodizerfan.feedback.potion.Ward.regenFactor(p);
            float to = Math.min(MAX, l.value() + regen);
            p.causeFoodExhaustion((to - l.value()) * EXHAUSTION_PER_POINT);
            put(p, to, l.lastSpent());
        }
    }
}
