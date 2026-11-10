// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.kick;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Dropkick's carry: a creature struck by the diving kicker is staggered and taken along, kept at the same
 * offset from the kicker's body (pulled there by velocity each tick, so walls and the ground still stop it),
 * until the kicker lands; the landing then hits it with everything else under the kicker ({@code Dropkick}). A
 * target much heavier than the kicker is not carried: the dive bounces the kicker off it. Player and mob
 * targets are carried the same. Server only; one carry per kicker.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Carrying {

    private static final class Carry {
        final UUID target;
        final Vec3 offset;
        int ticks;

        Carry(UUID target, Vec3 offset) {
            this.target = target;
            this.offset = offset;
        }
    }

    private static final Map<UUID, Carry> CARRIES = new HashMap<>();

    private Carrying() {
    }

    /** Whether {@code player} is carrying someone now (server). */
    public static boolean carrying(Player player) {
        return !player.level().isClientSide && CARRIES.containsKey(player.getUUID());
    }

    /**
     * Tries to carry {@code target}, which the diving {@code kicker} has just struck while moving along {@code dir}. A body
     * too heavy to carry bounces the kicker back instead.
     *
     * @return {@code 1} if the target is now carried, {@code -1} if the kicker bounced off it, {@code 0} if nothing happened
     */
    public static int begin(ServerPlayer kicker, LivingEntity target, Vec3 dir) {
        if (CARRIES.containsKey(kicker.getUUID()))
            return 0;
        float mine = BodyMass.of(kicker) + BodyMass.armour(kicker);
        if (BodyMass.of(target) + BodyMass.armour(target) > mine * KickTuning.CARRY_MASS_RATIO) {
            kicker.setDeltaMovement(dir.scale(-KickTuning.CARRY_REBOUND).add(0, 0.2, 0));
            kicker.hurtMarked = true;
            return -1;
        }
        double apart = kicker.getBbWidth() / 2 + target.getBbWidth() / 2;
        Vec3 offset = dir.scale(apart);
        CARRIES.put(kicker.getUUID(), new Carry(target.getUUID(), offset));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        CombatTicker.stagger(target, 2, ParryTuning.STAGGER_SLOW);
        return 1;
    }

    private static LivingEntity find(ServerPlayer kicker, UUID id) {
        return kicker.serverLevel().getEntity(id) instanceof LivingEntity l ? l : null;
    }

    /** Ends {@code kicker}'s carry; the target is left staggered for at least {@link KickTuning#CARRY_STAGGER_MIN} ticks. */
    public static void end(ServerPlayer kicker) {
        Carry c = CARRIES.remove(kicker.getUUID());
        LivingEntity target = c == null ? null : find(kicker, c.target);
        if (target != null && target.isAlive())
            CombatTicker.stagger(target, KickTuning.CARRY_STAGGER_MIN, ParryTuning.STAGGER_SLOW);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (CARRIES.isEmpty())
            return;
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, Carry>> it = CARRIES.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Carry c = entry.getValue();
            ServerPlayer kicker = server.getPlayerList().getPlayer(entry.getKey());
            LivingEntity target = kicker == null ? null : find(kicker, c.target);
            if (target == null || !target.isAlive() || !kicker.isAlive()) {
                it.remove();
                continue;
            }
            c.ticks++;
            Vec3 want = kicker.position().add(c.offset);
            Vec3 pull = want.subtract(target.position());
            if (c.ticks >= KickTuning.CARRY_MAX_TICKS || pull.length() > KickTuning.CARRY_BREAK_DISTANCE || kicker.isPassenger()
                    || kicker.isInWater() || kicker.getAbilities().flying || (c.ticks > 1 && kicker.onGround())) {
                it.remove();
                CombatTicker.stagger(target, KickTuning.CARRY_STAGGER_MIN, ParryTuning.STAGGER_SLOW);
                continue;
            }
            if (pull.length() > KickTuning.CARRY_MAX_PULL)
                pull = pull.normalize().scale(KickTuning.CARRY_MAX_PULL);
            target.setDeltaMovement(pull);
            target.hurtMarked = true;
            target.fallDistance = 0f;
            if (CombatState.of(target).phase() != CombatPhase.ROLL)
                CombatTicker.stagger(target, 2, ParryTuning.STAGGER_SLOW);
        }
    }

    /** A hard hit on the kicker knocks the target loose. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() >= KickTuning.CARRY_BREAK_DAMAGE && event.getEntity() instanceof ServerPlayer p && CARRIES.containsKey(p.getUUID()))
            end(p);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        CARRIES.clear();
    }
}
