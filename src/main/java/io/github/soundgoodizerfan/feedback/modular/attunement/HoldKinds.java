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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Hold effect: friction coupling. {@code clamp} on a
 * tool's grip keeps it from being thrown out of the hand (a clamp that is also hot keeps burning, through the ordinary
 * hot-contact hazard); {@code limber} on a legging lining lets the wearer crouch faster. Armour that cannot be taken off
 * and a clamp on swapping slots are not built: NeoForge has no clean hook for either.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HoldKinds {

    /** Sneaking speed added per level of limber (base is 0.3). [OPEN] until played */
    private static final double LIMBER_PER_LEVEL = 0.1;

    private static final ResourceLocation LIMBER = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "hold_limber");

    /** Chance per landed blow, per level of slip, that the weapon leaves the wielder's hand. [OPEN] until played */
    private static final float SLIP_PER_LEVEL = 0.04f;

    private HoldKinds() {
    }

    public static void register() {
        // Hold- on a handle: a loose grip can lose the weapon, which makes it half a curse. A clamp elsewhere still wins (the toss is cancelled).
        AttunementKinds.onStrike("slip", (event, a) -> {
            if (!(event.getSource().getEntity() instanceof Player player) || player.getRandom().nextFloat() >= a.level() * SLIP_PER_LEVEL)
                return;
            InteractionHand hand = StrikeSource.hand(player, StrikeSource.weapon(player, event.getSource()));
            ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty() && player.drop(held.copy(), false) != null)
                player.setItemInHand(hand, ItemStack.EMPTY);
        });
    }

    /** A clamped tool does not leave the hand by a toss; vanilla hands the stack back to the inventory. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (TideKinds.level(event.getEntity().getItem(), "clamp") > 0)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 10 != 0)
            return;
        TideKinds.sync(player, Attributes.SNEAKING_SPEED, LIMBER, TideKinds.level(player.getItemBySlot(EquipmentSlot.LEGS), "limber") * LIMBER_PER_LEVEL);
    }
}
