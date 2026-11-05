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
package io.github.soundgoodizerfan.feedback.machine.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * The address switch of a tube run. A dye on a face names the colour that leaves through it (the same
 * colour again clears it); a programmed junction also marks one face as the return line by an empty
 * hand; an empty sneaking hand clears the lot. It is wide bore so pods pass.
 */
public class JunctionBlock extends TubeBlock {

    private final boolean programmed;

    public JunctionBlock(Properties properties, boolean programmed) {
        super(properties, Kind.WIDE);
        this.programmed = programmed;
    }

    public boolean programmed() {
        return programmed;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof JunctionBlockEntity j) {
            if (!level.isClientSide) {
                Direction face = hit.getDirection();
                j.setFace(face, j.face(face) == dye.getDyeColor() ? null : dye.getDyeColor());
                report(player, j);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof JunctionBlockEntity j) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown())
                    j.clear();
                else if (programmed)
                    j.toggleReturn(hit.getDirection());
                report(player, j);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void report(Player player, JunctionBlockEntity j) {
        var out = Component.empty();
        for (Direction d : Direction.values()) {
            var colour = j.face(d);
            out.append(Component.translatable("message.feedback.junction.face", d.getName().substring(0, 1).toUpperCase(),
                    colour == null ? Component.translatable("message.feedback.junction.none")
                            : Component.translatable("color.minecraft." + colour.getName()),
                    d.get3DDataValue() == j.returnFace() ? "*" : "")).append(" ");
        }
        player.displayClientMessage(out, true);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JunctionBlockEntity(pos, state);
    }
}
