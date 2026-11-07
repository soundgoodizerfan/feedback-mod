// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A {@link ControllerBlockEntity} that needs no power -- a development cheat, like
 * {@link io.github.soundgoodizerfan.feedback.item.DebugHelmetItem}, deliberately absent from the
 * mod's own creative tab (see {@code FItems.CREATIVE_CONTROLLER}). Same Punch Card, same program,
 * same evaluation; it just never waits on a shaft.
 */
public class CreativeControllerBlockEntity extends ControllerBlockEntity {

    public CreativeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_CONTROLLER.get(), pos, state);
    }

    @Override
    protected boolean powered() {
        return true;
    }
}
