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
