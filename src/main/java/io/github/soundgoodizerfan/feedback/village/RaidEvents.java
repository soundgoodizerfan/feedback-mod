// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        if (!(event.getLevel() instanceof ServerLevel))
            return;
        if (event.getEntity() instanceof Raider raider)
            raider.goalSelector.addGoal(2, new RaiderStealGoal(raider));
        else if (event.getEntity() instanceof net.minecraft.world.entity.npc.Villager v
                && v.getVillagerData().getProfession() == net.minecraft.world.entity.npc.VillagerProfession.FARMER)
            giveSeeds(v);
    }

    /**
     * A farmer picks up and plants only seeds it holds, and its pick-up list is fixed, so a farmer who meets the mod's crops holds none of its seeds.
     * Joining a level with none, it is given one starter stack (once: not per tick), and then works the mod's crops on its plot as it works wheat.
     * Output is food the shop sells and Counters stock.
     */
    private static void giveSeeds(net.minecraft.world.entity.npc.Villager v) {
        var seeds = io.github.soundgoodizerfan.feedback.registry.FCrops.SEEDS;
        for (var seed : seeds)
            if (v.getInventory().hasAnyOf(java.util.Set.of(seed.get())))
                return;
        // Rice is a paddy crop and is left off the plantable tag, so a farmer is not given its seed.
        var plantable = seeds.stream().filter(s -> !s.getId().getPath().startsWith("rice")).toList();
        v.getInventory().addItem(new ItemStack(plantable.get(Math.floorMod(v.getUUID().hashCode(), plantable.size())).get(), 8));
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
