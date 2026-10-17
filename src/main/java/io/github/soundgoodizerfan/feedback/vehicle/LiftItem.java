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
package io.github.soundgoodizerfan.feedback.vehicle;

import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

/**
 * The Lift as carried ({@code feedback_vehicles_spec.md} section 4), Scrap Mechanic's way: use it
 * on any creation and the whole creation goes into the item -- every block, and what each block
 * holds -- and the creation is gone from the world. Use the carrying item on the ground and a
 * lift goes down with the creation on it, held. A Lift carrying nothing places a bare lift.
 *
 * <p>The creation is kept as a vanilla {@link StructureTemplate}, the structure block's own format,
 * so block entities (chests, tanks, a frame's plates and tubes) come back as they were with no
 * code of ours per block. Airs are left out, so placing it never clears anything it overlaps --
 * it refuses instead.
 */
public class LiftItem extends BlockItem {

    /** Placement flags: tell clients, skip neighbour shape updates mid-structure. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public LiftItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        CompoundTag carried = stack.get(FVehicles.CARRIED_CREATION.get());
        if (carried == null) {
            if (SableCompanion.INSTANCE.getContaining(level, ctx.getClickedPos()) == null)
                return super.useOn(ctx); // a bare lift
            if (level instanceof ServerLevel server && ctx.getPlayer() != null
                    && ServerSubLevelContainer.getContainer(server).getSubLevel(
                    SableCompanion.INSTANCE.getContaining(level, ctx.getClickedPos()).getUniqueId()) instanceof ServerSubLevel body)
                pickUp(server, body, ctx.getPlayer(), stack, ctx);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel server && ctx.getPlayer() != null)
            putDown(server, new BlockPlaceContext(ctx), carried, ctx.getPlayer(), stack);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Takes the vehicle {@code body} belongs to into one Lift from the stack in hand. Clicking a
     * swinging part takes the whole vehicle, and the parts are folded back into their frames first
     * (they are made again when it is next lifted). Two vehicles joined frame to frame are two
     * grids, and one template holds one: refused, until the joint is cut.
     */
    private static void pickUp(ServerLevel level, ServerSubLevel clicked, Player player, ItemStack stack, UseOnContext ctx) {
        List<ServerSubLevel> roots = Superobject.roots(level, Superobject.of(level, clicked));
        if (roots.size() > 1) {
            player.displayClientMessage(Component.translatable("message.feedback.lift.joined"), true);
            return;
        }
        ServerSubLevel body = roots.isEmpty() ? clicked : roots.get(0);
        for (FrameBlockEntity f : FrameBlockEntity.loadedOn(level))
            if (f.frame().locked() && SableCompanion.INSTANCE.getContaining(f) == body) {
                player.displayClientMessage(Component.translatable("message.feedback.lift.locked"), true);
                return;
            }
        for (FrameBlockEntity f : FrameBlockEntity.loadedOn(level))
            if (SableCompanion.INSTANCE.getContaining(f) == body) {
                f.breakCells(level); // the cell blocks too, or they would ride along in the template
                f.disassemble(level);
            }
        CompoundTag creation = capture(level, body);
        if (creation == null)
            return;
        ItemStack one = stack.split(1);
        one.set(FVehicles.CARRIED_CREATION.get(), creation);
        one.set(DataComponents.MAX_STACK_SIZE, 1); // a stack of carrying lifts would copy the creation
        if (stack.isEmpty())
            player.setItemInHand(ctx.getHand(), one);
        else if (!player.getInventory().add(one))
            player.drop(one, false);
        player.displayClientMessage(Component.translatable("message.feedback.lift.picked_up", creation.getInt("Count")), true);
    }

    /**
     * Saves every block of {@code body} into a template and removes the body. Containers are
     * emptied first, since what they hold is already in the template and would otherwise spill.
     */
    @org.jetbrains.annotations.Nullable
    public static CompoundTag capture(ServerLevel level, ServerSubLevel body) {
        var box = body.getPlot().getBoundingBox();
        BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
        Vec3i size = new Vec3i(box.maxX() - box.minX() + 1, box.maxY() - box.minY() + 1, box.maxZ() - box.minZ() + 1);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.AIR);

        int count = 0;
        BlockPos seat = null;
        double bestSeat = Double.MAX_VALUE;
        double cx = size.getX() / 2.0, cz = size.getZ() / 2.0;
        for (BlockPos p : BlockPos.betweenClosed(min, min.offset(size).offset(-1, -1, -1))) {
            if (level.getBlockState(p).isAir())
                continue;
            count++;
            BlockPos local = p.subtract(min);
            // The block that sits on the lift: lowest layer, nearest the middle.
            double score = local.getY() * 1e6 + Math.pow(local.getX() + 0.5 - cx, 2) + Math.pow(local.getZ() + 0.5 - cz, 2);
            if (score < bestSeat) {
                bestSeat = score;
                seat = local.immutable();
            }
        }
        if (seat == null)
            return null;

        LiftBlockEntity.forget(level, body.getUniqueId());
        for (BlockPos p : BlockPos.betweenClosed(min, min.offset(size).offset(-1, -1, -1))) {
            Clearable.tryClear(level.getBlockEntity(p));
            if (!level.getBlockState(p).isAir())
                level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS | Block.UPDATE_SUPPRESS_DROPS);
        }
        if (!body.isRemoved())
            ServerSubLevelContainer.getContainer(level).removeSubLevel(body, SubLevelRemovalReason.REMOVED);

        CompoundTag tag = template.save(new CompoundTag());
        tag.put("Seat", NbtUtils.writeBlockPos(seat));
        tag.putInt("Count", count);
        return tag;
    }

    /** Puts the carried creation down on a new lift where a block would go; the stack pays one Lift. */
    private static void putDown(ServerLevel level, BlockPlaceContext ctx, CompoundTag creation, Player player, ItemStack stack) {
        String result = ctx.canPlace() ? place(level, ctx.getClickedPos(), creation) : "no_room";
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof LiftBlockEntity be && result.equals("lifted")) {
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.feedback.lift.lifted", be.height()), true);
        } else
            player.displayClientMessage(Component.translatable("message.feedback.lift." + result, 0), true);
    }

    /**
     * Places a lift at {@code lift}, {@code creation} on top of it, and lifts it. Returns the
     * lift's message key -- {@code lifted} on success -- or {@code no_room} if anything stands in
     * the creation's box, in which case nothing is placed.
     */
    public static String place(ServerLevel level, BlockPos lift, CompoundTag creation) {
        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK), creation);
        BlockPos seat = NbtUtils.readBlockPos(creation, "Seat").orElse(BlockPos.ZERO);
        BlockPos origin = lift.above().subtract(seat);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        // The creation's whole box must be clear, not only its blocks: simpler, and a vehicle's
        // box is mostly vehicle anyway.
        var box = template.getBoundingBox(settings, origin);
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).canBeReplaced())
                return "no_room";
        level.setBlock(lift, FVehicles.LIFT.get().defaultBlockState(), Block.UPDATE_ALL);
        template.placeInWorld(level, origin, origin, settings, level.random, FLAGS);
        return level.getBlockEntity(lift) instanceof LiftBlockEntity be ? be.lift(level) : "empty";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag carried = stack.get(FVehicles.CARRIED_CREATION.get());
        if (carried != null)
            tooltip.add(Component.translatable("item.feedback.lift.carrying", carried.getInt("Count")));
    }
}
