// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.combat.cast.Disorder;
import io.github.soundgoodizerfan.feedback.combat.skill.Weakness;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.ModularGlove;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.modular.trait.Billhook;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The unarmed stance chain (skill spec 4.8, tree {@code fist}): each move is keyed on the movement state the
 * fighter is in when an empty-handed blow lands, so the decision is which state to be in next.
 * <ul>
 * <li><b>Jab</b>: standing on the ground, the reach is a little longer.</li>
 * <li><b>Lunge</b>: sprinting, the blow staggers harder and the lunge guards the fighter for a few ticks.</li>
 * <li><b>Sweep</b>: standing, the blow is also dealt across a wide arc at a share of its damage.</li>
 * <li><b>Chop</b>: falling, the blow drives the target down.</li>
 * </ul>
 * (Uppercut is a cast now, {@code combat/cast/Uppercut}, not a stance.)
 * The chain (Jab, then Lunge or Sweep, then Chop) is a combo bonus, not a gate: a move landing
 * soon after the one before it hits harder. Every move is learned separately (spec 4.8's default, still [OPEN]).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FistStances {

    private static final ResourceLocation JAB_REACH = Feedback.id("jab_reach");

    private record Last(String move, long tick) {
    }

    private static final Map<UUID, Last> LAST = new HashMap<>();
    private static boolean inSweep;

    private FistStances() {
    }

    private static boolean unarmed(ServerPlayer p) {
        return BareMode.mainHand(p).isEmpty();
    }

    /** Standing: on the ground and not sprinting. */
    private static boolean standing(ServerPlayer p) {
        return p.onGround() && !p.isSprinting();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        var reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        boolean on = unarmed(p) && standing(p) && Skills.unlocked(p, "fist_jab");
        if (on && !reach.hasModifier(JAB_REACH))
            reach.addTransientModifier(new AttributeModifier(JAB_REACH, FistTuning.JAB_REACH, AttributeModifier.Operation.ADD_VALUE));
        else if (!on && reach.hasModifier(JAB_REACH))
            reach.removeModifier(JAB_REACH);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (inSweep || !(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet() || !unarmed(p)
                || StrikeSource.hand(source) != InteractionHand.MAIN_HAND && !BareMode.on(p))
            return;
        LivingEntity target = event.getEntity();
        // Worn gauntlets add to every barehanded swing, move or not, at the swing's charge like vanilla's own scaling;
        // the moves below read the raised amount, so they scale with it. Weapon swings returned above.
        float fist = CombatGear.of(p).fist();
        if (fist > 0) {
            float charge = p.getAttackStrengthScale(0.5f);
            event.setAmount(event.getAmount() + fist * (0.2f + charge * charge * 0.8f));
        }
        var glove = CombatGear.platedGlove(p);
        var plate = ModularGlove.plateParams(glove);
        if (plate.containsKey("bleed_damage")) {
            float seconds = plate.getOrDefault("bleed_seconds", 5f), bleed = plate.get("bleed_damage");
            // Opening cut: claws on an unaware or back-turned creature open a bleed twice as deep.
            if (Skills.unlocked(p, "opening_cut") && (Weakness.unaware(target) || Weakness.backTurned(p, target)))
                bleed *= FistTuning.OPENING_CUT;
            // Gouge: a claw hit on a creature already bleeding stacks the bleed, and bleeding is a kind of disorder.
            if (Skills.unlocked(p, "gouge") && Billhook.stack(target, FistTuning.GOUGE_STACKS, seconds))
                Disorder.note(p, target, Disorder.Kind.BLEEDING);
            else
                Billhook.open(target, seconds, bleed);
            // Tear straps: the claws rake the target's armour, and wear on their own points.
            if (Skills.unlocked(p, "tear_straps")) {
                Disorder.wearArmour(target, ModularTuning.IRON_CLASS_HARDNESS * FistTuning.TEAR_HARDNESS_SHARE);
                if (glove.isDamageableItem() && glove.getDamageValue() < glove.getMaxDamage() - 1)
                    glove.setDamageValue(glove.getDamageValue() + 1);
            }
        }
        long now = p.level().getGameTime();
        String move = null;
        Vec3 v = p.getDeltaMovement();
        if (p.isSprinting() && p.onGround() && Skills.unlocked(p, "fist_lunge"))
            move = "lunge";
        else if (!p.onGround() && v.y < -FistTuning.AIR_SPEED && Skills.unlocked(p, "fist_chop"))
            move = "chop";
        else if (standing(p) && Skills.unlocked(p, "fist_sweep"))
            move = "sweep";
        else if (standing(p) && Skills.unlocked(p, "fist_jab"))
            move = "jab";
        if (move == null)
            return;
        Last last = LAST.get(p.getUUID());
        boolean combo = last != null && now - last.tick() <= FistTuning.COMBO_TICKS && follows(last.move(), move);
        LAST.put(p.getUUID(), new Last(move, now));
        float amount = event.getAmount() * (combo ? 1 + FistTuning.COMBO_BONUS : 1f);
        event.setAmount(amount);
        float impact = amount * ParryTuning.JOULES_PER_DAMAGE;
        switch (move) {
            case "lunge" -> {
                CombatTicker.stagger(target, Math.round(ParryTuning.staggerTicks(impact, ParryTuning.standInMass(target)) * FistTuning.LUNGE_STAGGER),
                        ParryTuning.STAGGER_SLOW);
                CombatState s = CombatState.of(p);
                CombatState.set(p, new CombatState(s.phase(), s.ticksLeft(), s.then(), Math.max(s.iFrames(), FistTuning.LUNGE_GUARD)));
            }
            case "sweep" -> {
                Vec3 look = p.getLookAngle().multiply(1, 0, 1);
                if (look.lengthSqr() < 1e-6)
                    break;
                Vec3 dir = look.normalize();
                inSweep = true;
                try {
                    for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(FistTuning.SWEEP_RADIUS),
                            e -> e != p && e != target && e.isAlive() && !e.isSpectator() && !p.isAlliedTo(e))) {
                        Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 1e-6 && to.normalize().dot(dir) >= Math.cos(Math.toRadians(FistTuning.SWEEP_HALF_DEG)))
                            e.hurt(p.damageSources().playerAttack(p), amount * FistTuning.SWEEP_SHARE);
                    }
                } finally {
                    inSweep = false;
                }
            }
            case "chop" -> {
                target.setDeltaMovement(target.getDeltaMovement().x, -FistTuning.DRIVE, target.getDeltaMovement().z);
                target.hurtMarked = true;
                CombatTicker.stagger(target, ParryTuning.staggerTicks(impact, ParryTuning.standInMass(target)), ParryTuning.STAGGER_SLOW);
            }
            default -> {
            }
        }
    }

    /** Whether {@code move} is the next link after {@code previous} in the chain. */
    private static boolean follows(String previous, String move) {
        return switch (move) {
            case "lunge", "sweep" -> previous.equals("jab");
            case "chop" -> previous.equals("lunge");
            default -> false;
        };
    }
}
