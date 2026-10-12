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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.net.GunFirePayload;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Firing on the client ({@code feedback_guns_spec.md} section 7, and 7.1 for what each choice
 * was taken from).
 *
 * <p>Everything the player feels happens here on the frame the button goes down, before the
 * server has heard of it: the sound, the smoke, and the kick. The server is asked afterwards
 * ({@link GunFirePayload}) and resolves the hit. That is prediction, and it is what keeps a gun
 * from feeling like it is on a bad ping.
 *
 * <p>Fire rate is wall-clock milliseconds polled once a <em>frame</em>, not once a tick, so the
 * cycle is the gun's and not the tick's (spec 7.1: TacZ's lesson). The kick is applied to the
 * player's actual view each frame, so the aim is displaced rather than the screen shaken.
 *
 * <p>Held fire is automatic for now; fire mode is runtime state (spec 2) and a later step.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunClient {

    /** One shot's kick: a pitch and yaw to land over {@link FTuning#GUN_KICK_MS}, and how much of it already has. */
    private static final class Kick {
        final long startMs;
        final float pitch, yaw;
        float applied;

        Kick(long startMs, float pitch, float yaw) {
            this.startMs = startMs;
            this.pitch = pitch;
            this.yaw = yaw;
        }
    }

    private static final List<Kick> KICKS = new ArrayList<>();
    private static long nextShotMs;

    private GunClient() {
    }

    /** A gun owns the attack button while held: no swing, no vanilla block-breaking. */
    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
            return;
        long now = System.currentTimeMillis();

        GunFigures figures = GunItem.figures(player.getMainHandItem());
        boolean holding = player.getMainHandItem().getItem() instanceof GunItem && figures != null;
        boolean trigger = holding && mc.screen == null && mc.mouseHandler.isMouseGrabbed()
                && mc.options.keyAttack.isDown() && !player.isSpectator();

        if (trigger) {
            // Idle for a cycle or more: the first shot is now. Otherwise keep the gun's own rhythm,
            // so a slow frame does not stretch the rate and a fast one does not shorten it.
            if (now - nextShotMs > figures.cycleMs())
                nextShotMs = now;
            for (int shots = 0; now >= nextShotMs && shots < 3; shots++) {
                shoot(mc, player, figures, now);
                nextShotMs += figures.cycleMs();
            }
        }
        applyKicks(player, now);
    }

    private static void shoot(Minecraft mc, LocalPlayer player, GunFigures f, long now) {
        PacketDistributor.sendToServer(new GunFirePayload(player.getYRot(), player.getXRot()));

        mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), GunSound.event(),
                SoundSource.PLAYERS, GunSound.volume(f), GunSound.pitch(f, player.getRandom()), false);
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        mc.level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0, 0.01, 0);

        // The kick has a noise floor (philosophy 8): the amplitude is the gun's, the instance is not.
        float noise = 0.75f + player.getRandom().nextFloat() * 0.5f;
        float yawNoise = (player.getRandom().nextFloat() - 0.5f) * 0.6f;
        KICKS.add(new Kick(now, f.kickDeg() * noise, f.kickDeg() * yawNoise));
    }

    /** Land each kick over its time with an ease-out, by the part of it this frame adds. */
    private static void applyKicks(LocalPlayer player, long now) {
        for (Iterator<Kick> it = KICKS.iterator(); it.hasNext(); ) {
            Kick k = it.next();
            float t = Mth.clamp((now - k.startMs) / (float) FTuning.GUN_KICK_MS, 0f, 1f);
            float eased = 1 - (1 - t) * (1 - t);
            float delta = eased - k.applied;
            k.applied = eased;
            player.setXRot(Mth.clamp(player.getXRot() - k.pitch * delta, -90f, 90f));
            player.setYRot(player.getYRot() + k.yaw * delta);
            if (t >= 1f)
                it.remove();
        }
    }
}
