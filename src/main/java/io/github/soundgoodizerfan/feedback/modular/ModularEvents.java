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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * Where vanilla's tools, weapons and armour arrive in the world, and each is made modular
 * ({@link ModularConvert}): loot (below, a global loot modifier), what a mob spawns holding, what a
 * villager offers, and -- for anything that got past those -- what a player finds in a pocket,
 * looked at once a second.
 */
public final class ModularEvents {

    private ModularEvents() {
    }

    /** Once a second is plenty: a stack this rarely sits unconverted long enough to be used. */
    private static final int INVENTORY_INTERVAL_TICKS = 20;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INVENTORY_INTERVAL_TICKS != 0)
            return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (ModularConvert.convertible(stack))
                inv.setItem(i, ModularConvert.convert(stack));
        }
    }

    /** A mob's gear, as it enters the world: spawned with it, or loaded holding it. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob))
            return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack held = mob.getItemBySlot(slot);
            if (ModularConvert.convertible(held))
                mob.setItemSlot(slot, ModularConvert.convert(held));
        }
    }

    public static void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().replaceAll((level, listings) -> wrap(listings));
    }

    public static void onWandererTrades(WandererTradesEvent event) {
        List<VillagerTrades.ItemListing> generic = wrap(event.getGenericTrades());
        event.getGenericTrades().clear();
        event.getGenericTrades().addAll(generic);
        List<VillagerTrades.ItemListing> rare = wrap(event.getRareTrades());
        event.getRareTrades().clear();
        event.getRareTrades().addAll(rare);
    }

    private static List<VillagerTrades.ItemListing> wrap(List<VillagerTrades.ItemListing> listings) {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        for (VillagerTrades.ItemListing l : listings)
            out.add((trader, random) -> {
                MerchantOffer offer = l.getOffer(trader, random);
                if (offer == null || !ModularConvert.convertible(offer.getResult()))
                    return offer;
                return new MerchantOffer(offer.getItemCostA(), offer.getItemCostB(), ModularConvert.convert(offer.getResult()),
                        offer.getUses(), offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier(), offer.getDemand());
            });
        return out;
    }

    /** Converts what a loot table generated. */
    public static class ConvertLoot extends LootModifier {

        public static final MapCodec<ConvertLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ConvertLoot::new));

        public ConvertLoot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            loot.replaceAll(ModularConvert::convert);
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
