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
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.ModularType;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The three unlocks of the melee-and-tether tree, each checked through {@link Skills#unlocked} on the
 * server and each keyed on the main-hand weapon's baked family (never an item id), fired by a blow that
 * landed ({@code LivingDamageEvent.Post}, so a parried or dodged blow earns nothing):
 * <ul>
 * <li><b>blade_flourish</b>: a blade hit takes {@link MeleeTuning#FLOURISH_SHAVE} ticks off the cooldown
 * running on the fighter, so a strike flows into the next roll, parry or kick.</li>
 * <li><b>mace_quake</b>: a full-weight mace blow shakes the ground; every other body within
 * {@link MeleeTuning#QUAKE_RADIUS} is shoved and staggered by the blow's impact over its own stand-in mass,
 * the parry's formula, so a golem shrugs it off.</li>
 * <li><b>spear_vault</b>: a sprinting wielder whose thrust lands is carried up and over the target.</li>
 * </ul>
 * Off-hand strikes do not trigger these (they pay no melee points either).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MeleeMoves {

    /** Earliest game tick each player may vault again. */
    private static final Map<UUID, Long> NEXT_VAULT = new HashMap<>();

    private MeleeMoves() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || event.getNewDamage() <= 0f)
            return;
        ModularType.Family family = SkillEarning.family(player.getMainHandItem());
        LivingEntity target = event.getEntity();
        if (family == ModularType.Family.BLADE && Skills.unlocked(player, "blade_flourish")) {
            CombatState s = CombatState.of(player);
            if (s.phase() == CombatPhase.IDLE && s.ticksLeft() > 0)
                CombatState.set(player, new CombatState(CombatPhase.IDLE, Math.max(0, s.ticksLeft() - MeleeTuning.FLOURISH_SHAVE), 0, s.iFrames()));
        } else if (family == ModularType.Family.MACE && event.getNewDamage() >= MeleeTuning.QUAKE_MIN_DAMAGE
                && Skills.unlocked(player, "mace_quake")) {
            float impact = event.getNewDamage() * ParryTuning.JOULES_PER_DAMAGE;
            for (LivingEntity near : target.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(MeleeTuning.QUAKE_RADIUS),
                    e -> e != player && e != target && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e))) {
                near.knockback(MeleeTuning.QUAKE_KNOCK, target.getX() - near.getX(), target.getZ() - near.getZ());
                CombatTicker.stagger(near, ParryTuning.staggerTicks(impact, ParryTuning.standInMass(near)), ParryTuning.STAGGER_SLOW);
                io.github.soundgoodizerfan.feedback.combat.skill.Rearms.on(player, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Rearm.STAGGER);
            }
        } else if (family == ModularType.Family.SPEAR && player.isSprinting() && player.onGround()
                && player.level().getGameTime() >= NEXT_VAULT.getOrDefault(player.getUUID(), 0L)
                && Skills.unlocked(player, "spear_vault")) {
            NEXT_VAULT.put(player.getUUID(), player.level().getGameTime() + MeleeTuning.VAULT_COOLDOWN);
            Vec3 fwd = player.getLookAngle().multiply(1, 0, 1).normalize();
            player.setDeltaMovement(fwd.x * MeleeTuning.VAULT_FORWARD, MeleeTuning.VAULT_UP, fwd.z * MeleeTuning.VAULT_FORWARD);
            player.hurtMarked = true;
        }
    }
}
