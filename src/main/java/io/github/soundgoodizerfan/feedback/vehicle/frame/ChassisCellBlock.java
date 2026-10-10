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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * One block's worth of a welded plate's collision, at 1/16 resolution
 * ({@code feedback_vehicles_spec.md} section 4, <em>Bridging to Sable</em>).
 *
 * <h2>Why a plate becomes blocks at all</h2>
 * Sable bakes collision from each {@link BlockState}'s {@code VoxelShape}, not from a block
 * entity, so arbitrary triangles cannot be handed to it. What can be handed over is a finite set
 * of states, and a flat plate crossing a cube is a plane -- which takes very few numbers to name:
 * a direction ({@link #ORIENT}, one of {@link #DIRECTIONS}) and how far along it the plane sits
 * ({@link #OFFSET}). Each pair gets a staircase of 1/16 voxels, greedy-merged into as few boxes as
 * possible, because Sable turns every box into a Rapier collider. A cell two plates cross at
 * different angles has no single plane and is solid ({@link #FULL}).
 *
 * <p>The block is invisible; the frame renders the true, flat plate. {@link #LOAD} carries the
 * cell's share of plate mass to Sable through its {@code physics_block_properties} table, since
 * that is per state too.
 *
 * <h2>A cell is its plate's material</h2>
 * It mines like that material -- break time, tool needed, sound, blast resistance all come from
 * the block the plate was laid in (the hardest, where two plates meet). Breaking it takes every
 * plate through this block out of the frame, drops their material if the tool could have
 * harvested it, and re-cuts the cells. The frame stays the one source of truth: a cell is never
 * removed on its own, because the plate would still render with a hole nobody could see.
 */
public class ChassisCellBlock extends Block implements BlockSubLevelCollisionShape {

    /** Plane normals: every primitive integer vector with components in -2..2, one per +/- pair. */
    public static final List<Vec3> DIRECTIONS = directions();
    /** {@code DIRECTIONS.size()} means a full cell. */
    public static final int FULL = DIRECTIONS.size();
    public static final int OFFSETS = 16;
    public static final int LOADS = 8;

    public static final IntegerProperty ORIENT = IntegerProperty.create("orient", 0, FULL);
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 0, OFFSETS - 1);
    public static final IntegerProperty LOAD = IntegerProperty.create("load", 0, LOADS - 1);

    private static final VoxelShape[] SHAPES = new VoxelShape[FULL * OFFSETS];

    public ChassisCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ORIENT, FULL).setValue(OFFSET, 0).setValue(LOAD, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT, OFFSET, LOAD);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(ORIENT), state.getValue(OFFSET));
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        return shape(state.getValue(ORIENT), state.getValue(OFFSET));
    }

    // --- behaving as the plate's material ---------------------------------------------------

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return material(level, pos).getDestroyProgress(player, level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return material(level, pos).getSoundType(level, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return material(level, pos).getExplosionResistance(level, pos, explosion);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server)
            breakPlates(server, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel server)
            breakPlates(server, pos, null);
        super.onBlockExploded(state, level, pos, explosion);
    }

    /**
     * Takes every plate through {@code pos} out of its frame, drops the material a tool could have
     * harvested (none for an explosion or in creative), and re-cuts the cells next tick -- after
     * this block is gone, so a neighbouring plate's cell is not put back and then deleted.
     */
    private static void breakPlates(ServerLevel level, BlockPos pos, @Nullable Player player) {
        FrameBlockEntity frame = owner(level, pos);
        if (frame == null)
            return;
        BlockPos rel = pos.subtract(frame.getBlockPos());
        for (Frame.Plate p : FrameAssembly.platesThrough(frame.frame(), rel)) {
            frame.frame().removePlate(p.a(), p.b(), p.c());
            BlockState material = p.materialState();
            if (player != null && !player.getAbilities().instabuild
                    && (!material.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(material)))
                Block.popResource(level, pos, new ItemStack(material.getBlock()));
        }
        frame.cells().remove(rel);
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), frame::changed));
    }

    /** The material this cell mines as: the hardest plate through it, iron if it has lost its frame. */
    private static BlockState material(BlockGetter level, BlockPos pos) {
        FrameBlockEntity frame = owner(level, pos);
        BlockState best = Blocks.IRON_BLOCK.defaultBlockState();
        if (frame == null)
            return best;
        float hardest = -1;
        for (Frame.Plate p : FrameAssembly.platesThrough(frame.frame(), pos.subtract(frame.getBlockPos()))) {
            BlockState m = p.materialState();
            float h = m.getDestroySpeed(level, pos);
            if (h > hardest) {
                hardest = h;
                best = m;
            }
        }
        return best;
    }

    /**
     * The frame that cut this cell. Frames sit within eight blocks of their cells, so the chunk and
     * its neighbours hold it; walking their block entities is far cheaper than scanning a cube.
     */
    @Nullable
    static FrameBlockEntity owner(BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level))
            return null;
        ChunkPos c = new ChunkPos(pos);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values())
                    if (be instanceof FrameBlockEntity f && f.cells().contains(pos.subtract(f.getBlockPos())))
                        return f;
            }
        return null;
    }

    // --- quantising a plane -----------------------------------------------------------------

    /** The direction closest to {@code normal}, sign ignored. */
    public static int orient(Vec3 normal) {
        Vec3 n = normal.normalize();
        int best = 0;
        double bestDot = -1;
        for (int i = 0; i < DIRECTIONS.size(); i++) {
            double d = Math.abs(n.dot(DIRECTIONS.get(i)));
            if (d > bestDot) {
                bestDot = d;
                best = i;
            }
        }
        return best;
    }

    /** Offset step for a plane through {@code pointInCell} (cell-local, 0..1) along direction {@code o}. */
    public static int offset(int o, Vec3 pointInCell) {
        Vec3 n = DIRECTIONS.get(o);
        double lo = range(n, false), hi = range(n, true);
        double t = (pointInCell.dot(n) - lo) / (hi - lo);
        return Math.max(0, Math.min(OFFSETS - 1, (int) Math.round(t * (OFFSETS - 1))));
    }

    /** The plane constant for offset step {@code k} along {@code n}. */
    private static double planeAt(Vec3 n, int k) {
        double lo = range(n, false), hi = range(n, true);
        return lo + (hi - lo) * k / (OFFSETS - 1);
    }

    /** Min or max of {@code n . x} over the unit cube's corners. */
    private static double range(Vec3 n, boolean max) {
        double s = 0;
        for (double c : new double[]{n.x, n.y, n.z})
            s += max ? Math.max(c, 0) : Math.min(c, 0);
        return s;
    }

    // --- shapes -----------------------------------------------------------------------------

    public static VoxelShape shape(int o, int k) {
        if (o >= FULL)
            return Shapes.block();
        int i = o * OFFSETS + k;
        VoxelShape s = SHAPES[i];
        if (s == null)
            SHAPES[i] = s = build(DIRECTIONS.get(o), k);
        return s;
    }

    /**
     * Every 1/16 voxel the plane passes through (supercover: the voxel's centre is within half a
     * voxel's projected width of the plane), then greedy-merged: runs along x, widened along z,
     * stacked along y.
     */
    private static VoxelShape build(Vec3 n, int k) {
        int g = Frame.GRID;
        double d = planeAt(n, k);
        double half = 0.5 / g * (Math.abs(n.x) + Math.abs(n.y) + Math.abs(n.z));
        boolean[][][] on = new boolean[g][g][g];
        for (int x = 0; x < g; x++)
            for (int y = 0; y < g; y++)
                for (int z = 0; z < g; z++)
                    on[x][y][z] = Math.abs(n.x * (x + 0.5) / g + n.y * (y + 0.5) / g + n.z * (z + 0.5) / g - d) <= half;

        VoxelShape out = Shapes.empty();
        List<VoxelShape> boxes = new ArrayList<>();
        for (int y = 0; y < g; y++)
            for (int z = 0; z < g; z++)
                for (int x = 0; x < g; x++) {
                    if (!on[x][y][z])
                        continue;
                    int x1 = x;
                    while (x1 + 1 < g && on[x1 + 1][y][z])
                        x1++;
                    int z1 = z;
                    while (z1 + 1 < g && rowOn(on, x, x1, y, z1 + 1))
                        z1++;
                    int y1 = y;
                    while (y1 + 1 < g && slabOn(on, x, x1, y1 + 1, z, z1))
                        y1++;
                    for (int yy = y; yy <= y1; yy++)
                        for (int zz = z; zz <= z1; zz++)
                            for (int xx = x; xx <= x1; xx++)
                                on[xx][yy][zz] = false;
                    boxes.add(Block.box(x, y, z, x1 + 1, y1 + 1, z1 + 1));
                }
        if (boxes.isEmpty())
            return out;
        return Shapes.or(boxes.get(0), boxes.subList(1, boxes.size()).toArray(VoxelShape[]::new));
    }

    private static boolean rowOn(boolean[][][] on, int x0, int x1, int y, int z) {
        for (int x = x0; x <= x1; x++)
            if (!on[x][y][z])
                return false;
        return true;
    }

    private static boolean slabOn(boolean[][][] on, int x0, int x1, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++)
            if (!rowOn(on, x0, x1, y, z))
                return false;
        return true;
    }

    private static List<Vec3> directions() {
        List<Vec3> out = new ArrayList<>();
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++) {
                    if (gcd(gcd(Math.abs(x), Math.abs(y)), Math.abs(z)) != 1)
                        continue; // zero vector, or a multiple of a shorter one
                    // One of each +/- pair: first non-zero component positive.
                    int first = x != 0 ? x : y != 0 ? y : z;
                    if (first < 0)
                        continue;
                    out.add(new Vec3(x, y, z).normalize());
                }
        return List.copyOf(out);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
