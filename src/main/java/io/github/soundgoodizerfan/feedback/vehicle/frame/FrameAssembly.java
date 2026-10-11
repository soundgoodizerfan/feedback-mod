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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turns a welded {@link Frame} into a Sable body ({@code feedback_vehicles_spec.md} section 4,
 * <em>Bridging to Sable</em>). The body is the frame block alone -- the one block Sable needs to
 * have a body at all -- and the plates and tubes are {@link FrameBodies free boxes} welded to it,
 * so nothing is placed in the world and nothing but the frame block is lifted out of it.
 */
public final class FrameAssembly {

    public enum Result { ASSEMBLED, NOTHING_TO_ASSEMBLE, BLOCKED }

    private FrameAssembly() {
    }

    /**
     * Lifts the frame block into a Sable body and marks the frame assembled; its boxes appear on
     * the frame's next tick. Refuses if there is no plate or tube to make a box of.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos framePos, FrameBlockEntity be, Result[] result) {
        Frame frame = be.frame();
        if (frame.plates().isEmpty() && frame.tubes().isEmpty()) {
            result[0] = Result.NOTHING_TO_ASSEMBLE;
            return null;
        }
        be.setAssembled(true);
        result[0] = Result.ASSEMBLED;
        return SubLevelAssemblyHelper.assembleBlocks(level, framePos, List.of(framePos),
                new BoundingBox3i(framePos.getX(), framePos.getY(), framePos.getZ(),
                        framePos.getX(), framePos.getY(), framePos.getZ()));
    }
}
