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

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
    private final int series;

    public GalvanicCellBlock(Properties properties, ResourceLocation anode, ResourceLocation cathode) {
        this(properties, anode, cathode, 1);
    }

    /** A pack of {@code series} cells in series: its voltage is the pair's times that, a pre-built block per band (ruled 2026-10-23). */
    public GalvanicCellBlock(Properties properties, ResourceLocation anode, ResourceLocation cathode, int series) {
        super(properties);
        this.anode = anode;
        this.cathode = cathode;
        this.series = series;
    }

    public int series() {
        return series;
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

    /** An ingot of the anode metal restores part of the anode. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        boolean anodeIngot = Material.find(MaterialTable.get().materials(), stack.getItem())
                .filter(l -> l.id().equals(anode) && l.form().equals("ingot")).isPresent();
        if (!anodeIngot)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GalvanicCellBlockEntity cell && cell.refill() && !player.isCreative())
            stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
