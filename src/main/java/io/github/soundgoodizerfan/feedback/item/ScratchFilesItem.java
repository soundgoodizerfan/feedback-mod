// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * A set of files, each harder than the last. Drawn across an edge, the first that bites is the
 * first harder than it, so the reading is a ladder -- "between file 10 and file 20" -- and never a
 * figure finer than the files themselves (an instrument may not claim more than it can stand
 * behind, philosophy 10). Carried, like the calipers; the files' hardnesses are
 * {@code ModularTuning.SCRATCH_FILES}.
 */
public class ScratchFilesItem extends Item implements Instrument {

    public ScratchFilesItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.HARDNESS;
    }

    /** The ladder has no resolution of its own to quantise by; its rungs are the files. */
    @Override
    public float resolution(Quantity quantity) {
        return 1f;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.scratch_files");
    }
}
