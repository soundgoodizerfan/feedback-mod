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

import java.util.Map;

import net.minecraft.util.Mth;

/**
 * The colour of each skill pool (RGB, no alpha). Plain data with no client types, so the skill tree, the cast radial, a
 * tooltip or a server message can all use the same hue for the same tree.
 */
public final class TreeHue {

    public static final int ANCHOR = 0x4FA3E0, BRUTE = 0xE0583C, CHAOS = 0xE05CC8, OBSERVATION = 0x3CE0B0,
            ONSLAUGHT = 0xFF9A2E, PROFICIENCY = 0xE8D04A, VEIL = 0x6C7DFF;

    private static final Map<String, Integer> NAMED = Map.of(
            "anchor", ANCHOR, "brute", BRUTE, "chaos", CHAOS, "observation", OBSERVATION,
            "onslaught", ONSLAUGHT, "proficiency", PROFICIENCY, "veil", VEIL);

    private TreeHue() {
    }

    /** The colour of a pool: named for the trees we ship, else a hue taken from its name. */
    public static int of(String pool) {
        Integer named = NAMED.get(pool);
        return named != null ? named : Mth.hsvToRgb(Math.floorMod(pool.hashCode(), 360) / 360f, 0.6f, 0.95f);
    }

    /** The colour of the tree that unlocks this technique, or {@code fallback} when none does. */
    public static int ofTechnique(String technique, int fallback) {
        for (SkillTree tree : SkillTrees.get().trees())
            if (tree.nodes().stream().anyMatch(n -> n.effect().unlock().filter(technique::equals).isPresent()))
                return of(tree.pool());
        return fallback;
    }
}
