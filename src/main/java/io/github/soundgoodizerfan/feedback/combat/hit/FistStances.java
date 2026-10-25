/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.hit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
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
 * <li><b>Uppercut</b>: rising, the blow launches the target.</li>
 * </ul>
 * The chain (Jab, then Lunge or Sweep, then Chop, then Uppercut) is a combo bonus, not a gate: a move landing
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
        return p.getMainHandItem().isEmpty();
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
                || StrikeSource.hand(source) != InteractionHand.MAIN_HAND)
            return;
        LivingEntity target = event.getEntity();
        // Worn gauntlets add to every barehanded swing, move or not, at the swing's charge like vanilla's own scaling;
        // the moves below read the raised amount, so they scale with it. Weapon swings returned above.
        float fist = CombatGear.of(p).fist();
        if (fist > 0) {
            float charge = p.getAttackStrengthScale(0.5f);
            event.setAmount(event.getAmount() + fist * (0.2f + charge * charge * 0.8f));
        }
        long now = p.level().getGameTime();
        String move = null;
        Vec3 v = p.getDeltaMovement();
        if (p.isSprinting() && p.onGround() && Skills.unlocked(p, "fist_lunge"))
            move = "lunge";
        else if (!p.onGround() && v.y < -FistTuning.AIR_SPEED && Skills.unlocked(p, "fist_chop"))
            move = "chop";
        else if (!p.onGround() && v.y > FistTuning.AIR_SPEED && Skills.unlocked(p, "fist_uppercut"))
            move = "uppercut";
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
            case "uppercut" -> {
                target.setDeltaMovement(target.getDeltaMovement().x, FistTuning.LAUNCH, target.getDeltaMovement().z);
                target.hurtMarked = true;
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
            case "uppercut" -> previous.equals("chop");
            default -> false;
        };
    }
}
