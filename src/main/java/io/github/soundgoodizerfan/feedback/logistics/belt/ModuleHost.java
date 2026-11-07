// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import io.github.soundgoodizerfan.feedback.logistics.sort.Band;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block entity that can hold one {@link BeltModule}: a belt cell, or the head roller (which takes only
 * {@link BeltModule#onHead} modules, the ones that need no band, wire or screen). The run and
 * {@code BeltModules} read modules only through this.
 */
public interface ModuleHost {

    BlockPos getBlockPos();

    BeltModule module();

    Direction face();

    Band band();

    int aperture();

    boolean engaged();

    boolean live();

    boolean flip();

    void soil();

    boolean dirty();

    void foul();

    boolean fouled();

    void pass(long time);

    float jetPressure();

    void install(BeltModule m, Direction side);

    boolean handUse(Player player, BlockHitResult hit);
}
