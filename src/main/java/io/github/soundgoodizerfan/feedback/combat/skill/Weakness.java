// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observation's weakness skills (weapon-independent: they read the target, never the weapon). {@code size_up} reads the
 * creature under the crosshair for {@link #READ_TICKS}; while it is read, a blow from this player by any weapon
 * (melee, bullet, arrow) is scaled by what the target gives away: {@code seams} (the armour and toughness it wears),
 * {@code opening} (facing away, unaware, burning, wet, staggered) and {@code exploit} (an armed cast that spends the read
 * for a large blow). {@code case_file} is standing knowledge of a creature type from kills, read or not.
 * STAND-IN figures, to tune in play.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Weakness {

    /** How long a read lasts; how long an exploit stays armed; reach of the read. */
    static final int READ_TICKS = 300, ARMED_TICKS = 60;
    static final double REACH = 24;
    /** Seams: share per point of armour or toughness, capped. Opening: cap on the sum. Exploit: the blow. Case file: per kill, capped. */
    static final float SEAM_PER_POINT = 0.02f, SEAM_CAP = 0.4f, OPENING_CAP = 0.8f, EXPLOIT = 0.5f, FILE_PER_KILL = 0.01f, FILE_CAP = 0.2f;

    private static final Map<UUID, Map<UUID, Long>> READ = new HashMap<>();
    private static final Map<UUID, Long> ARMED = new HashMap<>();

    private Weakness() {
    }

    private static boolean reading(ServerPlayer p, LivingEntity t) {
        Map<UUID, Long> mine = READ.get(p.getUUID());
        Long until = mine == null ? null : mine.get(t.getUUID());
        return until != null && until > p.level().getGameTime();
    }

    private static LivingEntity look(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestD = REACH * REACH;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(REACH), e -> e != p && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.lengthSqr();
            if (d < bestD && to.normalize().dot(look) > 0.97 && p.hasLineOfSight(e)) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** What the target gives away right now: each is a share added to a blow, capped together. */
    static List<String> openings(ServerPlayer p, LivingEntity t, float[] share) {
        List<String> words = new ArrayList<>();
        float sum = 0;
        Vec3 toP = p.position().subtract(t.position());
        if (t.getLookAngle().dot(toP) < 0) {
            words.add("facing away");
            sum += 0.25f;
        }
        if (t.isSleeping() || t instanceof Mob m && m.getTarget() == null) {
            words.add("unaware");
            sum += 0.25f;
        }
        if (t.isOnFire()) {
            words.add("burning");
            sum += 0.2f;
        }
        if (t.isInWaterOrRain()) {
            words.add("wet");
            sum += 0.1f;
        }
        if (CombatState.of(t).phase() == CombatPhase.STAGGER) {
            words.add("staggered");
            sum += 0.3f;
        }
        share[0] = Math.min(OPENING_CAP, sum);
        return words;
    }

    /** Size up: read the creature under the crosshair (twenty-four blocks, in sight) and say what it gives away. */
    public static boolean sizeUp(ServerPlayer p) {
        LivingEntity t = look(p);
        if (t == null)
            return false;
        READ.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(t.getUUID(), p.level().getGameTime() + READ_TICKS);
        float[] share = new float[1];
        List<String> words = openings(p, t, share);
        float armour = (float) t.getArmorValue(), tough = (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        p.displayClientMessage(Component.translatable("message.feedback.size_up", t.getDisplayName(), Math.round(armour), Math.round(tough),
                Math.round(t.getHealth()), Math.round(t.getMaxHealth()),
                words.isEmpty() ? Component.translatable("message.feedback.size_up.closed") : Component.literal(String.join(", ", words))), true);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
        level.sendParticles(ParticleTypes.ENCHANT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 14, 0.3, 0.4, 0.3, 0.6);
        return true;
    }

    /** Exploit: spend the read on one large blow; the next hit within three seconds on a read creature lands it. */
    public static boolean exploit(ServerPlayer p) {
        LivingEntity t = look(p);
        if (t == null || !reading(p, t))
            return false;
        ARMED.put(p.getUUID(), p.level().getGameTime() + ARMED_TICKS);
        p.serverLevel().playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7f, 1.6f);
        return true;
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p))
            return;
        LivingEntity t = event.getEntity();
        float mult = 1f;
        boolean read = reading(p, t);
        if (read) {
            if (Skills.unlocked(p, "seams"))
                mult += Math.min(SEAM_CAP, (t.getArmorValue() + (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) * SEAM_PER_POINT);
            float[] share = new float[1];
            openings(p, t, share);
            if (Skills.unlocked(p, "opening"))
                mult += share[0];
            Long armed = ARMED.get(p.getUUID());
            if (armed != null && armed > p.level().getGameTime() && Skills.unlocked(p, "exploit")) {
                mult += EXPLOIT + share[0];
                ARMED.remove(p.getUUID());
                READ.getOrDefault(p.getUUID(), Map.of()).remove(t.getUUID());
                p.serverLevel().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 18, 0.3, 0.4, 0.3, 0.5);
            }
        }
        if (Skills.unlocked(p, "case_file"))
            mult += Math.min(FILE_CAP, known(p, t) * FILE_PER_KILL);
        if (mult > 1f) {
            event.setAmount(event.getAmount() * mult);
            if (read)
                SkillEarning.award(p, SkillEarning.OBSERVATION, t.getStringUUID(), SkillTuning.GUN_HIT);
        }
    }

    private static int known(ServerPlayer p, LivingEntity t) {
        return p.getPersistentData().getCompound("feedback_case_file").getInt(t.getType().builtInRegistryHolder().key().location().toString());
    }

    @SubscribeEvent
    static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "case_file"))
            return;
        CompoundTag file = p.getPersistentData().getCompound("feedback_case_file");
        String type = event.getEntity().getType().builtInRegistryHolder().key().location().toString();
        file.putInt(type, file.getInt(type) + 1);
        p.getPersistentData().put("feedback_case_file", file);
    }
}
