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
package io.github.soundgoodizerfan.feedback.machine.cell;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A galvanic cell: two different metals and something between them, and a potential between the
 * metals. The block is given the two electrodes it is built from and nothing else; the voltage is
 * read from their material files ({@code electrical.electrode_potential}), so no cell names a
 * figure and a different pair is one more registration line (mechanics 6.8: chemical to
 * {@code Eu} at one block you can point at).
 */
public class GalvanicCellBlock extends Block implements EntityBlock {

    private final ResourceLocation anode;
    private final ResourceLocation cathode;

    public GalvanicCellBlock(Properties properties, ResourceLocation anode, ResourceLocation cathode) {
        super(properties);
        this.anode = anode;
        this.cathode = cathode;
    }

    public ResourceLocation anode() {
        return anode;
    }

    public ResourceLocation cathode() {
        return cathode;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GalvanicCellBlockEntity(pos, state);
    }
}
