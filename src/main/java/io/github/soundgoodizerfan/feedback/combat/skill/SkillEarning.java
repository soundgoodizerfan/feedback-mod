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
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Where skill points are earned (plan 2.7): each tree's pool by doing its own physical act, every award
 * diminished by how often that same target has paid lately ({@link SkillPools#awarded}). One pool per tree
 * (see {@code docs/feedback_skill_trees.md}).
 * <ul>
 * <li>Anchor: a parry that lands ({@link #parried}, called from the parry resolver), per attacker.</li>
 * <li>Veil: a roll that cancelled a blow struck by something ({@link #dodged}, called from the roll's i-frame
 * cancel), per attacker. Fire ticks and other sourceless damage do not pay.</li>
 * <li>Brute: a hit with a blade, mace or spear, by baked family, never an item id. Main hand only: an
 * off-hand strike carries its hand on the damage source (plan 5.8) and does not pay yet.</li>
 * <li>Onslaught: any blow with an empty main hand (a punch, a kick).</li>
 * <li>Observation: a gun hit at or past {@link SkillTuning#OBSERVE_RANGE} blocks.</li>
 * <li>Proficiency: surviving a blow struck by something, per attacker. Fire ticks, falls and other sourceless damage
 * do not pay, and a blow that kills pays nothing.</li>
 * <li>Chaos: a kill by the player, by any means, as a bonus on top of that hit.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SkillEarning {

    public static final String ANCHOR = "anchor", VEIL = "veil", BRUTE = "brute", ONSLAUGHT = "onslaught",
            OBSERVATION = "observation", PROFICIENCY = "proficiency", CHAOS = "chaos";

    private SkillEarning() {
    }

    /** A parry landed against {@code source}. */
    public static void parried(LivingEntity defender, DamageSource source) {
        award(defender, ANCHOR, keyOf(source), SkillTuning.PARRY);
    }

    /** A roll's i-frames swallowed {@code source}. */
    public static void dodged(LivingEntity who, DamageSource source) {
        if (source.getEntity() != null)
            award(who, VEIL, keyOf(source), SkillTuning.EVASION);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (event.getNewDamage() > 0f) {
            if (source.getEntity() instanceof ServerPlayer attacker)
                Rearms.fight(attacker);
            if (event.getEntity() instanceof ServerPlayer victim) {
                Rearms.fight(victim);
                if (source.getEntity() != null && source.getEntity() != victim && !victim.isDeadOrDying())
                    award(victim, PROFICIENCY, keyOf(source), SkillTuning.SURVIVED);
            }
        }
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || event.getNewDamage() <= 0f)
            return;
        Rearms.on(player, SkillTree.Rearm.STRIKE);
        String target = event.getEntity().getStringUUID();
        boolean dead = event.getEntity().isDeadOrDying();
        if (dead)
            award(player, CHAOS, target, SkillTuning.KILL_BONUS);
        // Hands and feet: any blow with an empty main hand (a punch, the Feedbacker, a kick).
        if (player.getMainHandItem().isEmpty()) {
            award(player, ONSLAUGHT, target, SkillTuning.FIST_HIT);
            return;
        }
        ModularType.Family family = family(player.getMainHandItem());
        if (family == ModularType.Family.GUN) {
            if (player.distanceTo(event.getEntity()) >= SkillTuning.OBSERVE_RANGE)
                award(player, OBSERVATION, target, SkillTuning.GUN_HIT);
        } else if (family == ModularType.Family.BLADE || family == ModularType.Family.MACE
                || family == ModularType.Family.SPEAR)
            award(player, BRUTE, target, SkillTuning.MELEE_HIT);
    }

    /** The baked family of a modular stack, or null for anything else. */
    public static ModularType.Family family(ItemStack stack) {
        var design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        return type == null ? null : type.family();
    }

    /** The attacker's uuid, or the damage type's name when nothing struck. */
    private static String keyOf(DamageSource source) {
        return source.getEntity() != null ? source.getEntity().getStringUUID() : source.getMsgId();
    }

    private static void award(LivingEntity who, String tree, String key, int base) {
        if (!(who instanceof ServerPlayer player) || player.isSpectator())
            return;
        SkillPools.set(player, SkillPools.of(player).awarded(tree, key, base, player.level().getGameTime()));
    }
}
