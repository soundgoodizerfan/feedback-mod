// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.state;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * A staggered creeper cannot explode: its fuse is cancelled the moment the stagger lands and cannot start while it lasts (the swell is held at
 * nothing and a lit one is put out at the start of every tick, before the creeper's own goals and swell count run, so it never reaches the end of
 * a fuse). Once the stagger ends it may light again as any creeper does.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class StaggerFuse {

    private StaggerFuse() {
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Creeper creeper) || !creeper.hasData(FCombat.STATE)
                || CombatState.of(creeper).phase() != CombatPhase.STAGGER)
            return;
        if (creeper.swell > 0)
            creeper.swell = 0;
        creeper.setSwellDir(-1);
        if (creeper.isIgnited())
            creeper.getEntityData().set(Creeper.DATA_IS_IGNITED, false);
    }
}
