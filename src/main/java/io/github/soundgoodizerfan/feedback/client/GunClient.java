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
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.net.GunFirePayload;
import io.github.soundgoodizerfan.feedback.net.GunModePayload;
import io.github.soundgoodizerfan.feedback.net.GunReloadPayload;
import io.github.soundgoodizerfan.feedback.net.GunUnloadPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Firing and handling on the client ({@code docs/feedback_guns_spec.md} section 7, and 7.1 for what
 * each choice was taken from).
 *
 * <p>Everything the player feels happens here on the frame the button goes down, before the
 * server has heard of it: the sound, the smoke, and the kick. The server is asked afterwards
 * ({@link GunFirePayload}) and resolves the hit. That is prediction, and it is what keeps a gun
 * from feeling like it is on a bad ping.
 *
 * <p>Fire rate is wall-clock milliseconds polled once a <em>frame</em>, not once a tick, so the
 * cycle is the gun's and not the tick's (spec 7.1: TacZ's lesson). The kick is applied to the
 * player's actual view each frame, so the aim is displaced rather than the screen shaken, and it
 * <em>climbs</em> over a streak of shots and settles after a pause.
 *
 * <p>The use key is the gun's too: held, it aims. Aiming eases the FOV down to the sight's, and
 * the server narrows the spread for a shooter who is standing and not sprinting ({@code
 * GunStance}). Fire mode is the held gun's component, switched by key and read here.
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
    public static final KeyMapping FIRE_MODE = new KeyMapping("key.feedback.fire_mode", InputConstants.KEY_V, "key.categories.feedback");
    public static final KeyMapping UNLOAD = new KeyMapping("key.feedback.unload", InputConstants.KEY_U, "key.categories.feedback");

    private static final List<Kick> KICKS = new ArrayList<>();
    /** Wall-clock ms the current reload ends, for the HUD. */
    private static long reloadUntilMs;
    /** 0 hip, 1 fully aimed: eased toward the use key over the sight's raise time. */
    private static float aim;
    private static long lastFrameMs;
    private static long lastShotMs;
    private static int streak;
    private static boolean reloadWasDown;

    private GunClient() {
    }

    /** Whether a reload is still in progress, and for the HUD to say so. */
    public static boolean reloading() {
        return System.currentTimeMillis() < reloadUntilMs;
    }

    /** How far aimed the held gun is, 0 to 1. */
    public static float aim() {
        return aim;
    }

    /** A gun owns the attack and use buttons while held: no swing, no block-breaking, no placing. */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;
        // The use key is also the off-hand pistol's trigger, when it has one.
        boolean mainGun = player.getMainHandItem().getItem() instanceof GunItem;
        if ((event.isAttack() && mainGun) || (event.isUseItem() && (mainGun || GunItem.offHandFires(player)))) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    /** Aimed down a sight the world is closer. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        var held = event.getPlayer().getMainHandItem();
        if (aim > 0 && held.getItem() instanceof GunItem && GunSpec.of(held).orElse(null) != null) {
            float zoom = GunSpec.of(held).orElse(null).sight().fov();
            event.setNewFovModifier(event.getNewFovModifier() * Mth.lerp(aim, 1f, zoom));
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
            return;
        long now = System.currentTimeMillis();
        long dt = lastFrameMs == 0 ? 0 : Math.min(now - lastFrameMs, 100);
        lastFrameMs = now;

        var held = player.getMainHandItem();
        var offItem = player.getOffhandItem();
        boolean holding = held.getItem() instanceof GunItem;
        GunSpec design = holding ? GunSpec.of(held).orElse(null) : null;
        GunSpec offDesign = offItem.getItem() instanceof GunItem ? GunSpec.of(offItem).orElse(null) : null;
        boolean offFires = GunItem.offHandFires(player) && offDesign != null;
        boolean ready = mc.screen == null && mc.mouseHandler.isMouseGrabbed() && !player.isSpectator();
        boolean canAct = holding && design != null && ready;
        boolean offCanAct = offFires && ready;
        // What the next round does in each gun: the client predicts from the same record the server fires.
        GunFigures figures = figuresOf(mc, player, held, design);
        GunFigures offFigures = offFires ? figuresOf(mc, player, offItem, offDesign) : null;

        // Aiming eases toward the use key at the sight's own pace, and back out faster. With a pistol
        // on the use key in the off hand, the key is that pistol's and nothing is aimed.
        boolean wantsAim = canAct && mc.options.keyUse.isDown() && !reloading() && !offFires;
        float step = design == null ? 1f : dt / (float) design.sight().raiseMs();
        aim = Mth.clamp(aim + (wantsAim ? step : -step * 1.5f), 0f, 1f);

        // R is held, not tapped: the server reloads while it is down and stops when it comes up, both guns if there are two.
        while (RELOAD.consumeClick()) {
        }
        boolean offGunHeld = offDesign != null;
        boolean reloadDown = (canAct || (offGunHeld && ready)) && RELOAD.isDown();
        if (reloadDown && !reloadWasDown) {
            // The estimate is every held gun's own reload, back to back: the server runs the emptiest first.
            long took = 0;
            for (var pair : new Object[][]{{held, design}, {offItem, offDesign}}) {
                var gun = (net.minecraft.world.item.ItemStack) pair[0];
                var gunDesign = (GunSpec) pair[1];
                if (gunDesign != null)
                    took += gunDesign.internalCapacity().isPresent()
                            ? (long) GunAmmo.loadable(player.getInventory(), gun, gunDesign).size() * FTuning.GUN_RELOAD_ROUND_MS
                            : GunAmmo.reloadMs(gun);
            }
            took = Math.round(took * GunShots.reloadScale(player));
            if (took > 0) {
                reloadUntilMs = now + took;
                PacketDistributor.sendToServer(new GunReloadPayload(true));
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN,
                        SoundSource.PLAYERS, 0.6f, 1.4f, false);
                MAIN.burstLeft = 0;
                OFF.burstLeft = 0;
            }
        } else if (!reloadDown && reloadWasDown) {
            reloadUntilMs = 0;
            PacketDistributor.sendToServer(new GunReloadPayload(false));
        }
        reloadWasDown = reloadDown;
        // Mode and unload go to the main gun; the off-hand one when that is the only gun, or Shift is held.
        boolean keyOff = offGunHeld && (!canAct || player.isShiftKeyDown());
        while (FIRE_MODE.consumeClick()) {
            if (canAct || (offGunHeld && ready)) {
                PacketDistributor.sendToServer(new GunModePayload(keyOff));
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.PLAYERS, 0.4f, 1.8f, false);
                MAIN.burstLeft = 0;
                OFF.burstLeft = 0;
            }
        }
        while (UNLOAD.consumeClick()) {
            if (canAct || (offGunHeld && ready))
                PacketDistributor.sendToServer(new GunUnloadPayload(keyOff));
        }

        // A revolver fanned (crouched, hammer and cylinder): the hand slaps the hammer for as long as the button is down.
        boolean fanning = design != null && design.has("fan") && io.github.soundgoodizerfan.feedback.modular.trait.GunFan.fanning(player);
        boolean offFanning = offDesign != null && offDesign.has("fan") && io.github.soundgoodizerfan.feedback.modular.trait.GunFan.fanning(player);
        pull(mc, player, held, figures, MAIN, canAct, canAct && mc.options.keyAttack.isDown(), fanning, false, now);
        pull(mc, player, offItem, offFigures, OFF, offCanAct, offCanAct && mc.options.keyUse.isDown(), offFanning, true, now);
        applyKicks(player, now);
    }

    /** What the next round does in {@code gun} as it is now, or null when it cannot fire. */
    private static GunFigures figuresOf(Minecraft mc, LocalPlayer player, net.minecraft.world.item.ItemStack gun, GunSpec design) {
        if (design == null || GunHazard.isSpent(gun))
            return null;
        var materials = ClientMaterials.on(mc.level);
        var round = GunAmmo.next(gun).map(r -> r.at(mc.level.getGameTime()));
        var base = round.flatMap(r -> design.firing(GunAmmo.count(gun)).bake(materials, r));
        var load = round.flatMap(r -> r.load(materials));
        if (base.isPresent() && load.isPresent() && !GunCondition.quenched(gun, mc.level, load.get()))
            return ModularTraits.gunAimed(player, gun, GunCondition.effective(base.get(), gun, mc.level, load.get().ignitionTu()));
        return null;
    }

    /** One hand's trigger: its own fire rhythm, burst and button edge, so two guns do not share a cycle. */
    private static final class Trigger {
        long nextShotMs;
        boolean wasDown;
        int burstLeft;
    }

    private static final Trigger MAIN = new Trigger(), OFF = new Trigger();

    /** The fire loop of one gun: its button, its mode, its figures. */
    private static void pull(Minecraft mc, LocalPlayer player, net.minecraft.world.item.ItemStack gun, GunFigures figures,
                             Trigger t, boolean active, boolean down, boolean fanning, boolean off, long now) {
        FireMode mode = GunItem.mode(gun);
        boolean pressed = down && !t.wasDown;
        t.wasDown = down;
        if (pressed && mode == FireMode.BURST && figures != null)
            t.burstLeft = FTuning.GUN_BURST_ROUNDS;
        if (!active)
            t.burstLeft = 0;
        boolean wants = switch (mode) {
            case AUTO -> down;
            case SEMI -> pressed || (fanning && down);
            case BURST -> t.burstLeft > 0;
        };
        if (!wants)
            return;
        if (figures == null) {
            // Empty, or the wrong bore: a click, not a shot, and not a click every frame.
            if (pressed || mode == FireMode.AUTO) {
                if (now >= t.nextShotMs) {
                    mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.DISPENSER_FAIL,
                            SoundSource.PLAYERS, 0.5f, 1.6f, false);
                    t.nextShotMs = now + 300;
                }
            }
            t.burstLeft = 0;
            return;
        }
        // Idle for a cycle or more: the first shot is now.
        if (now - t.nextShotMs > figures.cycleMs())
            t.nextShotMs = now;
        if (now >= t.nextShotMs) {
            shoot(mc, player, gun, figures, off, now);
            if (mode == FireMode.BURST)
                t.burstLeft--;
            // Keep the gun's rhythm, but never squeeze the gap below three quarters of a
            // cycle. Paying a late frame back as a second shot right behind the first is a
            // flam, and a gun that is steady matters more than one that is exact: a hitch
            // costs the long-run rate a little instead. (The first draft caught up, and
            // fired up to three shots in one frame.)
            t.nextShotMs = Math.max(t.nextShotMs + figures.cycleMs(), now + figures.cycleMs() * 3L / 4);
            // A burst ends with a beat, so the next burst is a decision and not a held button.
            if (mode == FireMode.BURST && t.burstLeft == 0)
                t.nextShotMs += figures.cycleMs();
        }
    }

    private static void shoot(Minecraft mc, LocalPlayer player, net.minecraft.world.item.ItemStack gun, GunFigures f, boolean off, long now) {
        PacketDistributor.sendToServer(new GunFirePayload(player.getYRot(), player.getXRot(), aim > 0.5f, off));
        reloadUntilMs = 0; // a pull ends a reload; the server does the same

        for (GunSound.Layer layer : GunSound.layers(f, player.getRandom()))
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), layer.event(),
                    SoundSource.PLAYERS, layer.volume(), layer.pitch(), false);
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        mc.level.addParticle(f.coil() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0, 0.01, 0);
        // A hot barrel smokes between shots as well as at them: the thing a hand and an eye can see.
        float heat = GunCondition.heatFraction(gun, mc.level, FTuning.GUN_PROPELLANT_IGNITION_TU);
        for (int i = 0; i < (int) (heat * 4); i++)
            mc.level.addParticle(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    (player.getRandom().nextFloat() - 0.5) * 0.02, 0.03, (player.getRandom().nextFloat() - 0.5) * 0.02);

        // Recoil climbs over a streak and settles after a pause; aiming takes the edge off it.
        streak = now - lastShotMs < f.cycleMs() * 5L / 2 ? Math.min(streak + 1, FTuning.GUN_KICK_CLIMB_MAX) : 0;
        lastShotMs = now;
        float climb = 1 + FTuning.GUN_KICK_CLIMB_PER_SHOT * streak;
        float aimed = Mth.lerp(aim, 1f, FTuning.GUN_ADS_KICK);
        // The kick has a noise floor (philosophy 8): the amplitude is the gun's, the instance is not.
        float noise = 0.75f + player.getRandom().nextFloat() * 0.5f;
        float yawNoise = (player.getRandom().nextFloat() - 0.5f) * 0.6f;
        KICKS.add(new Kick(now, f.kickDeg() * noise * climb * aimed, f.kickDeg() * yawNoise * aimed));
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
