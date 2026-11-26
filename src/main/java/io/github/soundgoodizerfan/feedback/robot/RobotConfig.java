// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server limits on robots (feedback-server.toml): how many may stand in a chunk and in a dimension, and how many chunks they may hold loaded. */
public final class RobotConfig {

    private RobotConfig() {
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue PER_CHUNK = B
            .comment("Most robots that may stand in one chunk; a robot that would be the next is refused.")
            .defineInRange("robotsPerChunk", 8, 1, 256);

    public static final ModConfigSpec.IntValue PER_DIMENSION = B
            .comment("Most robots that may stand in one dimension; a robot that would be the next is refused.")
            .defineInRange("robotsPerDimension", 64, 1, 4096);

    public static final ModConfigSpec.IntValue LOADED_CHUNKS = B
            .comment("Most chunks all robots together may hold loaded through a chunk-loading part; further robots halt when their chunk unloads.")
            .defineInRange("robotLoadedChunks", 16, 0, 1024);

    public static final ModConfigSpec.IntValue STEP_BUDGET = B
            .comment("Most robot program steps the whole server runs in one tick; a robot past it waits a tick and shows as slowed. Movement and work are not limited.")
            .defineInRange("stepBudgetPerTick", 120, 1, 100000);

    public static final ModConfigSpec SPEC = B.build();
}
