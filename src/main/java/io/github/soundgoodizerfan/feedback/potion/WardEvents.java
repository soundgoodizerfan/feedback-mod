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

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Where wards cut a hazard that has no code of the mod's own to hook. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WardEvents {

    private WardEvents() {}

    /** Vanilla poison ticks as sourceless magic damage on a poisoned body; every poison source (fumes, food, stings) lands here. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        var who = event.getEntity();
        if (event.getSource().is(DamageTypes.MAGIC) && event.getSource().getEntity() == null && who.hasEffect(MobEffects.POISON))
            event.setAmount(event.getAmount() * Ward.passes(who, Ward.Hazard.POISON));
    }
}
