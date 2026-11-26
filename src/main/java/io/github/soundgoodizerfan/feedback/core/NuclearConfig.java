// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server switch for the one thing in the mod made only to destroy (feedback-nuclear.toml): the fission device. Off by default. */
public final class NuclearConfig {

    private NuclearConfig() {
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ARMS = B
            .comment("Whether a fission device may be set off. Off: it sits inert and says so; nothing else changes.")
            .define("nuclearArms", false);

    public static final ModConfigSpec SPEC = B.build();
}
