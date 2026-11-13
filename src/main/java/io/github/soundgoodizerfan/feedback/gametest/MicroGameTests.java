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
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlock;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroPlacer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Micro cells ({@code docs/dev/vehicle.md}, phase 1): the voxels are the shape, the state
 * Sable reads names the same voxels at half-block resolution, a save keeps them, the cell weighs
 * what they weigh, and a held block places and removes them.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class MicroGameTests {

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState IRON = Blocks.IRON_BLOCK.defaultBlockState();

    /**
     * The cell these tests build in. The empty template is a single block ringed with barrier, which
     * a cell is rightly not made over, so the cell and the two neighbours the tests use are cleared first.
     */
    private static BlockPos clearCell(net.minecraft.gametest.framework.GameTestHelper helper) {
        BlockPos cell = helper.absolutePos(new BlockPos(1, 2, 1));
        for (BlockPos p : new BlockPos[] {cell, cell.east(), cell.above()})
            helper.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        return cell;
    }

    /** Three voxels, two kinds: the exact shape is those three, Sable's is whole octants round them, and a save reads back the same. */
    @GameTest(template = "empty")
    public static void voxelsMakeTheShapeAndSurviveASave(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos cell = clearCell(helper);
        int vx = cell.getX() * 4, vy = cell.getY() * 4, vz = cell.getZ() * 4;
        helper.assertTrue(MicroPlacer.put(level, vx, vy, vz, STONE), "the first voxel makes the cell");
        helper.assertTrue(MicroPlacer.put(level, vx + 1, vy, vz, STONE), "a second beside it");
        helper.assertTrue(MicroPlacer.put(level, vx + 3, vy + 3, vz + 3, IRON), "and one in the far corner");
        helper.assertFalse(MicroPlacer.put(level, vx + 1, vy, vz, IRON), "a taken voxel is not put over");

        BlockState state = level.getBlockState(cell);
        helper.assertTrue(state.is(FVehicles.MICRO_CELL.get()), "a micro cell stands there");
        var be = (MicroCellBlockEntity) level.getBlockEntity(cell);

        VoxelShape exact = state.getCollisionShape(level, cell);
        helper.assertTrue(Math.abs(volume(exact) - 3.0 / 64) < 1e-9, "the shape is three voxels, got " + volume(exact));
        helper.assertTrue(hits(exact, 0.25, 0, 0, 0.5, 0.25, 0.25), "the second voxel is solid");
        helper.assertFalse(hits(exact, 0.5, 0, 0, 0.75, 0.25, 0.25), "the one past it is not");

        helper.assertTrue(state.getValue(MicroCellBlock.OCTANTS) == (1 | 128),
                "the state names the first and last octants, got " + state.getValue(MicroCellBlock.OCTANTS));
        VoxelShape physics = ((BlockSubLevelCollisionShape) state.getBlock()).getSubLevelCollisionShape(level, state);
        helper.assertFalse(Shapes.joinIsNotEmpty(exact, physics, BooleanOp.ONLY_FIRST), "Sable's collider covers every voxel");
        helper.assertFalse(hits(physics, 0.5, 0, 0, 1, 0.5, 0.5), "and not an octant with none");

        var registries = level.registryAccess();
        CompoundTag tag = be.saveWithoutMetadata(registries);
        helper.assertTrue(tag.getList("Palette", Tag.TAG_COMPOUND).size() == 2, "two kinds of block, two palette entries");
        var copy = new MicroCellBlockEntity(cell, state);
        copy.loadWithComponents(tag, registries);
        helper.assertTrue(copy.filled() == be.filled(), "the same voxels are filled after a save");
        for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
            helper.assertTrue(copy.get(i) == be.get(i), "voxel " + i + " reads back as it was");

        helper.assertTrue(MicroPlacer.take(level, vx, vy, vz) == STONE, "a voxel comes out as the block that was in it");
        MicroPlacer.take(level, vx + 1, vy, vz);
        helper.assertTrue(level.getBlockState(cell).is(FVehicles.MICRO_CELL.get()), "the cell stays while a voxel does");
        MicroPlacer.take(level, vx + 3, vy + 3, vz + 3);
        helper.assertTrue(level.getBlockState(cell).isAir(), "the cell goes with its last voxel");
        helper.succeed();
    }

    /** A voxel on a cell's edge makes the next cell; a cell is not made over another block. */
    @GameTest(template = "empty")
    public static void aVoxelPastTheEdgeIsInTheNextCell(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos cell = clearCell(helper);
        int vx = cell.getX() * 4, vy = cell.getY() * 4, vz = cell.getZ() * 4;
        helper.assertTrue(MicroPlacer.put(level, vx + 3, vy, vz, STONE), "a voxel on the east edge");
        helper.assertTrue(level.getBlockState(cell.east()).isAir(), "nothing in the next cell yet");
        helper.assertTrue(MicroPlacer.put(level, vx + 4, vy, vz, STONE), "one past it");
        helper.assertTrue(level.getBlockState(cell.east()).is(FVehicles.MICRO_CELL.get()), "makes the next cell");
        MicroPlacer.take(level, vx + 3, vy, vz);
        helper.assertTrue(level.getBlockState(cell).isAir() && level.getBlockState(cell.east()).is(FVehicles.MICRO_CELL.get()),
                "the first cell goes, the second stays");

        level.setBlock(cell.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertFalse(MicroPlacer.put(level, vx, vy + 4, vz, STONE), "no cell is made over a block");
        level.removeBlock(cell.east(), false);
        level.removeBlock(cell.above(), false);
        helper.succeed();
    }

    /**
     * A cell weighs its voxels by their blocks' material density (a 64th of a block each), rounded
     * to the nearest mass class, which Sable reads off the state; a block no material claims
     * weighs the fallback.
     */
    @GameTest(template = "empty")
    public static void aCellWeighsItsVoxelsByMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos cell = clearCell(helper);
        int vx = cell.getX() * 4, vy = cell.getY() * 4, vz = cell.getZ() * 4;
        for (int i = 0; i < 8; i++)
            MicroPlacer.put(level, vx + (i & 1), vy + (i >> 1 & 1), vz + (i >> 2 & 1), STONE);
        double stone = 8 * density(Items.STONE) / 64;
        var be = (MicroCellBlockEntity) level.getBlockEntity(cell);
        helper.assertTrue(Math.abs(be.kpg() - stone) < 1e-4, "eight stone voxels are an eighth of a block of stone, " + stone + " kpg, got " + be.kpg());
        double ratio = MicroCellBlock.massOf(level.getBlockState(cell).getValue(MicroCellBlock.LOAD)) / stone;
        helper.assertTrue(ratio > 0.88 && ratio < 1.13, "the mass class is within half a step of the weight, got a ratio of " + ratio);

        helper.assertTrue(Math.abs(MicroCellBlockEntity.voxelKpg(IRON) - density(Items.IRON_BLOCK) / 64) < 1e-6,
                "a voxel of iron is a 64th of a block of iron, got " + MicroCellBlockEntity.voxelKpg(IRON));
        helper.assertTrue(Math.abs(MicroCellBlockEntity.voxelKpg(Blocks.SPONGE.defaultBlockState())
                - VehicleTuning.MICRO_FALLBACK_DENSITY.muPerMb() / 64) < 1e-6, "a block no material claims weighs the fallback");
        level.removeBlock(cell, false);
        helper.succeed();
    }

    /** A held block places in front of the face hit and costs one; sneaking removes nothing (mining does that); creative pays nothing. */
    @GameTest(template = "empty")
    public static void aHeldBlockPlacesAndSneakRemoves(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos cell = clearCell(helper);
        helper.assertTrue(MicroPlacer.put(level, cell.getX() * 4, cell.getY() * 4, cell.getZ() * 4, STONE), "a first voxel");
        var be = (MicroCellBlockEntity) level.getBlockEntity(cell);
        // The middle of the first voxel's east face.
        BlockHitResult east = new BlockHitResult(new Vec3(cell.getX() + 0.25, cell.getY() + 0.125, cell.getZ() + 0.125),
                Direction.EAST, cell, false);

        var survival = helper.makeMockPlayer(GameType.SURVIVAL);
        survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_BLOCK, 3));
        helper.assertTrue(MicroPlacer.use(survival, survival.getMainHandItem(), cell, east), "a held block places");
        helper.assertTrue(be.get(1) == IRON && survival.getMainHandItem().getCount() == 2, "the voxel east of the one hit is iron, and it cost one block");

        survival.setShiftKeyDown(true);
        // Taking a voxel out is mining it with a tool, never a sneak-click: sneaking changes nothing here.
        helper.assertFalse(MicroPlacer.use(survival, survival.getMainHandItem(), cell, east), "sneaking does not remove, and the voxel beyond is taken");
        helper.assertTrue(be.get(0) == STONE && be.get(1) == IRON && survival.getMainHandItem().getCount() == 2,
                "both voxels stay and nothing more is paid");

        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_BLOCK, 3));
        BlockHitResult top = new BlockHitResult(new Vec3(cell.getX() + 0.375, cell.getY() + 0.25, cell.getZ() + 0.125),
                Direction.UP, cell, false);
        helper.assertTrue(MicroPlacer.use(creative, creative.getMainHandItem(), cell, top), "a creative player places too");
        helper.assertTrue(be.get(MicroCellBlockEntity.index(1, 1, 0)) == IRON && creative.getMainHandItem().getCount() == 3,
                "on top of the voxel hit, and for nothing");
        level.removeBlock(cell, false);
        helper.succeed();
    }

    /** Only a plain block can be a voxel: not a tool, and not a block with an entity of its own. */
    @GameTest(template = "empty")
    public static void onlyPlainBlocksFit(GameTestHelper helper) {
        helper.assertTrue(MicroPlacer.fits(new ItemStack(Items.IRON_BLOCK)), "iron block fits");
        helper.assertFalse(MicroPlacer.fits(new ItemStack(FVehicles.LIFT_ITEM.get())), "a Lift has a block entity");
        helper.assertFalse(MicroPlacer.fits(new ItemStack(Items.CHEST)), "so does a chest");
        helper.succeed();
    }

    /** The material table's density for {@code item}, Mu/mB: the one source a voxel's weight comes from. */
    private static float density(Item item) {
        return Material.find(MaterialTable.get().materials(), item).orElseThrow().material().density().muPerMb();
    }

    private static double volume(VoxelShape shape) {
        double sum = 0;
        for (AABB box : shape.toAabbs())
            sum += (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
        return sum;
    }

    private static boolean hits(VoxelShape shape, double x0, double y0, double z0, double x1, double y1, double z1) {
        return Shapes.joinIsNotEmpty(shape, Shapes.create(new AABB(x0, y0, z0, x1, y1, z1)), BooleanOp.AND);
    }
}
