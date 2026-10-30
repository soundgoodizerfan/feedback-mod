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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/** The radiation ward's trade: the radical scavenger spends the body's repair, so every heal lands at a fraction while it lasts. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RadiationWard {

    private RadiationWard() {}

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        var who = event.getEntity();
        if (!who.level().isClientSide && who.hasEffect(FEffects.RADIATION_WARD))
            event.setAmount(event.getAmount() * PotionTuning.RADIATION_WARD_HEAL);
    }
}
