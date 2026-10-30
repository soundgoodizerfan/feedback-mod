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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A player's running dose (mechanics 9.8): each {@link FTuning#RAD_INTERVAL_TICKS} the rate they
 * stand in, through their armour, is added; {@link FTuning#RAD_CLEARANCE} a second is cleared. A
 * body complains in three steps -- hunger, then weakness and mining fatigue, then the wither --
 * and every step is a consequence, never a lock. Dose is kept across logging out, lost on death.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RadiationEvents {

    private RadiationEvents() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % FTuning.RAD_INTERVAL_TICKS != 0
                || player.isSpectator() || player.isCreative())
            return;
        var materials = MaterialTable.get().materials();
        float seconds = FTuning.RAD_INTERVAL_TICKS / 20f;
        float dose = player.getData(FCombat.RADIATION_DOSE);
        float taken = Radiation.rate(player, materials) * Radiation.shield(player, materials) * seconds;
        float next = Math.max(0f, dose + taken - FTuning.RAD_CLEARANCE * io.github.soundgoodizerfan.feedback.combat.skill.Skills.radiationClearance(player) * seconds);
        if (next != dose)
            player.setData(FCombat.RADIATION_DOSE, next);
        int ticks = FTuning.RAD_INTERVAL_TICKS * 3;
        if (next >= FTuning.RAD_SICK_DOSE) {
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.hazardSurvived(player, "radiation");
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 0, false, false));
        }
        if (next >= FTuning.RAD_ILL_DOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 0, false, false));
        }
        if (next >= FTuning.RAD_LETHAL_DOSE)
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks, 0, false, false));
    }
}
