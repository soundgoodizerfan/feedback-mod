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
package io.github.soundgoodizerfan.feedback.combat.grab;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.kick.Kicking;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.combat.tether.Tethers;
import io.github.soundgoodizerfan.feedback.modular.ModularGauntlet;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The grab (implementation plan 0, "Grapple"; 2.5, "Grab"): the third {@link Tether} source. A player
 * wearing a glove (a {@link GloveItem} in the Curios {@code hands} slot -- the source the ruling asks
 * for) presses the grab key at something within {@link GrabTuning#REACH}; the target gets a short rope
 * anchored on the grabber, no reel, a modest vulnerability. While held it is pinned within the rope
 * and cannot parry or roll (kept in the stagger phase). The hold ends on the key again, on
 * {@link GrabTuning#MAX_TICKS}, on distance, on either end dying or leaving, or on the grabber taking one
 * hit of {@link GrabTuning#BREAK_DAMAGE}. A body much heavier than the grabber cannot be held at all:
 * the rope takes the grabber instead. Server only; the rope is drawn by {@code TetherRopes} unchanged.
 * One hold per grabber; the tether's source id is the grabber's uuid.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Grabbing {

    private record Hold(UUID target, int ticks) {
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    private Grabbing() {
    }

    /** How long a hold lasts: the base, the bought grip nodes, and a glove's better purchase. */
    private static int holdTicks(Player player) {
        return GrabTuning.MAX_TICKS + Skills.grabHoldTicks(player) + (gloved(player) ? GrabTuning.GLOVE_HOLD : 0);
    }

    /** Whether {@code player} wears a glove: it improves the grab (longer hold, heavier targets), but the {@code grab} skill is what allows it. */
    public static boolean gloved(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(s -> s.getItem() instanceof ModularGauntlet)).isPresent();
    }

    /** Whether {@code player} is holding something now (server). */
    public static boolean holding(Player player) {
        return !player.level().isClientSide && HOLDS.containsKey(player.getUUID());
    }

    /** The body {@code player} is holding now, or null. */
    public static LivingEntity held(ServerPlayer player) {
        Hold h = HOLDS.get(player.getUUID());
        return h == null ? null : find(player.server, h.target());
    }

    /** Lets go of whatever {@code player} holds, without a throw; returns the body that was held, or null. */
    public static LivingEntity drop(ServerPlayer player) {
        LivingEntity held = held(player);
        release(player.server, player.getUUID());
        return held;
    }

    /** A grab press: lets go if holding, otherwise tries to take hold. */
    public static void press(ServerPlayer player) {
        if (HOLDS.containsKey(player.getUUID())) {
            Hold h = HOLDS.get(player.getUUID());
            LivingEntity held = find(player.server, h.target());
            release(player.server, player.getUUID());
            if (held != null)
                Throwing.attempt(player, held);
            return;
        }
        if (!Skills.unlocked(player, "grab") || !CombatState.of(player).ready() || player.isPassenger() || player.isSpectator()) {
            denied(player);
            return;
        }
        var picked = Kicking.pick(player, player.getEyePosition(), player.getLookAngle(), GrabTuning.REACH);
        if (picked.isEmpty()) {
            denied(player);
            return;
        }
        LivingEntity target = picked.get();
        if (BodyMass.of(target) > BodyMass.of(player) * GrabTuning.MASS_RATIO * (gloved(player) ? GrabTuning.GLOVE_MASS : 1f)) {
            Vec3 toward = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            player.setDeltaMovement(toward.scale(GrabTuning.YANK).add(0, 0.1, 0));
            player.hurtMarked = true;
            CombatCues.everyone(player, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 0.7f);
            return;
        }
        Tethers.add(target, new Tether(player.getUUID(), new Tether.Anchor.OnEntity(player.getId()), GrabTuning.ROPE, 0f,
                GrabTuning.VULNERABILITY, new Tether.Release(0, holdTicks(player))));
        HOLDS.put(player.getUUID(), new Hold(target.getUUID(), 0));
        CombatCues.everyone(player, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 1.3f);
    }

    private static void denied(ServerPlayer player) {
        player.playNotifySound(FSounds.PRESS_DENIED.get(), SoundSource.PLAYERS, FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
    }

    private static LivingEntity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels())
            if (level.getEntity(id) instanceof LivingEntity e)
                return e;
        return null;
    }

    /** Ends {@code grabber}'s hold, taking the rope off the target; its stagger runs out in two ticks. */
    private static void release(MinecraftServer server, UUID grabber) {
        Hold h = HOLDS.remove(grabber);
        if (h != null && find(server, h.target()) instanceof LivingEntity target)
            Tethers.remove(target, grabber);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (HOLDS.isEmpty())
            return;
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, Hold>> it = HOLDS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Hold h = entry.getValue();
            ServerPlayer grabber = server.getPlayerList().getPlayer(entry.getKey());
            LivingEntity target = grabber == null ? null : grabber.serverLevel().getEntity(h.target()) instanceof LivingEntity l ? l : null;
            if (target == null || !target.isAlive() || !grabber.isAlive() || h.ticks() >= holdTicks(grabber)
                    || grabber.distanceTo(target) > GrabTuning.BREAK_DISTANCE) {
                it.remove();
                LivingEntity held = target != null ? target : find(server, h.target());
                if (held != null)
                    Tethers.remove(held, entry.getKey());
                continue;
            }
            entry.setValue(new Hold(h.target(), h.ticks() + 1));
            hold(grabber, target);
        }
    }

    /** Pins {@code target} within the rope of {@code grabber} and keeps it from acting. */
    private static void hold(ServerPlayer grabber, LivingEntity target) {
        Vec3 offset = grabber.position().add(0, grabber.getBbHeight() * 0.5, 0)
                .subtract(target.position().add(0, target.getBbHeight() * 0.5, 0));
        double dist = offset.length();
        Vec3 motion = target.getDeltaMovement();
        if (dist > GrabTuning.ROPE)
            motion = offset.normalize().scale(Math.min(GrabTuning.PULL, dist - GrabTuning.ROPE + 0.05)).add(0, motion.y, 0);
        else
            motion = new Vec3(0, motion.y, 0);
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
        if (CombatState.of(target).phase() != CombatPhase.ROLL)
            CombatTicker.stagger(target, 2, ParryTuning.STAGGER_SLOW);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() >= GrabTuning.BREAK_DAMAGE && event.getEntity() instanceof ServerPlayer p && HOLDS.containsKey(p.getUUID()))
            release(p.server, p.getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        HOLDS.clear();
    }
}
