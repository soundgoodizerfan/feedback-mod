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
package io.github.soundgoodizerfan.feedback.village;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Takes the right-click on a villager or wandering trader away from vanilla's trade screen and opens ours. */
public final class VillageEvents {

    private VillageEvents() {
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractVillager m) || event.getItemStack().getItem() instanceof SpawnEggItem
                || !m.isAlive() || m.isSleeping() || event.getEntity().isSecondaryUseActive())
            return;
        // A child, an unemployed villager and a nitwit have nothing to deal in; vanilla's own reaction stands.
        if (m instanceof Villager v && (v.isBaby() || v.getVillagerData().getProfession() == VillagerProfession.NONE
                || v.getVillagerData().getProfession() == VillagerProfession.NITWIT))
            return;
        event.setCanceled(true);
        if (m.isTrading() && m.getTradingPlayer() != event.getEntity()) { // someone else is at the counter
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            return;
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp)
            VillageEconomy.open(sp, m);
    }
}
