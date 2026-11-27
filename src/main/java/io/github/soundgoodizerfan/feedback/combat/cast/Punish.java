// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Punish: a blow that only lands on someone already knocked off their swing. */
final class Punish {

    private Punish() {
    }

    /** Punish: a heavy blow, more than double the weapon's damage, on the nearest thing in reach that is staggered; nothing else will take it. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof GunItem)
            return false;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity t = null;
        double best = 16;
        for (LivingEntity e : near(p, eye.add(look.scale(1.8)), 3)) {
            double d = e.distanceToSqr(p);
            if (CombatState.of(e).phase() == CombatPhase.STAGGER && d < best && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.3) {
                best = d;
                t = e;
            }
        }
        if (t == null)
            return false;
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage * 2.2f);
        stagger(p, t, new Work(damage * 2f * 10f));
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "one_handed_stab", CastFx.MAIN, 12, 3);
        ServerLevel lv = p.serverLevel();
        Vec3 c = t.getBoundingBox().getCenter();
        CastFx.streak(lv, p.getEyePosition().add(0, -0.3, 0), c, CastFx.CRIMSON, 0.04f, 4);
        CastFx.shards(lv, c, CastFx.CRIMSON, 1.8f, 8);
        CastFx.pulse(lv, t.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.CRIMSON, 2.5f, 8);
        CastFx.sound(lv, c, FSounds.CAST_PUNCH.get(), 1f, 0.8f);
        CastFx.sound(lv, c, FSounds.CAST_SLAM.get(), 0.6f, 1.3f);
        return true;
    }
}
