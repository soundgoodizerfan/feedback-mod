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
package io.github.soundgoodizerfan.feedback.core.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Implemented by a block entity that may join a {@link Structure}. A part reports what it is (a
 * role, a plain string a pack's own block can also declare) and which faces it connects on; it
 * never ticks for the structure -- the structure ticks once. Call {@link Multiblocks#load} from
 * {@code onLoad} and {@link Multiblocks#remove} from {@code setRemoved}, server side only.
 * <p>
 * Implementors must be block entities.
 */
public interface Part {

    /** What this part is to a rule or a pattern: "tray", "reboiler", "casing". */
    String role();

    /** Whether this part joins a neighbour across {@code face}. Both sides must agree. */
    default boolean connects(Direction face) {
        return false;
    }

    /** Mode B: the rule this part self-joins under; null if it only appears in controller patterns. */
    @Nullable
    default RoleRule rule() {
        return null;
    }

    /** The structure currently holding this part, or null. Plain field accessor. */
    @Nullable
    Structure structure();

    void setStructure(@Nullable Structure structure);

    default BlockPos pos() {
        return ((BlockEntity) this).getBlockPos();
    }
}
