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

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointBlock;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A full-size block that holds up to 64 micro-blocks ({@code docs/dev/vehicle.md}): the
 * cell Sable already understands, so a build of cells lifts into a body like any other build.
 * The voxels are in its {@link MicroCellBlockEntity}.
 *
 * <p>Sable asks two kinds of question, and a block entity can answer only one. Players, the
 * look-at outline and entity collision ask per position, so there the entity answers with the
 * filled voxels merged into boxes. Sable's physics collider and its mass are read off the
 * {@link BlockState} alone (its collider bakery memoises per state and hands the block no
 * position; {@code PhysicsBlockPropertyHelper} reads a table keyed by state), so what the body
 * meets the world with has to be named by the state, as {@code ChassisCellBlock} does it:
 * <ul>
 * <li>{@link #OCTANTS}: which of the cell's eight half-block octants hold a voxel. The physics
 * collider is those octants whole -- half-block resolution, a thin plate collides a half block
 * thick -- where players and the outline get the exact voxels.</li>
 * <li>{@link #LOAD}: a mass class, one of {@link #LOADS} on a geometric scale, through
 * {@code physics_block_properties/micro_cell.json}. The cell's mass is its voxels' summed
 * ({@link MicroCellBlockEntity#kpg}), rounded to the nearest class; its centre of mass is Sable's
 * default, the middle of the octants' bounds.</li>
 * </ul>
 * The state is kept in step by {@link MicroCellBlockEntity#commit}; an empty cell does not exist.
 */
public class MicroCellBlock extends BaseEntityBlock implements BlockSubLevelCollisionShape {

    public static final MapCodec<MicroCellBlock> CODEC = simpleCodec(MicroCellBlock::new);

    /**
     * Bit {@code x + 2y + 4z} set: the half-block octant at (x, y, z) holds a voxel. Never zero: Sable
     * gives a state with an empty collision shape no mass, so a cleared mask falls back to the cell's own.
     */
    public static final IntegerProperty OCTANTS = IntegerProperty.create("octants", 1, 255);
    public static final int LOADS = 96;
    public static final IntegerProperty LOAD = IntegerProperty.create("load", 0, LOADS - 1);
    /** Sable mass at {@code load=0}, kpg; each step up is the sixth root of two times it. */
    public static final double MASS_AT_ZERO = 0.002;

    /** Vanilla's name for a drop a block fills in itself (a shulker box's contents). */
    private static final ResourceLocation CONTENTS = ResourceLocation.withDefaultNamespace("contents");
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    public MicroCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OCTANTS, 255).setValue(LOAD, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OCTANTS, LOAD);
    }

    /** Drawn by its block entity renderer, voxel by voxel; the chunk mesh has nothing of it. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** The Sable mass {@code load} stands for, kpg; keep in step with the properties table. */
    public static double massOf(int load) {
        return MASS_AT_ZERO * Math.pow(2, load / 6.0);
    }

    /** The load whose mass is nearest {@code kpg}, by ratio. */
    public static int loadFor(double kpg) {
        int k = (int) Math.round(6 * Math.log(Math.max(kpg, 1e-9) / MASS_AT_ZERO) / Math.log(2));
        return Math.clamp(k, 0, LOADS - 1);
    }

    /** The state of a cell whose voxels fill {@code octants} and weigh {@code kpg}. */
    public static BlockState stateFor(int octants, double kpg) {
        return FVehicles.MICRO_CELL.get().defaultBlockState().setValue(OCTANTS, octants).setValue(LOAD, loadFor(kpg));
    }

    /** The half-block boxes of {@code octants}. */
    public static VoxelShape octantShape(int octants) {
        VoxelShape s = SHAPES[octants];
        if (s == null) {
            s = Shapes.empty();
            for (int i = 0; i < 8; i++)
                if ((octants & (1 << i)) != 0) {
                    int x = (i & 1) * 8, y = (i >> 1 & 1) * 8, z = (i >> 2 & 1) * 8;
                    s = Shapes.or(s, Block.box(x, y, z, x + 8, y + 8, z + 8));
                }
            SHAPES[octants] = s = s.optimize();
        }
        return s;
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        return octantShape(state.getValue(OCTANTS));
    }

    /**
     * The voxels at this position, merged -- or the octants where this entity is not there to ask
     * (Sable's state-keyed solidity check, a client that has not been sent it yet), so a cell
     * always counts as solid.
     */
    private static VoxelShape fine(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell && cell.hasShape())
            return cell.shape();
        return octantShape(state.getValue(OCTANTS));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fine(state, level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fine(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /**
     * A player mining a cell takes out one voxel, the one looked at, and the cell stays; only its
     * last voxel takes the cell with it. The block is never removed here, so no loot of its own
     * drops; {@link MicroPlacer#breakLooked} hands the voxel's block over.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide)
            return false;
        if (!MicroPlacer.breakLooked(player, pos))
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        // The client took the cell out in advance; say it is still there.
        level.sendBlockUpdated(pos, state, level.getBlockState(pos), 3);
        return false;
    }

    /** Broken whole by anything but a player, a cell gives back its voxels' blocks, stacked, not one item entity each. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MicroCellBlockEntity cell)
            params = params.withDynamicDrop(CONTENTS, drop -> {
                Map<Item, Integer> count = new LinkedHashMap<>();
                cell.forEachVoxel(voxel -> count.merge(voxel.getBlock().asItem(), 1, Integer::sum));
                count.forEach((item, n) -> {
                    int left = item == Items.AIR ? 0 : n;
                    while (left > 0) {
                        int stack = Math.min(left, item.getDefaultMaxStackSize());
                        drop.accept(new ItemStack(item, stack));
                        left -= stack;
                    }
                });
            });
        return super.getDrops(state, params);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroCellBlockEntity(pos, state);
    }

    /** Server: drives the micro bearings a cell holds (a cell without one returns at once). */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FVehicles.MICRO_CELL_ENTITY.get(), MicroCellBlockEntity::tickServer);
    }

    /** Sable rewrote this cell's collider for the neighbour's change: write the voxel one again. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                   BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell)
            cell.applyCollider();
    }

    /** The brightest of the cell's voxels: a glowing block lights the space it is in. */
    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell ? cell.light() : 0;
    }

    /** A new full-size Bearing in hand on an assembled joint voxel fits it: the joint and its brake pad are as built. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof JointLinkItem && level.getBlockEntity(pos) instanceof MicroCellBlockEntity linked) {
            int p = linked.pick(hit.getLocation(), hit.getDirection());
            if (p >= 0 && !level.isClientSide)
                MicroLinks.use(player, linked, p);
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.BeltItem && level.getBlockEntity(pos) instanceof MicroCellBlockEntity belted) {
            int p = belted.pick(hit.getLocation(), hit.getDirection());
            if (p >= 0 && belted.get(p) != null && belted.get(p).getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlock) {
                if (!level.isClientSide)
                    MicroBelts.select(player, belted, p);
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.BEARING.get()) || !(level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell))
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int i = cell.pick(hit.getLocation(), hit.getDirection());
        var joint = i < 0 ? null : cell.bearing(i);
        if (joint == null)
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (joint.condition() >= 1 && joint.pad() >= 1) {
                player.displayClientMessage(Component.translatable("message.feedback.joint.as_built"), true);
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(false);
            }
            joint.repair();
            cell.setChanged();
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.feedback.joint.repaired"), true);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** An empty hand on a micro bearing voxel assembles or puts back what it turns ({@link MicroBearings}). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell))
            return InteractionResult.PASS;
        int i = cell.pick(hit.getLocation(), hit.getDirection());
        // Sneaking on a differential voxel steps it through open, limited slip and locked.
        if (i >= 0 && player.isShiftKeyDown() && cell.get(i) != null
                && cell.get(i).getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock) {
            if (!level.isClientSide) {
                BlockState next = cell.get(i).setValue(io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock.MODE,
                        (cell.get(i).getValue(io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock.MODE) + 1) % 3);
                cell.set(i, next);
                cell.commit();
                int mode = next.getValue(io.github.soundgoodizerfan.feedback.machine.differential.DifferentialBlock.MODE);
                io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, next.getBlock().getName(), io.github.soundgoodizerfan.feedback.net.AimPayloads.keys("message.feedback.differential.short.", "0", "1", "2"),
                        mode, Component.translatable("message.feedback.differential.mode." + mode));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (i < 0 || cell.get(i) == null || !(cell.get(i).getBlock() instanceof JointPart))
            return InteractionResult.PASS;
        // Sneaking on an assembled joint reads its condition and brake pad instead of putting it back.
        if (player.isShiftKeyDown() && cell.bearing(i) != null) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.joint.condition",
                        Math.round(cell.bearing(i).condition() * 100), Math.round(cell.bearing(i).pad() * 100)), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Sneaking on an unassembled joint cycles its named choice (cone, spring) instead.
        boolean cyclable = cell.get(i).getBlock() instanceof JointBlock
                || cell.get(i).getBlock() instanceof RotaryBearingBlock && !(cell.get(i).getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.bearing.RingGearBlock);
        if (player.isShiftKeyDown() && cyclable && cell.bearing(i) == null) {
            if (!level.isClientSide) {
                BlockState next = cell.get(i).getBlock() instanceof JointBlock ? JointBlock.cycled(cell.get(i))
                        : cell.get(i).setValue(RotaryBearingBlock.ROLE, (cell.get(i).getValue(RotaryBearingBlock.ROLE) + 1) % 5);
                cell.set(i, next);
                cell.commit();
                if (next.getBlock() instanceof JointBlock)
                    io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, next.getBlock().getName(), java.util.List.of(Component.literal("1"), Component.literal("2"), Component.literal("3")),
                            next.getValue(JointBlock.VARIANT), Component.empty());
                else
                    io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, next.getBlock().getName(), io.github.soundgoodizerfan.feedback.net.AimPayloads.keys("message.feedback.joint.role.short.", "0", "1", "2", "3", "4"),
                            next.getValue(RotaryBearingBlock.ROLE), Component.translatable("message.feedback.joint.role." + next.getValue(RotaryBearingBlock.ROLE)));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel server)
            player.displayClientMessage(Component.translatable("message.feedback.rotary_bearing." + MicroBearings.toggle(server, pos, i)), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
