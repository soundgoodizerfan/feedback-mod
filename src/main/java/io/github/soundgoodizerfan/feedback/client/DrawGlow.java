// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Read the draw (Observation): a creature drawing a bow or crossbow, or a mob holding a gun with its eyes on someone,
 * glows for you while it does. Only on your screen: it sets the outline on the entity's client copy and takes it
 * off the moment the creature stops. A glow the creature already had is never removed.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class DrawGlow {

    private static final double RANGE = 48;
    private static final Set<Integer> MINE = new HashSet<>();

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
        if (mc.level == null || mc.player == null || mc.isPaused())
            return;
        boolean on = Skills.unlocked(mc.player, "read_the_draw");
        Set<Integer> now = new HashSet<>();
        if (on)
            for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE),
                    e -> e != mc.player && e.isAlive() && aiming(e)))
                now.add(e.getId());
        for (int id : new HashSet<>(MINE))
            if (!now.contains(id)) {
                if (mc.level.getEntity(id) != null)
                    mc.level.getEntity(id).setGlowingTag(false);
                MINE.remove(id);
            }
        for (int id : now) {
            var e = mc.level.getEntity(id);
            if (e != null && !MINE.contains(id) && !e.isCurrentlyGlowing()) {
                e.setGlowingTag(true);
                MINE.add(id);
            }
        }
    }
}
