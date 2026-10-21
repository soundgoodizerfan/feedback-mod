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
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Katana (spec 5.4): the first blow after {@code quiet_ticks} of not attacking deals
 * {@code opening_mult} and opens a bleed of {@code bleed_damage} a second for {@code bleed_seconds}
 * scaled by the edge's BITE (a keener edge bleeds longer). It does not stack. The cost is the
 * patience: every blow, struck or not followed up, restarts the quiet.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class OpeningCut implements ModularTrait {

    private static final WielderState<Long> LAST_SWING = new WielderState<>();

    /** The bleeding, by victim: where it ends and what it costs. The entity is held until it ends. */
    private record Bleed(LivingEntity victim, long end, float damage) {
    }

    private static final Map<UUID, Bleed> BLEEDS = new ConcurrentHashMap<>();

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        long now = attacker.level().getGameTime();
        Long last = LAST_SWING.get(attacker.getUUID());
        LAST_SWING.put(attacker.getUUID(), now);
        if (last != null && now - last < params.get("quiet_ticks", 80f))
            return;
        event.setAmount(event.getAmount() * params.get("opening_mult", 1.8f));
        var figures = ModularStacks.figures(weapon);
        float bite = figures == null ? 1f : figures.get(Fig.BITE);
        long ticks = (long) (params.get("bleed_seconds", 4f) * bite * 20);
        LivingEntity victim = event.getEntity();
        BLEEDS.putIfAbsent(victim.getUUID(), new Bleed(victim, now + ticks, params.get("bleed_damage", 1f)));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (BLEEDS.isEmpty() || event.getServer().getTickCount() % 20 != 0)
            return;
        Iterator<Bleed> it = BLEEDS.values().iterator();
        while (it.hasNext()) {
            Bleed b = it.next();
            if (!b.victim.isAlive() || b.victim.level().getGameTime() >= b.end) {
                BLEEDS.remove(b.victim.getUUID());
                continue;
            }
            b.victim.hurt(b.victim.damageSources().generic(), b.damage);
        }
    }
}
