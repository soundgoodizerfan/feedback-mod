// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/** Hamstring: a low cut that slows hard. */
final class Hamstring {

    private Hamstring() {
    }

    /** Hamstring: a low cut at what you face, for most of the weapon's damage, that slows it hard so you pick the range. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof GunItem)
            return false;
        LivingEntity t = ahead(p, 3.6, 0.6);
        if (t == null)
            return false;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f);
        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "one_handed_slash_switch_blade_right", CastFx.MAIN, 12, 3);
        ServerLevel lv = p.serverLevel();
        Vec3 side = p.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.9), low = t.position().add(0, 0.4, 0);
        CastFx.streak(lv, low.add(side), low.subtract(side).add(0, -0.2, 0), CastFx.CRIMSON, 0.03f, 5);
        CastFx.slash(lv, low, p.getLookAngle(), CastFx.CRIMSON, 1f, 7);
        CastFx.shards(lv, low, CastFx.CRIMSON, 0.6f, 5);
        CastFx.pulse(lv, t.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.ICE, 0.8f, 8);
        CastFx.sound(lv, low, FSounds.CAST_SLASH.get(), 1f, 0.8f);
        return true;
    }
}
