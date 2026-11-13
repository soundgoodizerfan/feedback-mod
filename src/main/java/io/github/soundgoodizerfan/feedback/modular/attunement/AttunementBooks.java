// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A book teaches; it does not apply (roadmap 4). Right-click a vanilla enchanted book and, for each enchantment it holds that an attunement
 * names in its {@code books} list, the player learns that attunement; the book is used up. A book that teaches nothing new, or nothing at
 * all, is kept and says so. Villager and loot books are vanilla's, so they teach by this table without any change to them.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class AttunementBooks {

    private AttunementBooks() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.ENCHANTED_BOOK) || event.getEntity().isShiftKeyDown())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        ItemEnchantments held = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Attunements.Effect> taught = new ArrayList<>();
        boolean mapped = false;
        for (var entry : held.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(k -> k.location()).orElse(null);
            if (id == null)
                continue;
            for (Attunements.Effect e : Attunements.get().effects().values())
                if (e.books().contains(id)) {
                    mapped = true;
                    if (AttunementKnowledge.learn(player, e))
                        taught.add(e);
                }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!taught.isEmpty()) {
            stack.shrink(1);
            for (Attunements.Effect e : taught)
                player.displayClientMessage(Component.translatable("message.feedback.book.learned",
                        Component.translatable("attunement.feedback." + e.id().getPath())), false);
        } else
            player.displayClientMessage(Component.translatable(mapped ? "message.feedback.book.known" : "message.feedback.book.nothing"), true);
    }
}
