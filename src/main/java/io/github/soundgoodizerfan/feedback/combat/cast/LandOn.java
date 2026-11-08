// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Land on: a fall that ends on a creature lands on it, not the ground. Each creature landed on takes the fall by the
 * landing body's share of the two masses (armour counts) and is staggered by the drop's energy, and the lander takes
 * {@link #SOFTEN} less of it. Bare, only the body underfoot is landed on; sabatons ({@code trample}) widen it to
 * everything within their radius and add their weight ({@link #land}). The same fall figure as Dropkick's, with no
 * cast and no dive.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class LandOn {

    /** STAND-IN: blocks of fall below which nothing lands hard; share of the lander's fall damage the body underneath takes off. */
    static final float MIN_FALL = 2f, SOFTEN = 0.5f;

    private LandOn() {
    }

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        LivingEntity p = event.getEntity();
        // Sabatons land on their own terms (Trample), once.
        if (p.level().isClientSide || event.getDistance() < MIN_FALL || ModularTraits.worn(p).stream().anyMatch(w -> w.id().equals("trample")))
            return;
        if (land(p, event.getDistance(), 0, 0))
            event.setDamageMultiplier(event.getDamageMultiplier() * (1f - SOFTEN));
    }

    /**
     * Lands {@code lander} from {@code distance} blocks on every creature (not allies) under its feet, or within
     * {@code radius} of them when that is above 0, with {@code extraMu} of weight on top of its body and armour.
     *
     * @return whether anything was landed on
     */
    public static boolean land(LivingEntity lander, float distance, double radius, float extraMu) {
        AABB feet = radius > 0 ? lander.getBoundingBox().inflate(radius, 0.5, radius) : lander.getBoundingBox().move(0, -0.5, 0);
        float mine = BodyMass.of(lander) + BodyMass.armour(lander) + extraMu;
        boolean any = false;
        for (LivingEntity under : lander.level().getEntitiesOfClass(LivingEntity.class, feet,
                e -> e != lander && e.isAlive() && !e.isSpectator() && !lander.isAlliedTo(e))) {
            float theirs = BodyMass.of(under) + BodyMass.armour(under);
            under.invulnerableTime = 0;
            under.hurt(lander.damageSources().fall(), (distance - 1f) * mine / (mine + theirs));
            int ticks = ParryTuning.staggerTicks(mine * 9.8f * distance, theirs);
            if (ticks > 0)
                CombatTicker.stagger(under, ticks, ParryTuning.STAGGER_SLOW);
            any = true;
        }
        return any;
    }
}
