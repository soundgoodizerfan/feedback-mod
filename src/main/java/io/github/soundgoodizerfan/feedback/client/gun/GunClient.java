// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.GunFirePayload;
import io.github.soundgoodizerfan.feedback.net.GunModePayload;
import io.github.soundgoodizerfan.feedback.net.GunReloadPayload;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.net.GunSwingPayload;
import io.github.soundgoodizerfan.feedback.weapon.GunBludgeon;
import net.minecraft.world.InteractionHand;
import io.github.soundgoodizerfan.feedback.net.GunUnloadPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.client.ClientTuning;
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
 * Firing and handling on the client ({@code docs/specs/feedback_guns_spec.md} section 7, and 7.1 for what
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

    /** One shot's kick: a pitch and yaw to land over {@link ClientTuning#GUN_KICK_MS}, and how much of it already has. */
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

    /** Wall-clock ms each hand's bludgeon may swing again. */
    private static long mainSwingMs, offSwingMs;

    private static final List<Kick> KICKS = new ArrayList<>();
    /** Wall-clock ms the current reload ends, for the HUD. */
    private static long reloadUntilMs;
    /** 0 hip, 1 fully aimed: eased toward the use key over the sight's raise time. */
    private static float aim;
    private static long lastFrameMs;
    private static long casingAtMs;
    /** When the forend of a pump gun just fired hits the back of its stroke, and when it slams home; 0 for none. */
    private static long pumpBackAtMs, pumpHomeAtMs;
    private static int drawSlot = -1;
    private static boolean drawHolding;
    private static long lastShotMs;
    private static int streak;
    private static boolean reloadWasDown;

    private GunClient() {
    }

    /** Whether a reload is still in progress, and for the HUD to say so. */
    public static boolean reloading() {
        return System.currentTimeMillis() < reloadUntilMs;
    }

    /** Entity ids of other players aiming a gun, as relayed by the server. */
    private static final java.util.Set<Integer> AIMING = new java.util.HashSet<>();
    private static boolean aimSent;

    public static void setAimed(int entity, boolean aiming) {
        if (aiming)
            AIMING.add(entity);
        else
            AIMING.remove(entity);
    }

    /** Whether a player is drawn aiming: ours from the local ease, others from the relay. */
    public static boolean aimed(net.minecraft.world.entity.LivingEntity e) {
        var me = Minecraft.getInstance().player;
        return e == me ? aim > 0.5f : AIMING.contains(e.getId());
    }

    /** How far a player is drawn aimed, 0 to 1: ours eased, others' the relayed on or off. */
    public static float aimAmount(net.minecraft.world.entity.LivingEntity e) {
        var me = Minecraft.getInstance().player;
        return e == me ? aim : AIMING.contains(e.getId()) ? 1f : 0f;
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
        // The use key is also the off-hand pistol's trigger, when it has one. Barehanded mode is no
        // exception to the cancel: vanilla still does nothing, and onFrame swings the gun itself.
        boolean mainGun = player.getMainHandItem().getItem() instanceof GunItem;
        if ((event.isAttack() && mainGun) || (event.isUseItem() && (mainGun || GunItem.offHandFires(player)))) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    /** Aimed down a sight the sight is the aim: no crosshair. */
    @SubscribeEvent
    public static void onLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        if (aim > 0.5f && event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR))
            event.setCanceled(true);
    }

    /** Aimed down a sight the world is closer. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        var held = event.getPlayer().getMainHandItem();
        // A scope zooms inside its lens (GunLens), not the whole screen.
        if (aim > 0 && held.getItem() instanceof GunItem gun && !io.github.soundgoodizerfan.feedback.client.gun.GunLens.hasScope(held)
                && gun.mechanism().built(held)) {
            float zoom = gun.mechanism().sightZoom(held);
            event.setNewFovModifier(event.getNewFovModifier() * Mth.lerp(aim, 1f, zoom));
        }
    }

    /** A zoomed view turns slower, by the zoom, so the aim stays steady. */
    @SubscribeEvent
    public static void onTurn(net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent event) {
        var mc = Minecraft.getInstance();
        if (aim <= 0 || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem gun))
            return;
        var held = mc.player.getMainHandItem();
        if (gun.mechanism().built(held))
            event.setMouseSensitivity(event.getMouseSensitivity() * Mth.lerp(aim, 1f, gun.mechanism().sightZoom(held)));
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
        // A pump gun's forend: yanked back, then thrown home (the same beats GunMotion draws).
        if (pumpBackAtMs != 0 && now >= pumpBackAtMs) {
            pumpBackAtMs = 0;
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_PUMP.get(),
                    SoundSource.PLAYERS, 0.8f, 0.85f, false);
        }
        if (pumpHomeAtMs != 0 && now >= pumpHomeAtMs) {
            pumpHomeAtMs = 0;
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_ARMED.get(),
                    SoundSource.PLAYERS, 0.7f, 1.1f, false);
        }
        // A casing hits the floor a moment after a shot that ejected one.
        if (casingAtMs != 0 && now >= casingAtMs) {
            casingAtMs = 0;
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_CASING.get(), SoundSource.PLAYERS,
                    0.4f, 0.9f + player.getRandom().nextFloat() * 0.3f, false);
        }

        var held = player.getMainHandItem();
        var offItem = player.getOffhandItem();
        boolean holding = held.getItem() instanceof GunItem;
        // Drawing a gun, and putting it away: the same leather, higher and lower.
        int slot = player.getInventory().selected;
        if (drawSlot != -1 && (holding != drawHolding || holding && slot != drawSlot))
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_DRAW.get(), SoundSource.PLAYERS,
                    holding ? 0.6f : 0.4f, holding ? 1f : 0.8f, false);
        drawSlot = slot;
        drawHolding = holding;
        // A hot core breech in the hand breaks itself open to vent, however the last stroke ended.
        if (holding && ((GunItem) held.getItem()).mechanism().built(held) && io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun.hot(held))
            io.github.soundgoodizerfan.feedback.client.gun.GunMotion.vent();
        EGunSounds.tick(mc, player, holding ? held : net.minecraft.world.item.ItemStack.EMPTY, aim > 0.5f);
        // The firearm's design (reload, fan, sight read it); an electric gun has none, and is built if its own mechanism says so.
        GunSpec design = holding ? GunSpec.of(held).orElse(null) : null;
        GunSpec offDesign = offItem.getItem() instanceof GunItem ? GunSpec.of(offItem).orElse(null) : null;
        boolean built = holding && ((GunItem) held.getItem()).mechanism().built(held);
        boolean offBuilt = offItem.getItem() instanceof GunItem og && og.mechanism().built(offItem);
        boolean offFires = GunItem.offHandFires(player) && offBuilt;
        boolean ready = mc.screen == null && mc.mouseHandler.isMouseGrabbed() && !player.isSpectator();
        // Barehanded mode: the fire buttons swing the guns and nothing fires; leaving it makes them guns again.
        boolean stance = ready && BareMode.clientOn;
        boolean canAct = built && ready && !stance;
        boolean offCanAct = offFires && ready && !stance;
        // A nose-heavy gun sinks the view while it is held: a recoil the hand fights without a shot being fired.
        if (canAct && !player.isSpectator() && !player.isPassenger()) {
            float sink = io.github.soundgoodizerfan.feedback.weapon.GunBalance.drift(player, held);
            if (sink > 0f) {
                float pitch = Mth.clamp(player.getXRot() + sink * dt / 1000f, -90f, 90f);
                player.setXRot(pitch);
            }
        }
        if (stance) {
            if (built && mc.options.keyAttack.isDown())
                mainSwingMs = bludgeon(player, held, false, mainSwingMs, now);
            if (offFires && mc.options.keyUse.isDown())
                offSwingMs = bludgeon(player, offItem, true, offSwingMs, now);
        }
        // What the next round does in each gun: the client predicts from the same record the server fires.
        GunFigures figures = built ? figuresOf(mc, player, held, 0) : null;
        GunFigures offFigures = offFires ? figuresOf(mc, player, offItem, 1) : null;

        // Aiming eases toward the use key at the sight's own pace, and back out faster. With a pistol
        // on the use key in the off hand, the key is that pistol's and nothing is aimed.
        boolean wantsAim = canAct && mc.options.keyUse.isDown() && !reloading() && !offFires;
        float step = !built ? 1f : dt / (float) Math.max(1, ((GunItem) held.getItem()).mechanism().raiseMs(held));
        aim = Mth.clamp(aim + (wantsAim ? step : -step * 1.5f), 0f, 1f);
        if (wantsAim != aimSent) {
            aimSent = wantsAim;
            if (built && held.getItem() instanceof GunItem aimed)
                aimed.mechanism().aimChanged(held, wantsAim, mc.level.getGameTime());
            PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.AimPayload(wantsAim));
        }

        // R is held, not tapped: the server reloads while it is down and stops when it comes up, both guns if there are two.
        while (RELOAD.consumeClick()) {
        }
        boolean offGunHeld = offBuilt;
        boolean reloadDown = (canAct || (offGunHeld && ready)) && RELOAD.isDown();
        // A pulse gun's R works its fittings (a pump charge's handle; a hold charge is held on it and fires on release); there is nothing to reload.
        boolean pumping = canAct && ((GunItem) held.getItem()).mechanism().reloadKeyWorks(held);
        if (pumping) {
            if (reloadDown && !reloadWasDown) {
                if (((GunItem) held.getItem()).mechanism().reloadKeyReady(held))
                    io.github.soundgoodizerfan.feedback.client.gun.GunMotion.pump();
                PacketDistributor.sendToServer(new GunReloadPayload(true));
            }
        } else if (reloadDown && !reloadWasDown) {
            // The estimate is every held gun's own reload, back to back: the server runs the emptiest first.
            long took = 0;
            for (var pair : new Object[][]{{held, design}, {offItem, offDesign}}) {
                var gun = (net.minecraft.world.item.ItemStack) pair[0];
                var gunDesign = (GunSpec) pair[1];
                if (gunDesign != null)
                    took += gunDesign.internalCapacity().isPresent()
                            ? (long) GunAmmo.loadable(player.getInventory(), gun, gunDesign).size() * GunAmmo.roundMs(gunDesign)
                            : GunAmmo.reloadMs(gun);
            }
            took = io.github.soundgoodizerfan.feedback.core.Haste.time(Math.round(took * GunShots.reloadScale(player)), player);
            if (took > 0) {
                reloadUntilMs = now + took;
                io.github.soundgoodizerfan.feedback.client.gun.GunMotion.reload(reloadUntilMs, design != null && design.internalCapacity().isPresent()
                        ? GunAmmo.loadable(player.getInventory(), held, design).size() : 0);
                PacketDistributor.sendToServer(new GunReloadPayload(true));
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_RELOAD_START.get(),
                        SoundSource.PLAYERS, 0.6f, 1f, false);
                MAIN.burstLeft = 0;
                OFF.burstLeft = 0;
            }
        } else if (!reloadDown && reloadWasDown) {
            reloadUntilMs = 0;
            io.github.soundgoodizerfan.feedback.client.gun.GunMotion.reloadStop();
            PacketDistributor.sendToServer(new GunReloadPayload(false));
        }
        reloadWasDown = reloadDown;
        // Mode and unload go to the main gun; the off-hand one when that is the only gun, or Shift is held.
        boolean keyOff = offGunHeld && (!canAct || player.isShiftKeyDown());
        while (FIRE_MODE.consumeClick()) {
            if (canAct || (offGunHeld && ready)) {
                PacketDistributor.sendToServer(new GunModePayload(keyOff));
                mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_MODE_CLICK.get(),
                        SoundSource.PLAYERS, 0.6f, 1f, false);
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
        // A hold charge is held on the reload key and fires when it is let go; every other gun fires on the attack button.
        boolean holdOnR = built && ((GunItem) held.getItem()).mechanism().fireOnRelease(held);
        pull(mc, player, held, figures, MAIN, canAct, canAct && (holdOnR ? RELOAD.isDown() : mc.options.keyAttack.isDown()), fanning, false, now);
        pull(mc, player, offItem, offFigures, OFF, offCanAct, offCanAct && mc.options.keyUse.isDown(), offFanning, true, now);
        applyKicks(player, now);
    }

    /**
     * One hand's bludgeon swing while its button is down: the swing animation at once (the server plays it for
     * everyone else), then the server resolves the hit. Held repeats at the gun's own mass-derived cooldown.
     * Returns the wall-clock ms the hand may swing again.
     */
    private static long bludgeon(LocalPlayer player, net.minecraft.world.item.ItemStack gun, boolean off, long nextMs, long now) {
        GunFigures f = GunItem.figures(gun);
        if (f == null || now < nextMs)
            return nextMs;
        player.swing(off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        PacketDistributor.sendToServer(new GunSwingPayload(off));
        return now + GunBludgeon.cooldownTicks(f) * 50L;
    }

    /** What the next shot does in {@code gun} as it is now, or null when it cannot fire: the gun's own mechanism predicts it. */
    private static GunFigures figuresOf(Minecraft mc, LocalPlayer player, net.minecraft.world.item.ItemStack gun, int hand) {
        GunMechanism mechanism = GunItem.mechanismOf(gun);
        return mechanism == null ? null : mechanism.predict(player, gun, mc.level, ClientMaterials.on(mc.level), hand);
    }

    /** One hand's trigger: its own fire rhythm, burst and button edge, so two guns do not share a cycle. */
    private static final class Trigger {
        long nextShotMs;
        boolean wasDown;
        int burstLeft;
        /** A shot has left since the button went down: a flywheel's single shot is one a press. */
        boolean spent;
    }

    private static final Trigger MAIN = new Trigger(), OFF = new Trigger();

    /** The fire loop of one gun: its button, its mode, its figures. */
    private static void pull(Minecraft mc, LocalPlayer player, net.minecraft.world.item.ItemStack gun, GunFigures figures,
                             Trigger t, boolean active, boolean down, boolean fanning, boolean off, long now) {
        // A cylinder cannot be fired while its loading stroke runs: the hand is at work in it.
        if (reloading() && GunSpec.of(gun).map(g -> g.layout().feed() == io.github.soundgoodizerfan.feedback.weapon.GunLayout.Feed.CYLINDER).orElse(false)) {
            active = false;
            down = false;
        }
        FireMode mode = GunItem.mode(gun);
        boolean pressed = down && !t.wasDown;
        boolean released = !down && t.wasDown;
        // A gun that reads how long the trigger is held (a flywheel's spin, a hold charge) tells the server when it goes down and comes up.
        GunMechanism mech = GunItem.mechanismOf(gun);
        boolean onRelease = mech != null && active && mech.fireOnRelease(gun);
        if (mech != null && mech.tracksTrigger(gun) && (pressed || released)) {
            PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.PulseTriggerPayload(down, off));
            io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun.setHeld(gun, down, mc.level.getGameTime());
        }
        t.wasDown = down;
        if (pressed && mode == FireMode.BURST && figures != null && !onRelease)
            t.burstLeft = ModularTuning.GUN_BURST_ROUNDS;
        if (!active)
            t.burstLeft = 0;
        if (pressed)
            t.spent = false;
        // A flywheel's single shot waits for the wheel to spin up: it fires once while the trigger is held.
        boolean waits = mech != null && mech.tracksTrigger(gun) && !onRelease;
        boolean wants = onRelease ? released : switch (mode) {
            case AUTO -> down;
            case SEMI -> waits ? down && !t.spent : pressed || (fanning && down);
            case BURST -> t.burstLeft > 0;
        };
        if (!wants)
            return;
        if (figures == null) {
            // A flywheel still spinning up is not a click.
            if (mech != null && mech.tracksTrigger(gun) && !onRelease && down)
                return;
            // Empty, or the wrong bore: a click, not a shot, and not a click every frame.
            if (pressed || released && onRelease || mode == FireMode.AUTO) {
                if (now >= t.nextShotMs) {
                    mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), mech != null ? mech.dryClick() : io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_DRY_CLICK.get(),
                            SoundSource.PLAYERS, 0.6f, 0.95f + player.getRandom().nextFloat() * 0.1f, false);
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
            io.github.soundgoodizerfan.feedback.client.gun.GunMotion.fanning(off, fanning);
            shoot(mc, player, gun, figures, off, now);
            t.spent = true;
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
        // A flintlock's priming sometimes flashes in the pan: a click and a puff, the charge stays loaded, and the pull still costs its cycle.
        if (GunSpec.of(gun).map(g -> g.layout().action() == io.github.soundgoodizerfan.feedback.weapon.GunLayout.Action.FLINTLOCK).orElse(false)
                && player.getRandom().nextFloat() < ClientTuning.GUN_FLINT_MISFIRE) {
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PAN_FLASH.get(), SoundSource.PLAYERS, 0.8f, 1f, false);
            Vec3 pan = player.getEyePosition().add(player.getLookAngle().scale(0.5));
            mc.level.addParticle(ParticleTypes.SMOKE, pan.x, pan.y, pan.z, 0, 0.02, 0);
            GunHud.cue(net.minecraft.network.chat.Component.translatable("message.feedback.gun.flash_pan"), 40);
            io.github.soundgoodizerfan.feedback.client.gun.GunMotion.shot(off, f.cycleMs(), 0.5f);
            return;
        }
        PacketDistributor.sendToServer(new GunFirePayload(player.getYRot(), player.getXRot(), aim > 0.5f, off));
        reloadUntilMs = 0; // a pull ends a reload; the server does the same
        io.github.soundgoodizerfan.feedback.client.gun.GunMotion.reloadStop();
        io.github.soundgoodizerfan.feedback.client.gun.GunMotion.shot(off, f.cycleMs(), f.kickDeg());

        GunMechanism mechanism = GunItem.mechanismOf(gun);
        for (GunSound.Layer layer : mechanism != null ? mechanism.sound(gun, f, player.getRandom()) : GunSound.layers(f, player.getRandom()))
            mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), layer.event(),
                    SoundSource.PLAYERS, layer.volume(), layer.pitch(), false);
        if (mechanism instanceof io.github.soundgoodizerfan.feedback.weapon.FirearmMechanism
                && GunSpec.of(gun).map(g -> g.layout().action() != io.github.soundgoodizerfan.feedback.weapon.GunLayout.Action.FLINTLOCK).orElse(false))
            casingAtMs = now + 250 + player.getRandom().nextInt(250);
        var pumpAction = GunSpec.of(gun).map(g -> g.layout().action()).filter(a -> a == io.github.soundgoodizerfan.feedback.weapon.GunLayout.Action.PUMP);
        if (pumpAction.isPresent()) {
            int stroke = pumpAction.get().manualMs();
            pumpBackAtMs = now + Math.round(stroke * 0.4f);
            pumpHomeAtMs = now + Math.round(stroke * 0.9f);
        }
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        mc.level.addParticle(mechanism != null ? mechanism.muzzleParticle(gun, f) : f.muzzleParticle(), muzzle.x, muzzle.y, muzzle.z, 0, 0.01, 0);
        // A hot barrel smokes between shots as well as at them: the thing a hand and an eye can see.
        float heat = GunCondition.heatFraction(gun, mc.level, FTuning.GUN_PROPELLANT_IGNITION_TU);
        for (int i = 0; i < (int) (heat * 4); i++)
            mc.level.addParticle(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    (player.getRandom().nextFloat() - 0.5) * 0.02, 0.03, (player.getRandom().nextFloat() - 0.5) * 0.02);

        // A core breech vents itself after the shot (the server runs the same stroke).
        var pulse = io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun.specOf(gun);
        if (pulse != null && pulse.seat() == io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec.Seat.CORE_BREECH)
            io.github.soundgoodizerfan.feedback.client.gun.GunMotion.vent();
        kick(player, f.kickDeg(), f.cycleMs(), now);
    }

    /** A shot the server fired for this player (a cook-off): the gun and the view kick as for one of their own. */
    public static void serverKick(boolean off, int cycleMs, float kickDeg) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return;
        io.github.soundgoodizerfan.feedback.client.gun.GunMotion.shot(off, cycleMs, kickDeg);
        kick(player, kickDeg, cycleMs, System.currentTimeMillis());
    }

    private static void kick(LocalPlayer player, float kickDeg, int cycleMs, long now) {
        // Recoil climbs over a streak and settles after a pause; aiming takes the edge off it.
        streak = now - lastShotMs < cycleMs * 5L / 2 ? Math.min(streak + 1, ClientTuning.GUN_KICK_CLIMB_MAX) : 0;
        lastShotMs = now;
        float climb = 1 + ClientTuning.GUN_KICK_CLIMB_PER_SHOT * streak;
        float aimed = Mth.lerp(aim, 1f, GunTuning.GUN_ADS_KICK);
        // The kick has a noise floor (philosophy 10): the amplitude is the gun's, the instance is not.
        float noise = 0.75f + player.getRandom().nextFloat() * 0.5f;
        float yawNoise = (player.getRandom().nextFloat() - 0.5f) * 0.6f;
        float steady = io.github.soundgoodizerfan.feedback.potion.SteadyHand.factor(player);
        KICKS.add(new Kick(now, kickDeg * noise * climb * aimed * steady, kickDeg * yawNoise * aimed * steady));
    }

    /** Land each kick over its time with an ease-out, by the part of it this frame adds. */
    private static void applyKicks(LocalPlayer player, long now) {
        for (Iterator<Kick> it = KICKS.iterator(); it.hasNext(); ) {
            Kick k = it.next();
            float t = Mth.clamp((now - k.startMs) / (float) ClientTuning.GUN_KICK_MS, 0f, 1f);
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
