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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.control.program;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * What a Controller with a given set of fitted upgrades can run: the card cap (Card Racks), the
 * variable cap (Memory Drums) and whether typed delay lengths are allowed (Escapement). The
 * Controller checks a card against its own fitted figures; the Programmer, which does not know
 * which Controller a card will end up in, checks against {@link #MAX} (everything fitted).
 */
public record ControllerLimits(int nodes, int variables, boolean typedDelays) {

    /** A bare Controller. */
    public static final ControllerLimits BASE = of(0, 0, false);

    /** A Controller with every upgrade fitted: the ceiling no card can pass. */
    public static final ControllerLimits MAX = of(FTuning.MEMORY_DRUM_MAX, FTuning.CARD_RACK_MAX, true);

    public static ControllerLimits of(int drums, int racks, boolean escapement) {
        return new ControllerLimits(FTuning.CONTROLLER_MAX_NODES + racks * FTuning.CARD_RACK_NODES,
                FTuning.CONTROLLER_BASE_VARIABLES + drums * FTuning.MEMORY_DRUM_VARIABLES, escapement);
    }

    /** Why {@code graph} does not fit these limits (a {@code feedback.programmer.error} key), or empty. */
    public Optional<String> exceededBy(ProgramGraph graph) {
        if (graph.nodes().size() > nodes)
            return Optional.of("too_many_nodes");
        if (graph.variables().size() > variables)
            return Optional.of("too_many_variables");
        if (!typedDelays && graph.nodes().stream().anyMatch(n -> n instanceof DelayNode d && !d.named()))
            return Optional.of("typed_delay_locked");
        return Optional.empty();
    }

    /** Fewest Card Racks a Controller needs to hold {@code graph}'s cards. */
    public static int racksNeeded(ProgramGraph graph) {
        return Math.max(0, Math.ceilDiv(graph.nodes().size() - FTuning.CONTROLLER_MAX_NODES, FTuning.CARD_RACK_NODES));
    }

    /** Fewest Memory Drums a Controller needs to hold {@code graph}'s variables. */
    public static int drumsNeeded(ProgramGraph graph) {
        return Math.max(0, Math.ceilDiv(graph.variables().size() - FTuning.CONTROLLER_BASE_VARIABLES, FTuning.MEMORY_DRUM_VARIABLES));
    }
}
