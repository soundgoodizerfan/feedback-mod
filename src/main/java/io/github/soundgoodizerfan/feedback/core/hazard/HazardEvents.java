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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
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
 * interface instead of the identity (philosophy 11).
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
        if (player.level().isClientSide || player.tickCount % FTuning.CONTACT_INTERVAL_TICKS != 0)
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        float safe = Protection.handlesUpToTu(player);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty() || !ItemHeat.isHot(held, player.level()))
                continue;
            Tu tu = ItemHeat.get(held, player.level());
            float limit = held.getItem() instanceof Handled h ? Math.max(safe, h.holdsUpToTu(held, player.level())) : safe;
            if (tu.value() > limit && HotContact.burn(player, tu))
                warn(player, "hazard.feedback.hot_hand");
        }
    }

    /** A bare hand on a hot block or a live conductor. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND)
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof ThermalBody body) {
            Tu tu = body.getTemperature();
            if (tu.value() > Protection.handlesUpToTu(player) && HotContact.burn(player, tu))
                warn(player, "hazard.feedback.hot_touch");
        }
        if (be instanceof ElectricNode node) {
            float volts = node.touchVolts();
            if (volts > 0f && Shock.apply(player, volts))
                warn(player, "hazard.feedback.shock");
        }
    }

    private static void warn(Player player, String key) {
        if (player instanceof ServerPlayer sp)
            sp.displayClientMessage(Component.translatable(key), true);
    }
}
