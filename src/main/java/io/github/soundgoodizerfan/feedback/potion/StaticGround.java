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
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.modular.Attunements;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Static ground: the shock cut is a {@link Ward} entry ({@code Shock.apply}); this is its trade. A grounded body is a conductor, so metal in
 * either hand warms, Tu a pass, more than the pass interval cools, up to a ceiling, through the ordinary item heat stamp (which then cools as any piece does). Metal is the
 * material's kind, never an item list. One pass per holder every {@link PotionTuning#STATIC_GROUND_EVERY} ticks, two slots.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class StaticGround {

    private StaticGround() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || p.tickCount % PotionTuning.STATIC_GROUND_EVERY != 0 || !p.hasEffect(FEffects.STATIC_GROUND))
            return;
        warm(p.getMainHandItem(), level);
        warm(p.getOffhandItem(), level);
    }

    private static void warm(ItemStack s, ServerLevel level) {
        if (s.isEmpty() || Material.find(MaterialTable.get().materials(), s).map(f -> f.material().kind().orElse(null)).orElse(null) != Material.Kind.METAL)
            return;
        float t = ItemHeat.get(s, level).value();
        if (t >= PotionTuning.STATIC_GROUND_CAP)
            return;
        float next = Math.min(PotionTuning.STATIC_GROUND_CAP, t + PotionTuning.STATIC_GROUND_GAIN);
        // ItemHeat.set drops a stamp at or under WARM_TU; a warmed-but-not-hot piece keeps its stamp so the gain accumulates.
        s.set(FDataComponents.TEMPERATURE.get(), next);
        s.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
        // Same erase ItemHeat.set runs: a held attuned part warmed past its attuning point loses its attunements.
        Attunements.eraseIfOverheated(s, new Tu(next), level);
    }
}
