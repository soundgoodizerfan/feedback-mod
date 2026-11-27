// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.kick.Carrying;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Dropkick: dive forward and down; the landing is a kick on what is under it. A creature struck in the dive is carried to the landing ({@link Carrying}). */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Dropkick {

    private Dropkick() {
    }

    private static final java.util.Set<java.util.UUID> DIVING = new java.util.HashSet<>();
    /** Divers that already bounced off something too heavy to carry: they do not bounce twice in one dive. */
    private static final java.util.Set<java.util.UUID> BOUNCED = new java.util.HashSet<>();

    /** Dropkick: in the air, dive forward and down; the landing is a kick on whatever is under it, by your mass over its. */
    static boolean act(ServerPlayer p) {
        if (p.onGround() || p.isPassenger())
            return false;
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-6 ? Vec3.ZERO : f.normalize().scale(0.9);
        p.setDeltaMovement(f.x, -1.0, f.z);
        p.hurtMarked = true;
        p.fallDistance = 0f;
        DIVING.add(p.getUUID());
        BOUNCED.remove(p.getUUID());
        CastFx.shards(p.serverLevel(), p.position().add(0, 1, 0), 0xFFFFFF, 1.4f, 6);
        CastFx.trail(p.serverLevel(), p, new Vec3(0, 0.9, 0), CastFx.EMBER, 0.16f, 22);
        CastFx.sound(p.serverLevel(), p.position(), FSounds.CAST_WHOOSH.get(), 1f, 0.75f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.8f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        if (DIVING.contains(p.getUUID())) {
            p.fallDistance = 0f;
            if (!p.onGround()) {
                // Struck in the dive: the first creature the body meets is staggered and taken along to the landing.
                if (!Carrying.carrying(p) && !BOUNCED.contains(p.getUUID())) {
                    for (LivingEntity e : near(p, p.position().add(0, p.getBbHeight() / 2, 0), 0.0)) {
                        if (!e.getBoundingBox().intersects(p.getBoundingBox().inflate(0.3)))
                            continue;
                        Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1);
                        away = away.lengthSqr() < 1e-6 ? Vec3.ZERO : away.normalize();
                        int r = Carrying.begin(p, e, away);
                        if (r < 0)
                            BOUNCED.add(p.getUUID());
                        if (r != 0) {
                            CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.1f);
                            break;
                        }
                    }
                }
            }
            if (p.onGround()) {
                DIVING.remove(p.getUUID());
                BOUNCED.remove(p.getUUID());
                float work = (BodyMass.of(p) + BodyMass.armour(p)) * FTuning.GRAVITY_BLOCKS_PER_S2 * 3f;
                for (LivingEntity e : near(p, p.position(), 2.5)) {
                    e.invulnerableTime = 0;
                    e.hurt(p.damageSources().playerAttack(p), 4f);
                    stagger(p, e, new Work(work));
                }
                Carrying.end(p);
                Vec3 land = p.position().add(0, 0.2, 0);
                CastFx.smash(p.serverLevel(), land, 500, false);
                CastFx.sound(p.serverLevel(), land, FSounds.CAST_SLAM.get(), 1f, 1f);
                CastFx.pulse(p.serverLevel(), land, Vec3.ZERO, CastFx.EMBER, 3.2f, 8);
                CastFx.pulse(p.serverLevel(), land, Vec3.ZERO, 0xFFFFFF, 1.8f, 6);
                CastFx.shards(p.serverLevel(), land, CastFx.EMBER, 2.4f, 9);
                CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.9f);
            }
        }
    }
}
