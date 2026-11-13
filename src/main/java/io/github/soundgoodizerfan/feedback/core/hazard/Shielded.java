// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import net.minecraft.resources.ResourceLocation;

/**
 * A container that keeps its contents' radiation in. It names the material it is
 * lined with and nothing else: how well that stops radiation is that material's density, read in
 * {@link Radiation#containerShield}, the same property armour and walls are judged by.
 */
public interface Shielded {

    ResourceLocation shieldMaterial();
}
