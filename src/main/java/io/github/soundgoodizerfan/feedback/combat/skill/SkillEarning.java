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
 * <li>Observation: a hit with any projectile weapon (gun, bow, crossbow) at or past {@link SkillTuning#OBSERVE_RANGE}
 * blocks. Arrows are paid in {@link BowCraft}; the weapon is found by type ({@code ProjectileWeaponItem}), never an id.</li>
 * <li>Proficiency: surviving a blow struck by something, per attacker. Fire ticks, falls and other sourceless damage
 * do not pay, and a blow that kills pays nothing.</li>
 * <li>Chaos: variety. A new kind of disorder left on a target ({@link #disorder}: burning, armour worn, weapon stripped,
 * knocked into another body, blinded, tripped...) pays once, and each kind decays per target, so one trick repeated on
 * one mob pays little.</li>
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

    /** {@code player} left {@code target} in a {@code kind} of disorder: paid per kind per target, diminishing as that pair repeats. */
    public static void disorder(ServerPlayer player, LivingEntity target, String kind) {
        award(player, CHAOS, kind + "/" + target.getStringUUID(), SkillTuning.DISORDER);
    }

    /** {@code who} salvaged something (dismantled a worn part, brought something down with a placed charge): a small trickle, decaying per {@code key} so repeating one thing earns nothing. */
    public static void salvaged(ServerPlayer who, String key) {
        award(who, CHAOS, "salvage/" + key, SkillTuning.SALVAGE);
    }

    /** {@code who} has delivered a measure of work into the vehicle body {@code body}; repeats on one body decay, and a braked body takes no work at all. */
    public static void pushed(LivingEntity who, java.util.UUID body) {
        award(who, BRUTE, body.toString(), SkillTuning.PUSH);
    }

    /** {@code who} delivered aid (healing, a haul, stamina, a cut rope) to {@code ally}: a trickle per ally, decaying so standing by one friend farms little. */
    public static void aided(ServerPlayer who, ServerPlayer ally) {
        award(who, PROFICIENCY, "aid/" + ally.getStringUUID(), SkillTuning.AID);
    }

    /** {@code who} struck a clean streak at the forge, shaping {@code variant}: a trickle per variant, decaying so one idle bar farms little. */
    public static void forged(ServerPlayer who, String variant) {
        award(who, ONSLAUGHT, "forge/" + variant, SkillTuning.FORGE_STREAK);
    }

    /** {@code who} took a dose of a workshop hazard ({@code kind}: fume, radiation, food) and carried on: a trickle per kind, decaying so standing in it farms little. */
    public static void hazardSurvived(ServerPlayer who, String kind) {
        award(who, PROFICIENCY, "hazard/" + kind, SkillTuning.HAZARD);
    }

    /** {@code who} cranked a loaded crank at {@code pos}: a trickle per crank, decaying, so idling at one earns nothing. */
    public static void cranked(ServerPlayer who, net.minecraft.core.BlockPos pos) {
        award(who, ANCHOR, "crank/" + pos.asLong(), SkillTuning.CRANK);
    }

    /** {@code who} held footing against a tether {@code source}'s pull; paid per source and decaying, so a long reel pays once. */
    public static void heldPull(ServerPlayer who, java.util.UUID source) {
        award(who, ANCHOR, "pull/" + source, SkillTuning.HELD_PULL);
    }

    /** {@code who} passed a vibration listener at {@code pos} that would have heard them but for a hushed step or hand; per listener, decaying. */
    public static void hushed(ServerPlayer who, net.minecraft.world.phys.Vec3 pos) {
        award(who, VEIL, "hush/" + net.minecraft.core.BlockPos.containing(pos).asLong(), SkillTuning.HUSHED);
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

    static void award(LivingEntity who, String tree, String key, int base) {
        if (!(who instanceof ServerPlayer player) || player.isSpectator())
            return;
        SkillPools.set(player, SkillPools.of(player).awarded(tree, key, base, player.level().getGameTime()));
    }
}
