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
package io.github.soundgoodizerfan.feedback.machine.derrick;

import javax.annotation.Nullable;

import io.github.soundgoodizerfan.feedback.core.multiblock.Multiblocks;
import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One 8 m length of casing: a stack part that carries no state, only the reach it adds. */
public class DerrickCasingBlockEntity extends BlockEntity implements Part {

    @Nullable
    private Structure structure;

    public DerrickCasingBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DERRICK_CASING.get(), pos, state);
    }

    @Override
    public String role() {
        return DerrickStructure.CASING;
    }

    @Override
    public boolean connects(Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Override
    public RoleRule rule() {
        return DerrickStructure.RULE;
    }

    @Nullable
    @Override
    public Structure structure() {
        return structure;
    }

    @Override
    public void setStructure(@Nullable Structure structure) {
        this.structure = structure;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Multiblocks.load(this);
    }

    @Override
    public void setRemoved() {
        Multiblocks.remove(this);
        super.setRemoved();
    }
}
