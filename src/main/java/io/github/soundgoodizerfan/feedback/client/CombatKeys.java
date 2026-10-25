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


    /** The five cast keys (Z, Y, G; the last two unbound until set): each fires the technique slotted to it, and letting go ends a held one; the server checks the unlock, rearm and stamina. */
    public static final KeyMapping[] CAST = {
            new KeyMapping("key.feedback.cast_1", InputConstants.KEY_Z, "key.categories.feedback"),
            new KeyMapping("key.feedback.cast_2", InputConstants.KEY_Y, "key.categories.feedback"),
            new KeyMapping("key.feedback.cast_3", InputConstants.KEY_G, "key.categories.feedback"),
            new KeyMapping("key.feedback.cast_4", InputConstants.UNKNOWN.getValue(), "key.categories.feedback"),
            new KeyMapping("key.feedback.cast_5", InputConstants.UNKNOWN.getValue(), "key.categories.feedback")};

    /** The radial key: tap casts the technique selected in the radial, hold opens {@link CastRadialScreen} to select another. An alternative to the cast keys, not a replacement. */
    public static final KeyMapping RADIAL = new KeyMapping("key.feedback.cast_radial", InputConstants.KEY_C, "key.categories.feedback");
    /** Ticks the radial key must be held before the radial opens; a shorter press is a tap. */
    private static final int RADIAL_HOLD = 6;
    private static int radialTicks;
    /** Set once a hold has opened the radial, until the key is let go, so closing it with the key still down does not cast. */
    private static boolean radialSpent;

    /** What a cast slot's key reads as on the HUD and skill screen: its key, or the slot number if it is unbound. */
    public static net.minecraft.network.chat.Component castLabel(int slot) {
        return CAST[slot].isUnbound() ? net.minecraft.network.chat.Component.literal(Integer.toString(slot + 1)) : CAST[slot].getTranslatedKeyMessage();
    }

    private static boolean jumpHeld;
    /** Which cast keys were down last tick, so letting one go can end a held cast. */
    private static final boolean[] castHeld = new boolean[CAST.length];

    private CombatKeys() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        boolean live = player != null && mc.screen == null;
        for (int i = 0; i < CAST.length; i++) {
            boolean down = live && CAST[i].isDown(), clicked = false;
            while (CAST[i].consumeClick())
                clicked = true;
            // Clicks while the key was already down are the OS's key repeat, not presses: they would let a hold go.
            boolean fresh = live && clicked && !castHeld[i];
            if (fresh)
                PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads.Fire(i));
            // a tap pressed and let go inside one tick still needs its release
            if ((castHeld[i] || fresh) && !down && player != null)
                PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads.Release(i));
            castHeld[i] = down;
        }
        radial(mc, player, live);
        if (!live) {
            while (PARRY.consumeClick() || ROLL.consumeClick()) {
            }
            return;
        }
        // Wall kick: a fresh jump tap in the air against a wall (never a held key); the server checks the unlock, momentum and facing against a wall; the server checks the unlock and the momentum.
        boolean jump = mc.options.keyJump.isDown();
        if (jump && !jumpHeld && !player.isShiftKeyDown() && !player.onGround() && player.horizontalCollision)
            PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.combat.roll.WallKickPayload());
        jumpHeld = jump;
        while (PARRY.consumeClick()) {
            var stats = ParryStats.of(player);
            // A worn Feedbacker parries with nothing in hand, so a weapon is not the only way to press.
            boolean gauntlet = stats.isEmpty() && !io.github.soundgoodizerfan.feedback.combat.gear.CombatGear
                    .granting(player, io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.PUNCH_PROJECTILES).isEmpty();
            if (CombatState.of(player).ready() && (stats.isPresent() || gauntlet)) {
                PacketDistributor.sendToServer(new ParryPayload());
                player.swing(stats.map(ParryStats::hand).orElse(net.minecraft.world.InteractionHand.MAIN_HAND));
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


    /** Tap: cast the selected slot (the server decides). Hold: open the radial to change the selection. */
    private static void radial(Minecraft mc, LocalPlayer player, boolean live) {
        while (RADIAL.consumeClick()) {
        }
        boolean down = live && RADIAL.isDown();
        if (down && !radialSpent) {
            if (++radialTicks == RADIAL_HOLD) {
                radialSpent = true;
                radialTicks = 0;
                mc.setScreen(new CastRadialScreen());
            }
            return;
        }
        if (!down && radialTicks > 0 && !radialSpent) {
            int slot = CastRadialScreen.selected();
            if (slot < 0)
                deny(player);
            else {
                PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads.Fire(slot));
                PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.combat.cast.CastPayloads.Release(slot));
            }
        }
        if (!down && !(mc.screen instanceof CastRadialScreen))
            radialSpent = false;
        if (!down)
            radialTicks = 0;
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
