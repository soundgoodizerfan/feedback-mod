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

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.Comparator;

import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.combat.tether.Tethers;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Shake off (Proficiency): a hard full-body shake. If you are burning (a {@link Cauterize}, a hot-contact sear, a
 * flame), it beats the fire out; otherwise it clears the one harmful effect with the most time left on it. One thing
 * per cast, by category, never a named effect. With Break Free it first throws off anything holding you (a grab, a
 * hook), staggering whoever held the rope by {@link #BREAK_JOULES} against their mass. Refused with nothing to shake.
 * Rearms on a stagger: you earn the next one by pushing back.
 */
final class ShakeOff {

    /** STAND-IN: the shake's impact on a holder; a kick-sized jolt. */
    static final float BREAK_JOULES = 300f;

    private ShakeOff() {
    }

    static boolean act(ServerPlayer p) {
        if (!(Skills.unlocked(p, "break_free") && breakFree(p))) {
            if (p.isOnFire())
                p.clearFire();
            else if (!clearWorst(p))
                return false;
        }
        Vec3 at = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 14, 0.4, 0.5, 0.4, 0.05);
        CastFx.sound(p.serverLevel(), at, SoundEvents.WOLF_SHAKE, 1f, 0.8f);
        return true;
    }

    /** Clears the harmful effect with the most time left; false if there is none. */
    static boolean clearWorst(LivingEntity who) {
        MobEffectInstance worst = who.getActiveEffects().stream()
                .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .max(Comparator.comparingInt(MobEffectInstance::getDuration)).orElse(null);
        return worst != null && who.removeEffect(worst.getEffect());
    }

    /** Break Free: drops every rope on {@code p}, jolting whoever held one. False if nothing held it. */
    private static boolean breakFree(ServerPlayer p) {
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
                Casts.stagger(p, holder, BREAK_JOULES);
            }
            Tethers.remove(p, t.sourceId());
        }
        return true;
    }
}
