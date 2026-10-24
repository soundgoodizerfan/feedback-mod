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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Rearm;

import net.minecraft.world.entity.LivingEntity;

/**
 * Which techniques a fighter has spent and what will give them back (spec 3.2). A technique that fires calls
 * {@link #spend}; the kind of rearm comes from its node in the tree ({@link SkillTree#rearmOf}), so a stem's
 * variants share one clock unless a variant names its own. The events that rearm ({@code STRIKE},
 * {@code PARRY}, {@code STAGGER}) call {@link #on}; {@code TIME} runs out against the game clock.
 *
 * <p>Server side only and not persisted: a spent technique is combat-short state, and a stale entry is
 * harmless (compared against the clock or cleared by its event). Techniques with no node in any tree
 * (granted by gear alone) have no rule, so they are never spent.
 */
public final class Rearms {

    /** Ticks a {@code TIME} rearm takes before {@link Figure#REARM_TICKS}. */
    public static final int BASE_TICKS = 100;

    private record Spent(Rearm kind, long until, long at) {
    }

    private static final Map<UUID, Map<String, Spent>> SPENT = new HashMap<>();

    /** Game tick each fighter last dealt or took a blow, and how long out of a fight a strike rearm needs to come back by itself. */
    private static final Map<UUID, Long> FIGHT = new HashMap<>();
    public static final int QUIET_TICKS = 600;

    private Rearms() {
    }

    /** Whether {@code tech} may fire: never spent, or its rearm has happened. */
    public static boolean ready(LivingEntity who, String tech) {
        Map<String, Spent> mine = SPENT.get(who.getUUID());
        Spent s = mine == null ? null : mine.get(tech);
        if (s == null)
            return true;
        long now = who.level().getGameTime();
        // A technique waiting on a landed blow also comes back once the fighter has been out of a fight for 30 s.
        boolean quiet = s.kind() == Rearm.STRIKE && now - Math.max(FIGHT.getOrDefault(who.getUUID(), 0L), s.at()) >= QUIET_TICKS;
        if (s.kind() == Rearm.TIME && now >= s.until() || quiet) {
            mine.remove(tech);
            return true;
        }
        return false;
    }

    /**
     * What {@code tech} waits on, as one number for the screen: 0 ready, {@code 1} a landed blow, {@code 2} a parry,
     * {@code 3} a stagger, and {@code 10 + ticks left / 5} for a timed rearm.
     */
    public static int wait(LivingEntity who, String tech) {
        if (ready(who, tech))
            return 0;
        Spent s = SPENT.get(who.getUUID()).get(tech);
        return switch (s.kind()) {
            case STRIKE -> 1;
            case PARRY -> 2;
            case STAGGER -> 3;
            case TIME -> 10 + (int) Math.max(0, (s.until() - who.level().getGameTime()) / 5);
        };
    }

    /** {@code tech} fired: spend it until its rearm. A technique no tree node carries is not spent. */
    public static void spend(LivingEntity who, String tech) {
        for (SkillTree t : SkillTrees.get().trees())
            for (SkillTree.Node n : t.nodes())
                if (n.effect().unlock().filter(tech::equals).isPresent()) {
                    Rearm kind = t.rearmOf(n);
                    long until = who.level().getGameTime()
                            + Math.max(1, BASE_TICKS + Math.round(Skills.modifier(who, Figure.REARM_TICKS)));
                    SPENT.computeIfAbsent(who.getUUID(), k -> new HashMap<>()).put(tech, new Spent(kind, until, who.level().getGameTime()));
                    return;
                }
    }

    /** {@code who} dealt or took a blow: the fight clock for {@link #QUIET_TICKS}. */
    public static void fight(LivingEntity who) {
        FIGHT.put(who.getUUID(), who.level().getGameTime());
    }

    /** {@code who} did the thing {@code kind} names (struck, parried, staggered a target): everything it rearms comes back. */
    public static void on(LivingEntity who, Rearm kind) {
        Map<String, Spent> mine = SPENT.get(who.getUUID());
        if (mine != null)
            mine.values().removeIf(s -> s.kind() == kind);
    }
}
