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
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Starts a build ({@code docs/dev/vehicle.md}): used on a block, it puts a micro cell where
 * a block would go, with one voxel of stone in it where you pointed. Every voxel after that is
 * placed by holding a block against the build ({@link MicroPlacer}).
 */
public class MicroCoreItem extends Item {

    private static final int N = MicroCellBlockEntity.N;
    /** What the first voxel is made of. */
    private static final BlockState START = Blocks.STONE.defaultBlockState();

    public MicroCoreItem(Properties properties) {
        super(properties);
    }

    /** Sneaking with the core in the air steps its mode, as on a block. */
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player,
                                                                                           net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide)
            showMode(player, stack, MicroTool.cycle(stack));
        return player.isShiftKeyDown() ? net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide)
                showMode(player, context.getItemInHand(), MicroTool.cycle(context.getItemInHand()));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // In mirror mode a click on a build sets a plane through the face clicked, or takes it away.
        if (player != null && MicroTool.mode(context.getItemInHand()) == MicroTool.Mode.MIRROR
                && level.getBlockEntity(context.getClickedPos()) instanceof MicroCellBlockEntity cell) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            int hit = cell.pick(context.getClickLocation(), context.getClickedFace());
            if (hit < 0)
                return InteractionResult.FAIL;
            BlockPos c = context.getClickedPos();
            BlockPos voxel = new BlockPos(c.getX() * N + (hit & 3), c.getY() * N + (hit >> 2 & 3), c.getZ() * N + (hit >> 4));
            boolean ok = MicroTool.togglePlane(context.getItemInHand(), voxel, context.getClickedFace());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    ok ? "message.feedback.micro.planes" : "message.feedback.micro.planes_full",
                    MicroTool.planes(context.getItemInHand()).size()), true);
            return InteractionResult.CONSUME;
        }
        // On a micro cell it adds a stone voxel in front of the face it hit, so the core builds on its
        // own cell and not only one voxel to a block.
        if (player != null && level.getBlockEntity(context.getClickedPos()) instanceof MicroCellBlockEntity cell) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            int hit = cell.pick(context.getClickLocation(), context.getClickedFace());
            if (hit < 0)
                return InteractionResult.FAIL;
            BlockPos c = context.getClickedPos();
            Direction face = context.getClickedFace();
            boolean placed = MicroPlacer.place(player, context.getItemInHand(), START,
                    c.getX() * N + (hit & 3) + face.getStepX(), c.getY() * N + (hit >> 2 & 3) + face.getStepY(),
                    c.getZ() * N + (hit >> 4) + face.getStepZ(), face);
            return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        BlockPlaceContext where = new BlockPlaceContext(context);
        if (player == null || !where.canPlace())
            return InteractionResult.FAIL;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        // The click is on the new cell's edge or in it: the voxel it points at is the first.
        BlockPos at = where.getClickedPos();
        Vec3 click = context.getClickLocation();
        boolean placed = MicroPlacer.place(player, context.getItemInHand(), START,
                at.getX() * N + voxelOf(click.x - at.getX()), at.getY() * N + voxelOf(click.y - at.getY()),
                at.getZ() * N + voxelOf(click.z - at.getZ()), context.getClickedFace());
        return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /** The voxel along one axis that {@code local}, in blocks from the cell's corner, falls in. */
    private static int voxelOf(double local) {
        return Mth.clamp((int) Math.floor(local * N), 0, N - 1);
    }

    /** The build modes as a strip, the new one lit, with what it does beneath. */
    private static void showMode(Player player, net.minecraft.world.item.ItemStack stack, MicroTool.Mode mode) {
        String name = mode.name().toLowerCase(java.util.Locale.ROOT);
        io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, stack.getHoverName(),
                io.github.soundgoodizerfan.feedback.net.AimPayloads.keys("message.feedback.micro.mode.short.", "single", "line", "box", "mirror", "wedge"),
                mode.ordinal(), net.minecraft.network.chat.Component.translatable("message.feedback.micro.mode." + name));
    }
}
