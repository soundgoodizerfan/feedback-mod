// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the blow perks do to a struck creature (one static per perk), and the little state some of them keep, per creature and cleared
 * with it ({@link WielderState}). Every size is in {@link ModularTuning}. Registered as {@link StrikePerk}s by {@link ModularTraits}.
 */
public final class PerkEffects {

    private PerkEffects() {
    }

    /** Until when (game time) a creature is under a perk, and who put it there. */
    public record Mark(long until, UUID by) {
    }

    /** Mobs that forget their placator, flee their dreader, cannot shoot or cast. */
    static final WielderState<Mark> PLACATED = new WielderState<>();
    static final WielderState<Mark> DREAD = new WielderState<>();
    static final WielderState<Mark> STIFLED = new WielderState<>();

    /** A bleed in progress: pulses left and the game time of the next. */
    private static final class Bleed {
        final ServerLevel level;
        final UUID target;
        final UUID by;
        int left;
        long next;

        Bleed(ServerLevel level, UUID target, UUID by, int left, long next) {
            this.level = level;
            this.target = target;
            this.by = by;
            this.left = left;
            this.next = next;
        }
    }

    private static final List<Bleed> BLEEDS = new ArrayList<>();

    /** Whether {@code mark} still holds at {@code now}. */
    static boolean active(Mark mark, long now) {
        return mark != null && now < mark.until();
    }

    private static void mark(WielderState<Mark> state, LivingEntity target, LivingEntity striker, int ticks) {
        state.put(target.getUUID(), new Mark(target.level().getGameTime() + ticks, striker == null ? null : striker.getUUID()));
    }

    // --- the effects ------------------------------------------------------------------------------

    /** The target is soaked: fire out, and a water-shy creature (blaze, enderman, snow golem) is hurt by it. */
    public static void drench(ServerLevel level, LivingEntity target, LivingEntity striker) {
        target.clearFire();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
        if (target.isSensitiveToWater())
            target.hurt(level.damageSources().generic(), ModularTuning.PERK_DRENCH_DAMAGE);
    }

    /** One beneficial effect on the target ends. */
    public static void dispel(ServerLevel level, LivingEntity target, LivingEntity striker) {
        for (MobEffectInstance instance : new ArrayList<>(target.getActiveEffects()))
            if (instance.getEffect().value().isBeneficial()) {
                target.removeEffect(instance.getEffect());
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                return;
            }
    }

    /** A struck mob forgets its attacker for a while: a retarget onto the striker is refused ({@link PerkEvents}). */
    public static void placate(ServerLevel level, LivingEntity target, LivingEntity striker) {
        if (striker == null || !(target instanceof Mob mob))
            return;
        mark(PLACATED, mob, striker, ModularTuning.PERK_PLACATE_TICKS);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    /** A struck mob runs from its attacker: a flee goal, added once, runs while the mark holds. */
    public static void dread(ServerLevel level, LivingEntity target, LivingEntity striker) {
        if (striker == null || !(target instanceof net.minecraft.world.entity.PathfinderMob mob))
            return;
        mark(DREAD, mob, striker, ModularTuning.PERK_DREAD_TICKS);
        mob.setTarget(null);
        // Goals are not saved with a mob, so the goal itself is asked for, never a tag.
        if (mob.goalSelector.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof Flee))
            mob.goalSelector.addGoal(0, new Flee(mob));
    }

    private static final class Flee extends Goal {
        private final net.minecraft.world.entity.PathfinderMob mob;

        Flee(net.minecraft.world.entity.PathfinderMob mob) {
            this.mob = mob;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        private Mark mark() {
            Mark mark = DREAD.get(mob.getUUID());
            return active(mark, mob.level().getGameTime()) ? mark : null;
        }

        @Override
        public boolean canUse() {
            return mark() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mark() != null;
        }

        @Override
        public void tick() {
            Mark mark = mark();
            if (mark == null || mark.by() == null || !(mob.level() instanceof ServerLevel level) || !mob.getNavigation().isDone())
                return;
            net.minecraft.world.entity.Entity from = level.getEntity(mark.by());
            Vec3 away = DefaultRandomPos.getPosAway(mob, 14, 6, from == null ? mob.position() : from.position());
            if (away != null)
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
        }
    }

    /** A bleed: damage in pulses, ignoring the hit's own invulnerable frames; one per target, restarted by each blow. {@link PerkEvents} ticks them. */
    public static void rend(ServerLevel level, LivingEntity target, LivingEntity striker) {
        BLEEDS.removeIf(b -> b.target.equals(target.getUUID()));
        BLEEDS.add(new Bleed(level, target.getUUID(), striker == null ? null : striker.getUUID(),
                ModularTuning.PERK_REND_PULSES, level.getGameTime() + ModularTuning.PERK_REND_GAP_TICKS));
    }

    /** Open bleeds are dropped when the server stops, so a level is never held past it. */
    static void clearBleeds() {
        BLEEDS.clear();
    }

    /** Runs the bleeds due this tick; nothing at all when none are open. */
    static void tickBleeds(long now) {
        if (BLEEDS.isEmpty())
            return;
        BLEEDS.removeIf(b -> {
            if (now < b.next)
                return false;
            net.minecraft.world.entity.Entity e = b.level.getEntity(b.target);
            if (!(e instanceof LivingEntity target) || !target.isAlive())
                return true;
            target.invulnerableTime = 0;
            // Credited to whoever opened the wound (no direct entity, so no perk of theirs fires on a pulse).
            net.minecraft.world.entity.Entity by = b.by == null ? null : b.level.getEntity(b.by);
            target.hurt(by == null ? b.level.damageSources().generic() : b.level.damageSources().indirectMagic(null, by), ModularTuning.PERK_REND_DAMAGE);
            b.level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 3, 0.2, 0.3, 0.2, 0.0);
            b.next = now + ModularTuning.PERK_REND_GAP_TICKS;
            return --b.left <= 0;
        });
    }

