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

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FVillage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Raider hooks: the steal goal, added once as each raider joins a level, and what a dead raider drops of what it carried off. */
public final class RaidEvents {

    private RaidEvents() {
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Raider raider)
            raider.goalSelector.addGoal(2, new RaiderStealGoal(raider));
    }

    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Raider raider))
            return;
        List<ItemStack> loot = raider.getData(FVillage.LOOT);
        if (loot.isEmpty())
            return;
        for (ItemStack s : loot)
            event.getDrops().add(new ItemEntity(raider.level(), raider.getX(), raider.getY(), raider.getZ(), s.copy()));
        raider.setData(FVillage.LOOT, List.of());
    }
}
