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
import io.github.soundgoodizerfan.feedback.net.GunReloadPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.sounds.SoundEvents;
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

    /** Swap magazines. Registered by {@code FClientSetup}. */
    public static final KeyMapping RELOAD = new KeyMapping("key.feedback.reload", InputConstants.KEY_R, "key.categories.feedback");

    private static final List<Kick> KICKS = new ArrayList<>();
    private static long nextShotMs;
    /** Wall-clock ms the current reload ends, for the HUD. */
    private static long reloadUntilMs;

    /** Whether a reload is still in progress, and for the HUD to say so. */
    public static boolean reloading() {
        return System.currentTimeMillis() < reloadUntilMs;
    }

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

        var held = player.getMainHandItem();
        boolean holding = held.getItem() instanceof GunItem;
        GunDesign design = holding ? held.get(FWeapons.GUN_DESIGN.get()) : null;
        // What the next round does in this gun: the client predicts from the same record the server fires.
        GunFigures figures = design != null && !GunHazard.isSpent(held)
                ? GunAmmo.next(held).flatMap(r -> design.bake(ClientMaterials.on(mc.level), r)).orElse(null) : null;
        boolean canAct = holding && design != null && mc.screen == null && mc.mouseHandler.isMouseGrabbed()
                && !player.isSpectator();

        while (RELOAD.consumeClick()) {
            if (canAct) {
                reloadUntilMs = now + FTuning.GUN_RELOAD_MS;
                PacketDistributor.sendToServer(new GunReloadPayload());
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN,
                        SoundSource.PLAYERS, 0.6f, 1.4f, false);
                nextShotMs = now + FTuning.GUN_RELOAD_MS;
            }
        }
        boolean trigger = canAct && mc.options.keyAttack.isDown();

        if (trigger && figures == null) {
            // Empty, or the wrong bore: a click, not a shot, and not a click every frame.
            if (now >= nextShotMs) {
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 0.5f, 1.6f, false);
                nextShotMs = now + 300;
            }
        } else if (trigger) {
            // Idle for a cycle or more: the first shot is now.
            if (now - nextShotMs > figures.cycleMs())
                nextShotMs = now;
            if (now >= nextShotMs) {
                shoot(mc, player, figures, now);
                // Keep the gun's rhythm, but never squeeze the gap below three quarters of a
                // cycle. Paying a late frame back as a second shot right behind the first is a
                // flam, and a gun that is steady matters more than one that is exact: a hitch
                // costs the long-run rate a little instead. (The first draft caught up, and
                // fired up to three shots in one frame.)
                nextShotMs = Math.max(nextShotMs + figures.cycleMs(), now + figures.cycleMs() * 3L / 4);
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
