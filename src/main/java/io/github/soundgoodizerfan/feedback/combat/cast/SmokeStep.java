// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Smoke Step: the roll kicks up dust or snow from the ground you stand on, a small blinding puff that mobs lose you in. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class SmokeStep {

    private SmokeStep() {
    }

    private record Cloud(UUID owner, ServerLevel level, Vec3 at, double radius, long until, BlockState ground) {
    }

    private static final List<Cloud> CLOUDS = new ArrayList<>();

    /** Ground this soft or softer (destroy speed) throws a full cloud; harder ground kicks up only a fraction of it. */
    private static final float LOOSE_HARDNESS = 0.7f;

    /** Smoke step: the roll kicks a puff off the ground you stand on; anything inside it loses you, unless it is already at your elbow. Loose ground (sand, dirt, snow) throws more. */
    static boolean act(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        BlockState ground = p.level().getBlockState(p.blockPosition().below());
        if (p.isPassenger() || look.lengthSqr() < 1e-6 || !p.onGround() || ground.isAir())
            return false;
        boolean thick = Skills.unlocked(p, "thick_smoke");
        float loose = ground.getDestroySpeed(p.level(), p.blockPosition().below()) <= LOOSE_HARDNESS ? 1f : CastTuning.HARD_SHARE;
        double radius = (CastTuning.CLOUD_RADIUS + (thick ? CastTuning.CLOUD_THICK_RADIUS : 0)) * (loose == 1f ? CastTuning.LOOSE_RADIUS : loose);
        int ticks = Math.round((CastTuning.CLOUD_TICKS + (thick ? CastTuning.CLOUD_THICK_TICKS : 0)) * (loose == 1f ? CastTuning.LOOSE_TICKS : loose));
        Cloud c = new Cloud(p.getUUID(), p.serverLevel(), p.position().add(0, 0.5, 0), radius, p.level().getGameTime() + ticks, ground);
        CLOUDS.add(c);
        Vec3 d = look.normalize();
        p.setDeltaMovement(d.x * 0.9, 0.15, d.z * 0.9);
        p.hurtMarked = true;
        c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.at().x, c.at().y, c.at().z,
                (int) (30 * radius), c.radius() * 0.4, 0.3, c.radius() * 0.4, 0.05);
        CastFx.sound(c.level(), c.at(), ground.getSoundType().getBreakSound(), 1f, 0.6f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.9f);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CLOUDS.isEmpty())
            return;
        for (Iterator<Cloud> it = CLOUDS.iterator(); it.hasNext(); ) {
            Cloud c = it.next();
            if (c.level().getGameTime() >= c.until()) {
                it.remove();
                continue;
            }
            if (c.level().getGameTime() % 2 == 0) // a billow of the ground's own dust, thinning as it runs down
                c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, c.ground()), c.at().x, c.at().y + 0.5, c.at().z, 4, c.radius() * 0.35, 0.5, c.radius() * 0.35, 0.01);
            var owner = c.level().getPlayerByUUID(c.owner());
            for (Mob m : c.level().getEntitiesOfClass(Mob.class, new AABB(c.at(), c.at()).inflate(c.radius()), Mob::isAlive))
                if (m.getTarget() != null && m.getTarget().getUUID().equals(c.owner()) && (owner == null || m.distanceToSqr(owner) > 2.25)) {
                    m.setTarget(null);
                    m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false)); // dust in the eyes
                }
        }
    }
}