    public static void wither(ServerLevel level, LivingEntity target, LivingEntity striker) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, ModularTuning.PERK_WITHER_TICKS));
    }

    public static void hex(ServerLevel level, LivingEntity target, LivingEntity striker) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ModularTuning.PERK_HEX_TICKS));
    }

    public static void dazzle(ServerLevel level, LivingEntity target, LivingEntity striker) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ModularTuning.PERK_DAZZLE_TICKS));
    }

    /** The target cannot loose a projectile for a while ({@link PerkEvents} refuses what it fires). */
    public static void stifle(ServerLevel level, LivingEntity target, LivingEntity striker) {
        mark(STIFLED, target, striker, ModularTuning.PERK_STIFLE_TICKS);
    }

    /** Every harmful effect on the target spreads to the creatures beside it, the striker excepted. */
    public static void contagion(ServerLevel level, LivingEntity target, LivingEntity striker) {
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance instance : target.getActiveEffects())
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                harmful.add(instance);
        if (harmful.isEmpty())
            return;
        AABB box = target.getBoundingBox().inflate(ModularTuning.PERK_CONTAGION_BLOCKS);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != target && e != striker && e.isAlive()))
            for (MobEffectInstance instance : harmful)
                near.addEffect(new MobEffectInstance(instance));
    }

    /** The target is dragged toward the striker, harder the more armour it wears. */
    public static void yank(ServerLevel level, LivingEntity target, LivingEntity striker) {
        if (striker == null)
            return;
        double pull = Math.min(ModularTuning.PERK_YANK_MAX, target.getArmorValue() * ModularTuning.PERK_YANK_PER_ARMOUR);
        if (pull <= 0)
            return;
        Vec3 toward = striker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        target.push(toward.x * pull, 0.1, toward.z * pull);
        target.hurtMarked = true;
    }

    /** The striker reads how hurt and how armoured the target is, in words (numbers cost an instrument). */
    public static void insight(ServerLevel level, LivingEntity target, LivingEntity striker) {
        if (!(striker instanceof ServerPlayer player))
            return;
        float health = target.getHealth() / target.getMaxHealth();
        int armour = target.getArmorValue();
        player.displayClientMessage(Component.translatable("perk.feedback.insight.read", target.getDisplayName(),
                Component.translatable("perk.feedback.insight.health." + (health > ModularTuning.PERK_INSIGHT_SOUND_HEALTH ? "sound"
                        : health > ModularTuning.PERK_INSIGHT_FAILING_HEALTH ? "hurt" : "failing")),
                Component.translatable("perk.feedback.insight.armor." + (armour <= 0 ? "bare" : armour < ModularTuning.PERK_INSIGHT_HEAVY_ARMOR ? "light" : "heavy"))), true);
    }

    /** Standing a villager counts toward the price for a player with rapport in either hand or on the body; 0 for one without. */
    public static int rapport(net.minecraft.world.entity.player.Player player) {
        boolean has = io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.of(player.getMainHandItem()).containsKey(io.github.soundgoodizerfan.feedback.process.Material.Perk.RAPPORT)
                || io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.of(player.getOffhandItem()).containsKey(io.github.soundgoodizerfan.feedback.process.Material.Perk.RAPPORT)
                || ModularTraits.worn(player).stream().anyMatch(w -> w.id().equals("rapport"));
        return has ? ModularTuning.PERK_RAPPORT_STANDING : 0;
    }

    // --- shared helpers ---------------------------------------------------------------------------

    /** Whether {@code state} has run out for {@code entity}, and if it has, starts it again for {@code ticks}. A cooldown that lives on the wielder. */
    static boolean ready(WielderState<Long> state, LivingEntity entity, int ticks) {
        if (cooling(state, entity))
            return false;
        start(state, entity, ticks);
        return true;
    }

    /** Whether {@code entity}'s cooldown in {@code state} is still running. */
    static boolean cooling(WielderState<Long> state, LivingEntity entity) {
        Long until = state.get(entity.getUUID());
        return until != null && entity.level().getGameTime() < until;
    }

    /** Starts {@code entity}'s cooldown in {@code state}, for a perk that spends it only when it did something. */
    static void start(WielderState<Long> state, LivingEntity entity, int ticks) {
        state.put(entity.getUUID(), entity.level().getGameTime() + ticks);
    }

    /** The nearest living things beside {@code centre}, the two given excepted, nearest first. Players, villagers and tamed animals are never among them: a burst or an arc is a weapon's, not a stray. */
    static List<LivingEntity> beside(ServerLevel level, LivingEntity centre, LivingEntity except, double blocks) {
        return level.getEntitiesOfClass(LivingEntity.class, centre.getBoundingBox().inflate(blocks),
                e -> e != centre && e != except && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
                        && !(e instanceof net.minecraft.world.entity.npc.AbstractVillager)
                        && !(e instanceof net.minecraft.world.entity.OwnableEntity owned && owned.getOwnerUUID() != null))
                .stream().sorted(Comparator.comparingDouble(centre::distanceToSqr)).toList();
    }

    /** Drops the marks that have run out; called now and then, since a mob that left the world never clears its own. */
    static void prune(long now) {
        PLACATED.removeIf(m -> !active(m, now));
        DREAD.removeIf(m -> !active(m, now));
        STIFLED.removeIf(m -> !active(m, now));
    }

    static void sound(ServerLevel level, LivingEntity at, net.minecraft.sounds.SoundEvent event, float pitch) {
        level.playSound(null, at.blockPosition(), event, SoundSource.PLAYERS, 0.6f, pitch);
    }
}
