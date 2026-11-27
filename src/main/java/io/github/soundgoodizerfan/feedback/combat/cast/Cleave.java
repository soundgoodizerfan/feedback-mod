// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Cleave: an overhead blow whose weight travels on through the ground. */
final class Cleave {

    private Cleave() {
    }

    /** Cleave: raise the weapon, bring it down; the target takes the blow and a crack runs seven blocks on, staggering by the weapon's damage and fading with distance. Needs a heavy weapon (damage 5 or more). */
    static boolean act(ServerPlayer p) {
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (p.getMainHandItem().getItem() instanceof GunItem || damage < 5f)
            return false;
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, 0.6, 0);
        CastFx.gather(p.serverLevel(), hand, CastFx.STEEL, 1.2f, 6);
        CastFx.sound(p.serverLevel(), hand, FSounds.CAST_WHOOSH.get(), 0.8f, 0.8f);
        CastFx.clip(p, "two_handed_slash_vertical_right", CastFx.TWO, 18, 6);
        later(p, 6, () -> {
            if (!p.isAlive())
                return;
            p.swing(InteractionHand.MAIN_HAND, true);
            ServerLevel lv = p.serverLevel();
            Vec3 look = p.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1e-6)
                return;
            Vec3 d = look.normalize(), start = p.position().add(d.scale(1.5));
            LivingEntity t = ahead(p, 3.4, 0.5);
            if (t != null) {
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().playerAttack(p), damage * 1.3f);
            }
            for (LivingEntity e : near(p, start.add(d.scale(3.5)), 4.2)) {
                Vec3 off = e.position().subtract(start);
                double along = off.dot(d), side = off.subtract(d.scale(along)).multiply(1, 0, 1).length();
                if (along < 0 || along > 7 || side > 1.4 || e == t)
                    continue;
                stagger(p, e, new Work(damage * ParryTuning.WORK_PER_DAMAGE * (float) (1 - along / 9)));
            }
            CastFx.streak(lv, start.add(0, 0.1, 0), start.add(d.scale(7)).add(0, 0.1, 0), CastFx.EMBER, 0.06f, 6);
            CastFx.slash(lv, start.add(0, 1, 0), d, CastFx.STEEL, 2f, 8);
            CastFx.column(lv, start, CastFx.EMBER, 2f, 8);
            CastFx.pulse(lv, start.add(0, 0.1, 0), Vec3.ZERO, CastFx.EMBER, 2.5f, 8);
            CastFx.sound(lv, start, FSounds.CAST_SLAM.get(), 1f, 0.8f);
            CastFx.sound(lv, start, FSounds.CAST_SLASH.get(), 0.7f, 0.8f);
        });
        return true;
    }
}
