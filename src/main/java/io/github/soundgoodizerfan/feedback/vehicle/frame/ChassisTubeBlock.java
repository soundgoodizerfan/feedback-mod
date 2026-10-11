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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One block's worth of a tube's collision ({@code feedback_vehicles_spec.md} section 4a): every 1/16
 * voxel within the tube's radius of its centre line, merged into boxes -- the tube outlined in
 * sixteenths, the same way {@link ChassisCellBlock} outlines a plate.
 *
 * <h2>Why the line is quantised</h2>
 * Sable takes collision per {@link BlockState}, never per position, so every shape a tube can cut
 * has to be a state. A line through a cell is a direction and a point: {@link #DIR} is one of the
 * plate cell's 49 directions (sign does not matter to a line), and {@link #U}/{@link #V} place it
 * on the plane across that direction in 1/8 steps out to 3/4 block from the cell's centre, past
 * which a line only grazes a corner. With {@link #SIZE} that is 49 x 13 x 13 x 3 = 24,843 states.
 * 1/16 steps would be about 95,000, for at most 1/16 of error.
 *
 * <p>Mass is by {@link #SIZE} alone, from this block's {@code physics_block_properties} table --
 * there is no room left in the state for a load like a plate cell's, so a tube cell weighs what an
 * iron tube of its size would, whatever it is made of. [OPEN] in the spec. Where a plate shares the
 * cell the plate cell wins, carrying the tube's true mass in its load.
 */
public class ChassisTubeBlock extends ChassisPieceBlock {

    /** Offset steps across the line: -3/4 to +3/4 block in 1/8s, centre included. */
    public static final int STEPS = 13;
    /** Offset step, in blocks. */
    public static final double STEP = 0.125;

    public static final IntegerProperty DIR = IntegerProperty.create("dir", 0, ChassisCellBlock.DIRECTIONS.size() - 1);
    public static final IntegerProperty U = IntegerProperty.create("u", 0, STEPS - 1);
    public static final IntegerProperty V = IntegerProperty.create("v", 0, STEPS - 1);
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, Frame.TUBE_SIZES.length - 1);

    private static final VoxelShape[] SHAPES = new VoxelShape[ChassisCellBlock.DIRECTIONS.size() * STEPS * STEPS * Frame.TUBE_SIZES.length];

    public ChassisTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DIR, 0).setValue(U, 0).setValue(V, 0).setValue(SIZE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIR, U, V, SIZE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state);
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        return shape(state);
    }

    /** The state for a tube of size {@code size} along {@code direction} through {@code pointInCell} (cell-local, 0..1). */
    public static BlockState state(BlockState base, Vec3 direction, Vec3 pointInCell, int size) {
        int dir = ChassisCellBlock.orient(direction);
        Vec3[] uv = basis(ChassisCellBlock.DIRECTIONS.get(dir));
        Vec3 c = pointInCell.subtract(0.5, 0.5, 0.5);
        return base.setValue(DIR, dir).setValue(U, step(c.dot(uv[0]))).setValue(V, step(c.dot(uv[1]))).setValue(SIZE, size);
    }

    private static int step(double o) {
        return Math.max(0, Math.min(STEPS - 1, (int) Math.round(o / STEP) + STEPS / 2));
    }

    private static double at(int step) {
        return (step - STEPS / 2) * STEP;
    }

    /** Two unit vectors across {@code d}, fixed per direction so a state means one line. */
    private static Vec3[] basis(Vec3 d) {
        Vec3 u = (Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(d).normalize();
        return new Vec3[]{u, d.cross(u)};
    }

    public static VoxelShape shape(BlockState state) {
        int dir = state.getValue(DIR), u = state.getValue(U), v = state.getValue(V), size = state.getValue(SIZE);
        int i = ((dir * STEPS + u) * STEPS + v) * Frame.TUBE_SIZES.length + size;
        VoxelShape s = SHAPES[i];
        if (s == null)
            SHAPES[i] = s = build(dir, u, v, size);
        return s;
    }

    private static VoxelShape build(int dir, int u, int v, int size) {
        int g = Frame.GRID;
        Vec3 d = ChassisCellBlock.DIRECTIONS.get(dir);
        Vec3[] uv = basis(d);
        Vec3 o = uv[0].scale(at(u)).add(uv[1].scale(at(v)));
        double r = Frame.TUBE_SIZES[size] / 2.0 / g + 1e-9;
        boolean[][][] on = new boolean[g][g][g];
        for (int x = 0; x < g; x++)
            for (int y = 0; y < g; y++)
                for (int z = 0; z < g; z++) {
                    Vec3 q = new Vec3((x + 0.5) / g - 0.5, (y + 0.5) / g - 0.5, (z + 0.5) / g - 0.5).subtract(o);
                    on[x][y][z] = q.subtract(d.scale(q.dot(d))).length() <= r;
                }
        return ChassisCellBlock.merge(on);
    }
}
