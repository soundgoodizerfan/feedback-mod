// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;

import io.github.soundgoodizerfan.feedback.combat.cast.Stamina;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * What an effect node's {@code effect} id does to a body. A registered {@code MobEffect} is applied as one; an id no registry holds is a
 * special the potion map can name without a new class: {@link #IGNITE} sets the body alight (thrown, a fast heat release: see
 * {@link #burst}, {@link ThrownPotionBottle}), {@link #EXPLOSION} detonates (see {@link #explode}); {@link #apply} leaves a body untouched by it, {@link #consume} is the drinker's share.
 */
public final class PotionEffects {

    public static final ResourceLocation IGNITE = Feedback.id("ignite"), EXPLOSION = Feedback.id("explosion");

    private PotionEffects() {}

    /** Applies the effect to {@code target} for {@code ticks} at level {@code amplifier} (0 is I). */
    public static void apply(ResourceLocation id, LivingEntity target, int amplifier, int ticks) {
        if (IGNITE.equals(id))
            target.igniteForSeconds(ticks / 20f);
        else
            BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(e -> target.addEffect(new MobEffectInstance(e, ticks, amplifier)));
    }

    /**
     * The thrown share of an effect, from a flask that landed at {@code at}: the place-and-strength form of the drunk one. Most effects
     * apply to every body within {@code reach}, nearer = longer, the thrower included; the rest act on the place (see each branch).
     * {@code duration} is the bottle's own length. Mobs have no stamina, no gun and no palate, so effects that only a player can use skip them.
     */
    public static void splash(ServerLevel level, net.minecraft.world.entity.Entity owner, net.minecraft.world.phys.Vec3 at, float reach,
            ResourceLocation id, int amplifier, int duration) {
        int base = Math.max(20, Math.round(duration * PotionTuning.THROW_DURATION));
        if (is(id, FEffects.GREEN_THUMB))
            GreenThumb.pour(level, BlockPos.containing(at), amplifier);
        else if (is(id, FEffects.STATIC_GROUND))
            StaticGround.ground(level, at, reach, base);
        else if (is(id, FEffects.QUENCH))
            Quench.douse(level, at, amplifier);
        else if (is(id, FEffects.FINE_PALATE) || is(id, FEffects.ASSAY))
            reveal(level, owner, at, reach, is(id, FEffects.ASSAY));
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(reach))) {
            float near = 1f - (float) Math.sqrt(body.distanceToSqr(at)) / reach;
            if (near <= 0f)
                continue;
            int ticks = Math.max(20, Math.round(base * near));
            boolean player = body instanceof net.minecraft.world.entity.player.Player;
            if (is(id, FEffects.GREEN_THUMB) || is(id, FEffects.STATIC_GROUND))
                continue; // place effects, no status on the body
            else if (is(id, FEffects.QUENCH))
                body.clearFire();
            else if (is(id, FEffects.ANTIVENOM)) {
                body.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
                apply(id, body, amplifier, ticks);
            } else if (is(id, FEffects.SECOND_WIND)) {
                // Only a player has stamina: an instant refill, no wind to repay.
                if (body instanceof net.minecraft.server.level.ServerPlayer sp)
                    Stamina.gain(sp, Stamina.MAX * PotionTuning.SECOND_WIND_REFILL * (amplifier + 1) * near);
            } else if (is(id, FEffects.SLIPSTREAM) && !player)
                body.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        PotionTuning.SLIPSTREAM_STUMBLE * (amplifier + 1), 0));
            else if (is(id, FEffects.SPRINGHEEL))
                Springheel.launch(body, amplifier);
            else if (player || !(is(id, FEffects.STEADY_HAND) || is(id, FEffects.FINE_PALATE) || is(id, FEffects.ASSAY)))
                apply(id, body, amplifier, ticks);
        }
    }

    private static boolean is(ResourceLocation id, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> e) {
        return e.getId().equals(id);
    }

    /** Dropped laced food within reach shows itself to the thrower: a palate eases the taste thresholds, an assay sharpens them further and names the potion. */
    private static void reveal(ServerLevel level, net.minecraft.world.entity.Entity owner, net.minecraft.world.phys.Vec3 at, float reach, boolean assay) {
        if (!(owner instanceof net.minecraft.world.entity.player.Player thrower))
            return;
        float keen = assay ? PotionTuning.ASSAY_KEEN : 1f - PotionTuning.PALATE_EASE;
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(reach))) {
            var stack = item.getItem();
            var lacing = stack.get(FDataComponents.LACING.get());
            var meal = stack.get(FDataComponents.MEAL.get());
            if (lacing != null && meal != null && lacing.detectable(meal, keen))
                thrower.displayClientMessage(assay ? Component.translatable("message.feedback.assay_laced", stack.getHoverName(), name(lacing.effect()))
                        : Component.translatable("message.feedback.palate_laced", stack.getHoverName()), true);
        }
    }

    /**
     * Drinking or eating the potion: the same effect with the body at the centre. {@link #IGNITE} burns for the length times one plus the
     * level; {@link #EXPLOSION} detonates on the drinker.
     */
    public static void consume(ResourceLocation id, LivingEntity target, int amplifier, int ticks) {
        if (EXPLOSION.equals(id)) {
            if (target.level() instanceof ServerLevel server)
                // No source entity: a blast leaves its own source unhurt, and the drinker is meant to take it.
                explode(server, null, target.position(), amplifier);
        } else if (IGNITE.equals(id))
            target.igniteForSeconds(ticks / 20f * (1 + amplifier));
        else
            apply(id, target, amplifier, ticks);
    }

    /** A real blast of {@link PotionTuning#EXPLOSION_POWER} plus a step per level, breaking blocks as TNT does. */
    public static void explode(ServerLevel level, net.minecraft.world.entity.Entity source, net.minecraft.world.phys.Vec3 at, int amplifier) {
        level.explode(source, at.x, at.y, at.z, PotionTuning.EXPLOSION_POWER + PotionTuning.EXPLOSION_POWER_PER_LEVEL * amplifier,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT);
    }

    public static Component name(ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> h.value().getDisplayName()).orElseGet(() -> Component.translatable(id.toLanguageKey("effect")));
    }

    /**
     * A potion {@code state} let go at {@code at}, from a flask or a round: its effect to every body within {@code reach} (the fire node
     * burns and bursts, the explosion node explodes), the same place-and-strength act whoever let it go. A dud or a faded potion does nothing.
     *
     * @param owner the thrower or shooter, or null.
     * @param cause what the explosion is blamed on when there is no owner (the flask itself); never null.
     */
    public static void impact(ServerLevel server, net.minecraft.world.entity.Entity owner, net.minecraft.world.entity.Entity cause,
            net.minecraft.world.phys.Vec3 at, PotionState state, float reach) {
        int amplifier = state == null || state.dud() ? -1 : state.amplifierNow(server);
        if (amplifier < 0)
            return;
        var id = state.effect().get();
        boolean blast = IGNITE.equals(id);
        if (EXPLOSION.equals(id))
            explode(server, owner != null ? owner : cause, at, amplifier);
        else if (blast) for (LivingEntity body : server.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(reach))) {
            float near = 1f - (float) Math.sqrt(body.distanceToSqr(at)) / reach;
            if (near <= 0f)
                continue;
            body.hurt(io.github.soundgoodizerfan.feedback.core.hazard.Hazards.source(server, io.github.soundgoodizerfan.feedback.core.hazard.Hazards.HOT_CONTACT),
                    PotionTuning.BLAST_DAMAGE * (1 + amplifier) * near);
            body.igniteForSeconds(PotionTuning.BLAST_BURN_SECONDS * near);
        }
        if (blast)
            burst(server, BlockPos.containing(at));
        else if (!EXPLOSION.equals(id))
            splash(server, owner, at, reach, id, amplifier, state.duration());
    }

    public static void burst(ServerLevel level, BlockPos at) {
        PressureBurst.hiss(level, at);
        level.sendParticles(ParticleTypes.EXPLOSION, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.8f, 1.4f);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1)))
            if (level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, Direction.UP))
                for (Direction d : Direction.values())
                    if (level.getBlockState(p.relative(d)).isFlammable(level, p.relative(d), d.getOpposite())) {
                        level.setBlockAndUpdate(p.immutable(), BaseFireBlock.getState(level, p));
                        break;
                    }
    }}
