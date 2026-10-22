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
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryPayload;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryStats;
import io.github.soundgoodizerfan.feedback.combat.roll.RollPayload;
import io.github.soundgoodizerfan.feedback.combat.roll.Rolling;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;

import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The parry and roll keys. A press goes to the server, which decides; the roll's displacement is also
 * run here at once so it does not wait a round trip (the server's velocity packet then agrees). No pose
 * yet: the raise is a placeholder that plays nothing until there is an animation to play.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CombatKeys {

    public static final KeyMapping PARRY = new KeyMapping("key.feedback.parry", InputConstants.KEY_G, "key.categories.feedback");
    public static final KeyMapping ROLL = new KeyMapping("key.feedback.roll", InputConstants.KEY_LALT, "key.categories.feedback");

    private CombatKeys() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null) {
            while (PARRY.consumeClick() || ROLL.consumeClick()) {
            }
            return;
        }
        while (PARRY.consumeClick()) {
            var stats = ParryStats.of(player);
            if (CombatState.of(player).ready() && stats.isPresent()) {
                PacketDistributor.sendToServer(new ParryPayload());
                player.swing(stats.get().hand());
            } else
                deny(player);
        }
        while (ROLL.consumeClick())
            if (Rolling.canRoll(player)) {
                float forward = player.input.forwardImpulse, strafe = player.input.leftImpulse;
                PacketDistributor.sendToServer(new RollPayload(forward, strafe));
                Rolling.begin(player, forward, strafe, false);
            } else
                deny(player);
    }

    private static long lastDenyTick = Long.MIN_VALUE / 2;

    /** A press that did nothing: a click for this player only, at most once per {@link FeelTuning#DENY_MIN_TICKS}. */
    private static void deny(LocalPlayer player) {
        long now = player.level().getGameTime();
        if (now - lastDenyTick < FeelTuning.DENY_MIN_TICKS)
            return;
        lastDenyTick = now;
        player.playSound(FSounds.PRESS_DENIED.get(), FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
    }
}
