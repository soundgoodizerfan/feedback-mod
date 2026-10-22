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

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * What combat code asks of the skill trees. Only players have pools: any other entity reads as no
 * modifier and nothing unlocked (mobs adopting the kit later go through this same API).
 *
 * <p>{@link #unlocked} is where "bought OR a worn item grants the move" gets decided.
 */
public final class Skills {

    private Skills() {
    }

    /** Sum of the bought figure-modifier nodes' amounts for {@code figure}; 0 for a non-player. */
    public static float modifier(LivingEntity who, Figure figure) {
        if (!(who instanceof Player player))
            return 0f;
        SkillPools pools = SkillPools.of(player);
        float sum = 0f;
        for (SkillTree t : SkillTrees.get().trees())
            for (SkillTree.Node n : t.nodes())
                if (n.effect().figure().filter(figure::equals).isPresent() && pools.has(t.tree(), n.id()))
                    sum += n.effect().amount();
        return sum;
    }

    /**
     * Whether {@code who} has the move {@code move}: a bought unlock node, or worn gear that grants it (plan
     * 2.8: the move id as a {@code feedback:} grant, same as {@code feedback:punch_projectiles}). Server-side
     * checks only; it is never a reason to refuse a machine or a recipe.
     */
    public static boolean unlocked(LivingEntity who, String move) {
        if (!(who instanceof Player player))
            return false;
        if (!io.github.soundgoodizerfan.feedback.combat.gear.CombatGear
                .granting(who, io.github.soundgoodizerfan.feedback.Feedback.id(move)).isEmpty())
            return true;
        SkillPools pools = SkillPools.of(player);
        for (SkillTree t : SkillTrees.get().trees())
            for (SkillTree.Node n : t.nodes())
                if (n.effect().unlock().filter(move::equals).isPresent() && pools.has(t.tree(), n.id()))
                    return true;
        return false;
    }

    /** Parry tuning hook: the held weapon's figures with the bought modifiers applied. */
    public static io.github.soundgoodizerfan.feedback.combat.parry.ParryStats tune(LivingEntity who,
            io.github.soundgoodizerfan.feedback.combat.parry.ParryStats st) {
        return new io.github.soundgoodizerfan.feedback.combat.parry.ParryStats(
                Math.max(1, st.window() + Math.round(modifier(who, Figure.PARRY_WINDOW))),
                Math.max(1, st.recovery() + Math.round(modifier(who, Figure.PARRY_RECOVERY))),
                st.coneHalfDeg(), st.mass(), st.hand(), st.stack(),
                st.projectilesOnly(), st.redirect(), st.knock());
    }

    /** Multiplier on the grapple's reel speed: {@code GRAPPLE_REEL} nodes add a fraction (0.2 is a fifth faster). */
    public static float reelScale(LivingEntity who) {
        return Math.max(0.25f, 1 + modifier(who, Figure.GRAPPLE_REEL));
    }

    /** Extra ticks a grab holds before the hand tires. */
    public static int grabHoldTicks(LivingEntity who) {
        return Math.max(0, Math.round(modifier(who, Figure.GRAB_HOLD)));
    }

    /** Multiplier on the roll's launch speed: distance scales with it. */
    public static double rollSpeedScale(LivingEntity who) {
        return Math.max(0.25, 1 + modifier(who, Figure.ROLL_DISTANCE) / SkillTuning.ROLL_BASE_BLOCKS);
    }

    /** Extra ticks of roll i-frames (and roll length with them). */
    public static int rollIFrames(LivingEntity who) {
        return Math.max(0, Math.round(modifier(who, Figure.I_FRAMES)));
    }

    /** Buys {@code node} of {@code tree} if it exists, is not owned, its requirements are owned and the pool covers it. */
    public static boolean buy(ServerPlayer player, String tree, String node) {
        var found = SkillTrees.get().find(tree).flatMap(t -> t.node(node));
        SkillPools pools = SkillPools.of(player);
        if (found.isEmpty() || pools.has(tree, node) || pools.points(tree) < found.get().cost()
                || !found.get().requires().stream().allMatch(r -> pools.has(tree, r)))
            return false;
        SkillPools.set(player, pools.bought(tree, node, found.get().cost()));
        return true;
    }

    /** Un-buys everything and refunds each node's cost to its tree's pool. @return whether anything was owned. */
    public static boolean respec(ServerPlayer player) {
        SkillPools pools = SkillPools.of(player);
        if (pools.unlocked().isEmpty())
            return false;
        Set<String> owned = new HashSet<>(pools.unlocked());
        SkillPools next = pools.withUnlocked(Set.of());
        for (SkillTree t : SkillTrees.get().trees())
            for (SkillTree.Node n : t.nodes())
                if (owned.contains(SkillPools.key(t.tree(), n.id())))
                    next = next.withPoints(t.tree(), next.points(t.tree()) + n.cost());
        SkillPools.set(player, next);
        return true;
    }
}
