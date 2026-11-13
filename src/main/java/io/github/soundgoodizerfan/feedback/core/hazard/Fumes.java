// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a hot toxic material gives off: working leaded solder poisons whoever is
 * within reach of it while it is hot. Read off the material's {@code toxic} perk, so lead-free
 * solder and any modded clean alloy give off nothing and a modded toxic one does. Poison never
 * kills. This sits beside {@link Exposure}, which is the shock multiplier and has no toxin path.
 */
public final class Fumes {

    private Fumes() {
    }

    /** Poison level of a material: its {@code toxic} perk, 0 if it has none. */
    public static int level(Material material) {
        return material.perks().getOrDefault(Material.Perk.TOXIC, 0);
    }

    /** Dose everything living within {@link HazardTuning#FUME_RADIUS_BLOCKS} of {@code at}. Server side. */
    public static void release(Level level, Vec3 at, int toxic) {
        if (toxic <= 0 || level.isClientSide)
            return;
        float r = HazardTuning.FUME_RADIUS_BLOCKS;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r)))
            if (victim.position().distanceTo(at) <= r) {
                float share = io.github.soundgoodizerfan.feedback.combat.skill.Skills.fumeShare(victim);
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, Math.round(HazardTuning.FUME_POISON_TICKS_PER_LEVEL * toxic * share)));
                if (victim instanceof net.minecraft.server.level.ServerPlayer p)
                    io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.hazardHandled(p, "fume", 1f - share);
            }
    }
}
