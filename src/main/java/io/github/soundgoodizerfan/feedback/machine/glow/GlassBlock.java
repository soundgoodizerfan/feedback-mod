// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

import io.github.soundgoodizerfan.feedback.item.FormBlock;

import net.minecraft.resources.ResourceLocation;

/** A block of a glass material: brittle and clear, otherwise a form block like any other. */
public class GlassBlock extends FormBlock {

    public GlassBlock(ResourceLocation material, String form) {
        super(material, form, glassProperties());
    }
}
