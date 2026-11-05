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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.item.GrappleItem;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The grapple swing, run where the player's motion actually lives: their own client. Each tick, after the player has moved, a taut
 * line cancels the velocity running away from the hook and draws the stretch back ({@link GrappleHook#constrain}); a slack line does nothing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GrappleSwing {

    /** The line the player's own client is running, shortened by the reel from the hook's bite length. */
    private static float rope;
    private static boolean swinging;

    private GrappleSwing() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || player.isSpectator())
            return;
        GrappleHook hook = GrappleHook.of(mc.level, player);
        if (hook == null || !hook.anchored() || hook.swingRope() <= 0f) {
            // Letting go while moving throws the swing's momentum on (boosted, capped).
            Vec3 v = player.getDeltaMovement();
            if (swinging && v.length() >= ModularTuning.GRAPPLE_RELEASE_MIN && !player.onGround()) {
                Vec3 boosted = v.scale(ModularTuning.GRAPPLE_RELEASE_BOOST);
                player.setDeltaMovement(boosted.length() > ModularTuning.GRAPPLE_RELEASE_MAX ? boosted.scale(ModularTuning.GRAPPLE_RELEASE_MAX / boosted.length()) : boosted);
            }
            swinging = false;
            rope = 0f;
            return;
        }
        swinging = true;
        if (rope <= 0f || rope > hook.swingRope())
            rope = hook.swingRope();
        // Holding use with the grapple in hand winches the line in.
        if (mc.screen == null && mc.options.keyUse.isDown() && (player.getMainHandItem().getItem() instanceof GrappleItem
                || player.getOffhandItem().getItem() instanceof GrappleItem))
            rope = Math.max((float) ModularTuning.GRAPPLE_STOP_DISTANCE, rope - hook.reelSpeed());
        Vec3 offset = hook.position().subtract(player.position().add(0, player.getBbHeight() * 0.5, 0));
        player.setDeltaMovement(GrappleHook.constrain(offset, rope, player.getDeltaMovement()));
        player.fallDistance = 0f;
    }
}
