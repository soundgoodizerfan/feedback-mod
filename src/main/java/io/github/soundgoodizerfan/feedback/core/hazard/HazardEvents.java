// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Where a person meets the hazards: a hot piece in a hand, a hot or live thing under a hand.
 *
 * <h2>Two ways to meet one, no machine in either</h2>
 * Philosophy 9 asks that a glowing ingot be a glowing ingot wherever it is, a hand included, and
 * that consequences which follow obviously be allowed to. So the piece in a hand is read from
 * its own stamp ({@link ItemHeat}, nothing ticked per item), and a block is hot if it is a
 * {@link ThermalBody} at a temperature -- not if it is a crucible. A mod's own heated block that
 * implements the interface is covered with no change here, which is the point of asking the
 * interface instead of the identity (philosophy 12).
 *
 * <h2>A consequence, never a lock</h2>
 * Touching is not cancelled. The use goes through and the hand pays; the player who must reach
 * into a hot crucible with a bare hand can, once, and learns why gloves exist. Creative and
 * spectator players are left alone, as for any damage.
 */
public final class HazardEvents {

    private HazardEvents() {
    }

    /** A hot stack in either hand, checked twice a second. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % HazardTuning.CONTACT_INTERVAL_TICKS != 0)
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        float safe = Protection.handlesUpToTu(player);
        for (InteractionHand hand : InteractionHand.values()) {
            burnFor(player, player.getItemInHand(hand), safe);
        }
        burnFor(player, player.containerMenu.getCarried(), safe);
    }

    private static void burnFor(Player player, ItemStack held, float safe) {
        {
            if (held.isEmpty() || !ItemHeat.isHot(held, player.level()))
                return;
            Tu tu = ItemHeat.get(held, player.level());
            float limit = held.getItem() instanceof Handled h ? Math.max(safe, h.holdsUpToTu(held, player.level())) : safe;
            if (tu.value() > limit && HotContact.burn(player, tu)) {
                warn(player, "hazard.feedback.hot_hand");
                // The hand takes the heat the piece gives up.
                ItemHeat.drain(held, player.level(), (tu.value() - AmbientTable.at(player.level()).value()) * HazardTuning.CONTACT_HEAT_TAKEN);
            }
            else if (tu.value() > ThermalTuning.CONTACT_PAIN_TU.value() && tu.value() <= limit && Protection.insulatedFrom(player, tu.value()))
                io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(player, "progress/heat/tongs");
        }
    }

    /** A bare hand on a hot block or a live conductor. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND)
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        // A hot block is not a hazard to open; the hot piece taken out burns via onPlayerTick.
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof ElectricNode node) {
            if (Shock.apply(player, node.touchBand()))
                warn(player, "hazard.feedback.shock");
        }
    }

    private static void warn(Player player, String key) {
        if (player instanceof ServerPlayer sp)
            sp.displayClientMessage(Component.translatable(key), true);
    }
}
