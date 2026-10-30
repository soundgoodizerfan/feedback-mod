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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Adrenaline. While it lasts, cast techniques cost a fraction of their stamina ({@code combat/cast/Stamina.spend} asks here) and the body
 * does not heal: regeneration, a full belly and any other heal on the player are refused. No second meter; it only bends the one stamina bar's cost.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Adrenaline {

    private Adrenaline() {}

    /** The stamina a cast really costs: scaled down while adrenaline lasts. */
    public static float cost(Player p, float base) {
        return p.hasEffect(FEffects.ADRENALINE) ? base * PotionTuning.ADRENALINE_COST : base;
    }

    /** No healing of any kind on a player running on adrenaline (server side; the heal is cancelled before it lands). */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && p.hasEffect(FEffects.ADRENALINE))
            event.setCanceled(true);
    }
}
