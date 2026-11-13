// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.registry.FEntities;

/**
 * The steam a pulse gun's forced vent lets go: a cloud in front of the muzzle that lives a few seconds and scalds any living body that
 * stays in it for about a second, the wielder and allies included. The server does the scalding; both sides draw the cloud as particles.
 */
public class SteamCloud extends Entity {

    private int age;
    /** Ticks each body has been in the cloud. */
    private final Map<Integer, Integer> inside = new HashMap<>();

    public SteamCloud(EntityType<? extends SteamCloud> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A vent in front of {@code shooter}, who aimed along {@code yaw}/{@code pitch}. */
    public static void vent(ServerLevel level, LivingEntity shooter, float pitch, float yaw) {
        SteamCloud cloud = new SteamCloud(FEntities.STEAM_CLOUD.get(), level);
        Vec3 at = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(1.6)).add(0, -0.4, 0);
        cloud.setPos(at.x, at.y, at.z);
        level.addFreshEntity(cloud);
        level.playSound(null, at.x, at.y, at.z, io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_VENT.get(), SoundSource.PLAYERS, 0.9f, 0.9f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (++age > ModularTuning.EGUN_STEAM_TICKS) {
            discard();
            return;
        }
        double r = ModularTuning.EGUN_STEAM_RADIUS;
        if (level() instanceof ServerLevel server) {
            var box = getBoundingBox().inflate(r);
            Map<Integer, Integer> now = new HashMap<>();
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                int t = inside.getOrDefault(e.getId(), 0) + 1;
                if (t >= ModularTuning.EGUN_STEAM_SCALD_TICKS) {
                    e.invulnerableTime = 0;
                    e.hurt(Hazards.source(server, Hazards.SCALD), ModularTuning.EGUN_STEAM_DAMAGE);
                    t = 0;
                }
                now.put(e.getId(), t);
            }
            inside.clear();
            inside.putAll(now);
        } else {
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 2 * r, getY() + random.nextDouble() * r,
                        getZ() + (random.nextDouble() - 0.5) * 2 * r, 0, 0.03, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
    }
}
