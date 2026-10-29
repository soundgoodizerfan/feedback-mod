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
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The billhook's hooked cut. A blow on a mob opens a bleed of {@code bleed_damage} a second for
 * {@code bleed_seconds}; it does not stack. Cost: the blow itself lands at {@code hit_mult}, so
 * the bleed has to do the work.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Billhook implements ModularTrait {

    private record Bleed(LivingEntity victim, long end, float damage) {
    }

    private static final Map<UUID, Bleed> BLEEDS = new ConcurrentHashMap<>();

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        event.setAmount(event.getAmount() * params.get("hit_mult", 0.8f));
        open(event.getEntity(), params.get("bleed_seconds", 5f), params.get("bleed_damage", 1f));
    }

    /** Opens a bleed on {@code victim}, unless one is already running (claws on a glove use it too). */
    public static void open(LivingEntity victim, float seconds, float damage) {
        BLEEDS.putIfAbsent(victim.getUUID(), new Bleed(victim, victim.level().getGameTime() + (long) (seconds * 20), damage));
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
