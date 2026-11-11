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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Placing and removing micro-blocks ({@code docs/specs/feedback_micro_spec.md}, <em>Placing</em>): hold
 * a block and use it on a micro cell, and the voxel in front of the face you hit becomes that
 * block; sneak, and the voxel you hit goes, and its block comes back to the hand. A face on the
 * edge of the cell puts the voxel in the next cell, which is made if the block there is air.
 * A survival player pays one block a voxel; a creative one pays nothing.
 *
 * <p>The hook is the right click itself, caught before it reaches the block or the item, and only
 * when it lands on a {@link MicroCellBlock} with a block in hand. A sneak-click with a block in
 * hand never reaches the block's own {@code useItemOn} (vanilla keeps sneaking from using a block
 * while holding an item), so that is the only place removal can be; and every click anywhere
 * else is untouched, so building with blocks is as it was. Both sides take the click, so the
 * client does not predict a whole block where a voxel goes; the server does the work.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MicroPlacer {

    private static final int N = MicroCellBlockEntity.N;

    private MicroPlacer() {
    }

    /**
     * True if {@code stack} is a block that can be a voxel: one that is only a block state, with no
     * block entity, drawn from a model; or a rotation transmission part, which says so by being a
     * {@link MicroRotation} (its network membership is the cell's {@link MicroRotor}, not a block
     * entity). A voxel is a bare state, so a lift, an engine, a machine or a chest cannot be one.
     */
    public static boolean fits(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item))
            return false;
        if (item.getBlock() instanceof MicroRotation)
            return true;
        BlockState state = item.getBlock().defaultBlockState();
        return !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (event.getEntity().isSpectator() || !(level.getBlockState(event.getPos()).getBlock() instanceof MicroCellBlock)
                || !fits(event.getItemStack()))
            return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide)
            use(event.getEntity(), event.getItemStack(), event.getPos(), event.getHitVec());
    }

    /**
     * {@code held}, a block that {@link #fits}, used on micro cell {@code cell} where {@code hit}
     * says: the voxel in front of the face it hit takes the block. Taking one out is mining, with a
     * tool ({@link #onBreakSpeed}), never a sneak-click. False if nothing happened.
     */
    public static boolean use(Player player, ItemStack held, BlockPos cell, BlockHitResult hit) {
        if (!(player.level().getBlockEntity(cell) instanceof MicroCellBlockEntity be))
            return false;
        int hitVoxel = be.pick(hit.getLocation(), hit.getDirection());
        if (hitVoxel < 0)
            return false;
        int vx = cell.getX() * N + (hitVoxel & 3), vy = cell.getY() * N + (hitVoxel >> 2 & 3), vz = cell.getZ() * N + (hitVoxel >> 4);
        Direction face = hit.getDirection();
        int tx = vx + face.getStepX(), ty = vy + face.getStepY(), tz = vz + face.getStepZ();
        BlockState voxel = placementState(player, held, hit, be.get(hitVoxel), tx, ty, tz);
        return voxel != null && place(player, held, voxel, tx, ty, tz, face);
    }

    /**
     * The state {@code held}'s block takes in voxel ({@code tx}, {@code ty}, {@code tz}): its own
     * {@code getStateForPlacement}, asked as if the voxel were a full block at its cell's position --
     * the click point mapped into the voxel's unit cube, the face the one clicked -- so a shaft lies
     * along the face, a log along its axis, a stair by the half clicked. A block that reads its
     * neighbours while placing sees the full-size world around the cell, not the voxels, except a
     * rotation part, which is handed {@code against}, the voxel clicked (a cog takes a micro shaft's
     * axis). Null if the block refuses there.
     */
    @Nullable
    private static BlockState placementState(Player player, ItemStack held, BlockHitResult hit, @Nullable BlockState against,
                                             int tx, int ty, int tz) {
        BlockPos at = cellOf(tx, ty, tz);
        Vec3 inVoxel = hit.getLocation().scale(N).subtract(tx, ty, tz);
        BlockHitResult asBlock = new BlockHitResult(Vec3.atLowerCornerOf(at).add(inVoxel), hit.getDirection(),
                at.relative(hit.getDirection().getOpposite()), false);
        InteractionHand hand = player.getItemInHand(InteractionHand.OFF_HAND) == held
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        // The clicked position is the cell, whatever stands behind the face (vanilla would take an air
        // block there as the one being replaced).
        BlockPlaceContext context = new BlockPlaceContext(player, hand, held, asBlock) {
            @Override
            public BlockPos getClickedPos() {
                return at;
            }
        };
        Block block = ((BlockItem) held.getItem()).getBlock();
        return block instanceof MicroRotation part ? part.getStateForPlacement(context, against) : block.getStateForPlacement(context);
    }

    /**
     * Puts {@code voxel} at voxel coordinates ({@code vx}, {@code vy}, {@code vz}) on behalf of
     * {@code player}, who pays one of {@code paid} unless creative. False if the player may not
     * build there, the voxel is taken, or the block in its cell is not air.
     */
    public static boolean place(Player player, ItemStack paid, BlockState voxel, int vx, int vy, int vz, Direction face) {
        Level level = player.level();
        BlockPos at = cellOf(vx, vy, vz);
        if (!level.mayInteract(player, at) || !player.mayUseItemAt(at, face, paid) || !put(level, vx, vy, vz, voxel))
            return false;
        SoundType sound = voxel.getSoundType();
        level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, at, GameEvent.Context.of(player, voxel));
        if (!player.isCreative())
            paid.shrink(1);
        return true;
    }

    /**
     * Mining a micro cell takes out the one voxel the player is looking at, and hands its block over
     * as a placed-and-removed one; the cell stays unless that was its last. A look that misses the
     * cell (the server's ray differs a hair from the client's) takes the voxel nearest its middle,
     * so mining always finishes. False if the cell is not one.
     */
    public static boolean breakLooked(Player player, BlockPos cell) {
        if (!(player.level().getBlockEntity(cell) instanceof MicroCellBlockEntity be))
            return false;
        int voxel = lookedVoxel(player, cell, be);
        if (voxel < 0)
            return false;
        // Playtest 2026-11-02: one click was seen to take two voxels; the tick shows a held repeat from a double break.
        org.slf4j.LoggerFactory.getLogger("feedback.micro").info("[feedback] micro break cell={} voxel={} tick={} creative={}",
                cell, voxel, player.level().getGameTime(), player.isCreative());
        return remove(player, cell.getX() * N + (voxel & 3), cell.getY() * N + (voxel >> 2 & 3), cell.getZ() * N + (voxel >> 4));
    }

    /** The voxel of {@code cell} the player's look is on; failing that, the one nearest its middle; -1 if it is empty. */
    private static int lookedVoxel(Player player, BlockPos cell, MicroCellBlockEntity be) {
        if (player.pick(player.blockInteractionRange() + 1, 1f, false) instanceof BlockHitResult hit && hit.getBlockPos().equals(cell)) {
            int voxel = be.pick(hit.getLocation(), hit.getDirection());
            if (voxel >= 0)
                return voxel;
        }
        return be.pick(net.minecraft.world.phys.Vec3.atCenterOf(cell), Direction.UP);
    }

    /**
     * Mining a micro cell goes by the voxel looked at, not by the cell: that block's hardness, and the
     * tool it wants (a pickaxe for stone), so the right tool is the only way to break a voxel and
     * get its block. The cell block's own hardness is the divisor vanilla uses, so the speed is
     * scaled by the ratio; the cell asks for no tool itself.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getState().getBlock() instanceof MicroCellBlock) || event.getPosition().isEmpty())
            return;
        Player player = event.getEntity();
        BlockPos cell = event.getPosition().get();
        if (!(player.level().getBlockEntity(cell) instanceof MicroCellBlockEntity be))
            return;
        int index = lookedVoxel(player, cell, be);
        BlockState voxel = index < 0 ? null : be.get(index);
        if (voxel == null)
            return;
        float theirs = voxel.getDestroySpeed(player.level(), cell);
        if (theirs < 0) {
            event.setNewSpeed(0);
            return;
        }
        float speed = player.getDestroySpeed(voxel);
        // Vanilla divides by 30 with the right tool and 100 without; the cell asks for none, so it is 30.
        if (voxel.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(voxel))
            speed *= 30f / 100f;
        float own = event.getState().getDestroySpeed(player.level(), cell);
        event.setNewSpeed(speed * own / Math.max(theirs, 1e-3f));
    }

    private static boolean remove(Player player, int vx, int vy, int vz) {
        Level level = player.level();
        BlockPos at = cellOf(vx, vy, vz);
        if (!level.mayInteract(player, at))
            return false;
        BlockState was = take(level, vx, vy, vz);
        if (was == null)
            return false;
        SoundType sound = was.getSoundType();
        level.playSound(null, at, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_DESTROY, at, GameEvent.Context.of(player, was));
        // A voxel that wants a tool (stone wants a pickaxe) gives its block only to one; the rest is lost.
        if (!player.isCreative() && (!was.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(was))) {
            ItemStack back = new ItemStack(was.getBlock());
            if (!back.isEmpty() && !player.getInventory().add(back))
                player.drop(back, false);
        }
        return true;
    }

    /** The block position of the cell that voxel ({@code vx}, {@code vy}, {@code vz}) is in. */
    public static BlockPos cellOf(int vx, int vy, int vz) {
        return new BlockPos(Math.floorDiv(vx, N), Math.floorDiv(vy, N), Math.floorDiv(vz, N));
    }

    private static int indexOf(int vx, int vy, int vz) {
        return MicroCellBlockEntity.index(Math.floorMod(vx, N), Math.floorMod(vy, N), Math.floorMod(vz, N));
    }

    /**
     * Fills voxel ({@code vx}, {@code vy}, {@code vz}) with {@code voxel}, in world voxel
     * coordinates (a block is four on a side), making its cell if the block there is air or
     * replaceable. False if the voxel is taken or something else stands there.
     */
    public static boolean put(Level level, int vx, int vy, int vz, BlockState voxel) {
        BlockPos at = cellOf(vx, vy, vz);
        int i = indexOf(vx, vy, vz);
        BlockState there = level.getBlockState(at);
        if (there.getBlock() instanceof MicroCellBlock) {
            if (!(level.getBlockEntity(at) instanceof MicroCellBlockEntity cell) || cell.get(i) != null)
                return false;
            cell.set(i, voxel);
            cell.commit();
            return true;
        }
        if (level.isOutsideBuildHeight(at) || !(there.isAir() || there.canBeReplaced() && there.getFluidState().isEmpty()))
            return false;
        // The first voxel names the cell's state, so the cell is never in the world with another.
        level.setBlock(at, MicroCellBlock.stateFor(MicroCellBlockEntity.octants(1L << i), MicroCellBlockEntity.voxelKpg(voxel)),
                Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof MicroCellBlockEntity cell))
            return false;
        cell.set(i, voxel);
        cell.commit();
        return true;
    }

    /** Empties the voxel and returns the block that was in it, or null if there was none. The cell goes with its last voxel. */
    @Nullable
    public static BlockState take(Level level, int vx, int vy, int vz) {
        if (!(level.getBlockEntity(cellOf(vx, vy, vz)) instanceof MicroCellBlockEntity cell))
            return null;
        int i = indexOf(vx, vy, vz);
        BlockState was = cell.get(i);
        if (was == null)
            return null;
        cell.set(i, null);
        cell.commit();
        return was;
    }
}
