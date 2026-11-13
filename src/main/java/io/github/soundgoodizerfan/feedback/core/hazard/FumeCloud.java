// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A volatile toxic gas loose in the room (osmium tetroxide): a cloud that lingers where it was let go, whose strength is the
 * material's {@code toxic} perk. Unlike {@link Fumes}, which poisons at once, a cloud gives each living thing inside it an
 * <em>exposure</em> that builds a step a second and drains twice as fast outside: past the first threshold the eyes sting and
 * the head swims (nausea), past the second the sight goes (blindness), past the third the lungs burn (damage a second).
 * The damage stops with one heart left, so a cloud is slow, readable and walked out of, never an instant death. Exposure rides
 * the entity's persistent data with the game time it was last updated, so it drains without anything ticking the entity.
 * <p>
 * Released by a hot toxic material heated without its protective gas ({@code ThermalVesselCore}) and by a toxic gas a reactor
 * has nowhere to put ({@link #vent}). A sealed vessel with room in its tank releases nothing.
 * <p>
 * shortcut: held in memory, not saved (a reload clears clouds); the cloud does not drift or spread, it is a ball around a point;
 * upgrade if clouds must survive unloads or follow draughts.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FumeCloud {

    private static final class Cloud {
        final Vec3 at;
        int level;
        long until;

        Cloud(Vec3 at, int level, long until) {
            this.at = at;
            this.level = level;
            this.until = until;
        }
    }

    private static final Map<ServerLevel, List<Cloud>> CLOUDS = new WeakHashMap<>();
    private static final String EXPOSURE = "FeedbackFume", AT = "FeedbackFumeAt";

    private FumeCloud() {
    }

    /** Lets a cloud of strength {@code toxic} go at {@code at}; one already within a block grows to the stronger and lasts the longer. Server side. */
    public static void release(Level level, Vec3 at, int toxic) {
        if (toxic <= 0 || !(level instanceof ServerLevel server))
            return;
        long until = server.getGameTime() + (long) HazardTuning.FUME_CLOUD_TICKS_PER_LEVEL * toxic;
        List<Cloud> clouds = CLOUDS.computeIfAbsent(server, l -> new ArrayList<>());
        for (Cloud c : clouds)
            if (c.at.distanceToSqr(at) <= 1.0) {
                c.level = Math.max(c.level, toxic);
                c.until = Math.max(c.until, until);
                return;
            }
        clouds.add(new Cloud(at, toxic, until));
    }

    /** A toxic fluid with nowhere to go lets its cloud go: the strength is its material's {@code toxic} perk, 0 for a harmless one. */
    public static void vent(Level level, Vec3 at, FluidStack lost) {
        if (lost.isEmpty())
            return;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(lost.getFluid()));
        if (m != null)
            release(level, at, Fumes.level(m));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % HazardTuning.FUME_CLOUD_INTERVAL_TICKS != 0)
            return;
        List<Cloud> clouds = CLOUDS.get(level);
        if (clouds == null)
            return;
        long now = level.getGameTime();
        clouds.removeIf(c -> c.until <= now);
        float r = HazardTuning.FUME_RADIUS_BLOCKS;
        for (Cloud c : clouds) {
            level.sendParticles(ParticleTypes.SMOKE, c.at.x, c.at.y, c.at.z, 6, r * 0.4, r * 0.3, r * 0.4, 0.005);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(c.at, c.at).inflate(r)))
                if (victim.position().distanceTo(c.at) <= r)
                    dose(level, victim, c.level, now);
        }
    }

    private static void dose(ServerLevel level, LivingEntity victim, int cloudLevel, long now) {
        CompoundTag data = victim.getPersistentData();
        float seconds = (now - data.getLong(AT)) / 20f;
        float before = Math.max(0f, data.getFloat(EXPOSURE) - seconds * HazardTuning.FUME_CLOUD_DECAY_PER_SECOND);
        float after = before + cloudLevel * HazardTuning.FUME_CLOUD_DOSE_PER_LEVEL * Skills.fumeShare(victim);
        data.putFloat(EXPOSURE, after);
        data.putLong(AT, now);
        if (after >= HazardTuning.FUME_CLOUD_NAUSEA)
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160));
        if (after >= HazardTuning.FUME_CLOUD_BLIND)
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
        if (after >= HazardTuning.FUME_CLOUD_HURT && victim.getHealth() > HazardTuning.FUME_CLOUD_DAMAGE_FLOOR_HEALTH)
            victim.hurt(level.damageSources().magic(), HazardTuning.FUME_CLOUD_DAMAGE);
        if (victim instanceof ServerPlayer p) {
            int step = after >= HazardTuning.FUME_CLOUD_HURT ? 3 : after >= HazardTuning.FUME_CLOUD_BLIND ? 2 : after >= HazardTuning.FUME_CLOUD_NAUSEA ? 1 : 0;
            int was = before >= HazardTuning.FUME_CLOUD_HURT ? 3 : before >= HazardTuning.FUME_CLOUD_BLIND ? 2 : before >= HazardTuning.FUME_CLOUD_NAUSEA ? 1 : 0;
            if (step > was)
                p.displayClientMessage(Component.translatable("feedback.fume_cloud." + step), true);
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level)
            CLOUDS.remove(level);
    }
}
