// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.combat.tether.Tethers;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Shake off (Proficiency): a hard full-body shake that throws off what a body can shake: fire (a {@link Cauterize}, a
 * hot-contact sear, a flame), dirt in the eyes (blindness), anything sticking to the limbs (slowness), a daze (nausea) and
 * a stun (stagger). Never a systemic effect: poison, wither, hunger and the like stay. With Break Free it first throws off
 * anything holding you (a grab, a hook), staggering whoever held the rope by {@link #BREAK_JOULES} against their mass.
 * Refused with nothing to shake. Rearms on a stagger: you earn the next one by pushing back.
 */
final class ShakeOff {

    /** STAND-IN: the shake's impact on a holder; a kick-sized jolt. */
    static final float BREAK_JOULES = 300f;

    private ShakeOff() {
    }

    static boolean act(ServerPlayer p) {
        if (!(Skills.unlocked(p, "break_free") && freeOf(p, p))) {
            if (!shake(p))
                return false;
        }
        Vec3 at = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 14, 0.4, 0.5, 0.4, 0.05);
        CastFx.sound(p.serverLevel(), at, SoundEvents.WOLF_SHAKE, 1f, 0.8f);
        return true;
    }

    /** The effects a body can shake: dirt in the eyes, a sticky drag on the limbs, a daze. */
    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> SHAKEABLE = java.util.List.of(
            MobEffects.BLINDNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.CONFUSION, MobEffects.DARKNESS);

    /** Throws off fire, the shakeable effects and a stun; false if there was nothing to shake. */
    static boolean shake(LivingEntity who) {
        boolean shook = false;
        if (who.isOnFire()) {
            who.clearFire();
            shook = true;
        }
        for (var effect : SHAKEABLE)
            shook |= who.removeEffect(effect);
        if (CombatState.of(who).phase() == CombatPhase.STAGGER) {
            who.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(CombatTicker.STAGGER_ID);
            CombatState.set(who, new CombatState(CombatPhase.IDLE, 0, 0, CombatState.of(who).iFrames()));
            shook = true;
        }
        return shook;
    }

    /** Break Free: drops every rope on {@code p}, jolting whoever held one (credited to {@code by}). False if nothing held it. */
    static boolean freeOf(ServerPlayer p, ServerPlayer by) {
        if (!p.hasData(FCombat.TETHERS))
            return false;
        // your own grapple's rope is yours to keep: only ropes someone else put on you
        var ropes = p.getData(FCombat.TETHERS).all().stream()
                .filter(t -> !(p.serverLevel().getEntity(t.sourceId()) instanceof Projectile own && own.getOwner() == p))
                .toList();
        if (ropes.isEmpty())
            return false;
        for (Tether t : ropes) {
            if (t.anchor() instanceof Tether.Anchor.OnEntity on && p.level().getEntity(on.entityId()) instanceof LivingEntity holder) {
                if (holder instanceof ServerPlayer grabber && Grabbing.held(grabber) == p)
                    Grabbing.drop(grabber);
                Casts.stagger(by, holder, BREAK_JOULES);
            }
            Tethers.remove(p, t.sourceId());
        }
        return true;
    }
}
