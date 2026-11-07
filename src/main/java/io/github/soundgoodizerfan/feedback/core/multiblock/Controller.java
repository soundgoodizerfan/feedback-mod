// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.multiblock;

import net.minecraft.resources.ResourceLocation;

/** Mode A: a part that owns a datapack pattern ({@code data/<ns>/multiblock/<path>.json}) and walks it. */
public interface Controller extends Part {

    /** Pattern id, {@code <ns>:<path>}. */
    ResourceLocation pattern();

    /** The shared-state holder for this machine; override with a subclass of {@link Structure}. */
    default Structure createStructure() {
        return new Structure();
    }

    @Override
    default String role() {
        return "controller";
    }
}
