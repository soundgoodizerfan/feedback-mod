// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

import net.minecraft.world.level.block.Block;

/** Finned aluminium. Set against a converter, it sheds the converter's heat to air faster (counted by {@link ConverterBlockEntity}). */
public class HeatsinkBlock extends Block {

    public HeatsinkBlock(Properties properties) {
        super(properties);
    }
}
