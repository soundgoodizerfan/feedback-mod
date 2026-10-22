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
package io.github.soundgoodizerfan.feedback.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places every deposit in {@link OreVeinTable} for one chunk (mechanics §9.3). It is the only
 * feature the table needs and it is placed with no placement modifiers, so {@code origin} is the
 * chunk's corner: for each deposit that occurs in this dimension it makes {@code count} attempts,
 * each at a random column of the chunk and a Y drawn from the deposit's own window, and each
 * attempt fills an ellipsoid of {@code cluster_size} candidate blocks, each becoming ore with the
 * deposit's density where the block there is the deposit's host. Stone hosts give {@code ore},
 * deepslate hosts give {@code deepslate_ore}. Only the feature's random source is used, so a seed
 * always builds the same world.
 */
public class OreVeinFeature extends Feature<NoneFeatureConfiguration> {

    public OreVeinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        var dimension = level.getLevel().dimension().location();

        boolean placed = false;
        for (OreVein vein : OreVeinTable.get().entries()) {
            if (!vein.dimensions().contains(dimension))
                continue;
            var ore = BuiltInRegistries.BLOCK.getOptional(vein.ore());
            var deepslateOre = BuiltInRegistries.BLOCK.getOptional(vein.deepslateOre());
            if (ore.isEmpty() || deepslateOre.isEmpty())
                continue;
            int attempts = (int) vein.count();
            if (random.nextFloat() < vein.count() - attempts)
                attempts++;
            int span = vein.maxY() - vein.minY() + 1;
            for (int i = 0; i < attempts; i++) {
                int y = vein.minY() + (vein.triangle()
                        ? (random.nextInt(span) + random.nextInt(span)) / 2
                        : random.nextInt(span));
                BlockPos centre = origin.offset(random.nextInt(16), 0, random.nextInt(16)).atY(y);
                if (vein.biomes().isPresent() && !level.getBiome(centre).is(vein.biomes().get()))
                    continue;
                placed |= placeBlob(level, random, vein, centre, ore.get().defaultBlockState(),
                        deepslateOre.get().defaultBlockState());
            }
        }
        return placed;
    }

    private static boolean placeBlob(WorldGenLevel level, RandomSource random, OreVein vein, BlockPos origin,
                                     BlockState ore, BlockState deepslateOre) {
        // A sphere holding clusterSize blocks, stretched a little differently on each axis so no
        // two blobs are the same shape.
        double radius = Math.cbrt(vein.clusterSize() * 3.0 / (4.0 * Math.PI));
        double rx = radius * (0.8 + 0.5 * random.nextDouble());
        double ry = radius * (0.8 + 0.5 * random.nextDouble());
        double rz = radius * (0.8 + 0.5 * random.nextDouble());
        int reachX = (int) Math.ceil(rx);
        int reachY = (int) Math.ceil(ry);
        int reachZ = (int) Math.ceil(rz);

        boolean placed = false;
        int candidates = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reachX; dx <= reachX && candidates < vein.clusterSize(); dx++) {
            for (int dy = -reachY; dy <= reachY && candidates < vein.clusterSize(); dy++) {
                for (int dz = -reachZ; dz <= reachZ && candidates < vein.clusterSize(); dz++) {
                    double dist = dx * dx / (rx * rx) + dy * dy / (ry * ry) + dz * dz / (rz * rz);
                    if (dist > 1.0)
                        continue;
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.ensureCanWrite(pos))
                        continue;
                    candidates++;
                    if (random.nextFloat() >= vein.density())
                        continue;
                    BlockState existing = level.getBlockState(pos);
                    boolean stone = existing.is(vein.host());
                    if (!stone && !existing.is(vein.deepslateHost()))
                        continue;
                    if (vein.airDiscard() > 0f && random.nextFloat() < vein.airDiscard() && touchesAir(level, pos))
                        continue;
                    placed |= level.setBlock(pos, stone ? ore : deepslateOre, 2);
                }
            }
        }
        return placed;
    }

    private static boolean touchesAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values())
            if (level.getBlockState(pos.relative(direction)).isAir())
                return true;
        return false;
    }
}
