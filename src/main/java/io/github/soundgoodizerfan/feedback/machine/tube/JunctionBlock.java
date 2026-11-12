// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, j.getBlockPos(), out);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JunctionBlockEntity(pos, state);
    }
}
