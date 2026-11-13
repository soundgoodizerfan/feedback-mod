// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.hud;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Read the draw (Observation): a trained eye reads the stance of a creature drawing a bow or crossbow, or a mob holding a
 * gun with its eyes on someone: while it does, and only with a clear line of sight to it, a small spark flickers above its
 * head. Nothing glows and nothing shows through a wall; the cue is a client particle only you see.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class DrawGlow {

    private static final double RANGE = 48;

    private DrawGlow() {
    }

    /** Whether {@code e} is, right now, drawing a bow, loading a crossbow or leveling a gun. */
    static boolean aiming(LivingEntity e) {
        if (e.isUsingItem() && e.getUseItem().getItem() instanceof ProjectileWeaponItem)
            return true;
        return e instanceof Mob m && m.isAggressive() && e.getMainHandItem().getItem() instanceof GunItem;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.player.tickCount % 4 != 0
                || !Skills.unlocked(mc.player, "read_the_draw"))
            return;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE),
                e -> e != mc.player && e.isAlive() && aiming(e) && mc.player.hasLineOfSight(e)))
            mc.level.addParticle(ParticleTypes.CRIT, e.getX(), e.getEyeY() + 0.5, e.getZ(), 0, 0.05, 0);
    }
}
