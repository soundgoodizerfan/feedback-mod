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
package io.github.soundgoodizerfan.feedback.geology;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Basin placement (petrochemistry spec 2.3), the convention of {@code OreVeinFeature}: the world is cut
 * into regions of {@value #REGION} blocks, each holding {@value #SLOTS} basin slot whose kind, centre
 * and crude grade come from the world seed and the slot alone, so every caller computes the same
 * basin. {@link #sitesNear} is that pure function; {@link BasinState} and the survey meter use it
 * too. The feature itself only lays the surface seeps: the basin is data, not blocks.
 *
 * <p>Tuning: one basin per 160 block region, radius 40, so a basin lies within about 100 blocks of
 * anywhere. {@code Basin.biomes} gates the seeps only (reading a biome for a basin that may sit in
 * an unloaded chunk would force a chunk load), so a basin under an ocean exists but shows nothing.
 */
public class BasinFeature extends Feature<NoneFeatureConfiguration> {

    static final int REGION = 160, SLOTS = 1;
    /** Widest a basin may be; the region is wider, which is what lets a lookup read only the 3 by 3 regions around it. */
    public static final int MAX_RADIUS = 64;
    /** Chance a chunk touching a basin gets a seep patch. */
    private static final float SEEP_CHANCE = 0.5f;

    /** One placed basin: a pure function of seed, so it exists before {@link BasinState} has heard of it. */
    public record Site(String id, Basin basin, BlockPos centre, CrudeGrade grade) {
        public int radius() {
            return basin.radius();
        }
    }

    public BasinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Every basin of the 3 by 3 regions around column {@code (x, z)}; callers filter by their own reach. */
    public static List<Site> sitesNear(long seed, ResourceLocation dimension, int x, int z) {
        List<Basin> basins = BasinTable.get().entries().stream().filter(b -> b.dimensions().contains(dimension)).toList();
        int totalWeight = basins.stream().mapToInt(Basin::weight).sum();
        List<Site> out = new ArrayList<>();
        if (totalWeight <= 0)
            return out;
        int regionX = Math.floorDiv(x, REGION), regionZ = Math.floorDiv(z, REGION);
        for (int rx = regionX - 1; rx <= regionX + 1; rx++)
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++)
                for (int slot = 0; slot < SLOTS; slot++) {
                    long s = mix(seed ^ mix(rx * 341873128712L + rz * 132897987541L + slot * 0x9E3779B97F4A7C15L + 0x0B451L));
                    RandomSource random = new XoroshiroRandomSource(s, mix(s));
                    int roll = random.nextInt(totalWeight);
                    Basin basin = basins.get(basins.size() - 1);
                    for (Basin candidate : basins) {
                        roll -= candidate.weight();
                        if (roll < 0) {
                            basin = candidate;
                            break;
                        }
                    }
                    int cx = rx * REGION + random.nextInt(REGION), cz = rz * REGION + random.nextInt(REGION);
                    int cy = basin.minY() + random.nextInt(Math.max(1, basin.maxY() - basin.minY() + 1));
                    CrudeGrade c = basin.crude();
                    float spread = basin.gradeSpread();
                    CrudeGrade grade = new CrudeGrade(
                            Mth.clamp(c.density() * wobble(random, spread), 0.6f, 1.1f),
                            Mth.clamp(c.sulfur() * wobble(random, spread), 0f, 1f),
                            Mth.clamp(c.lightFraction() * wobble(random, spread), 0f, 1f));
                    out.add(new Site(rx + "_" + rz + "_" + slot, basin, new BlockPos(cx, cy, cz), grade));
                }
        return out;
    }

    private static float wobble(RandomSource random, float spread) {
        return 1f + (random.nextFloat() * 2f - 1f) * spread;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        long seed = level.getSeed();
        boolean placed = false;
        for (Site site : sitesNear(seed, level.getLevel().dimension().location(), origin.getX() + 8, origin.getZ() + 8)) {
            int r = site.radius();
            BlockPos c = site.centre();
            if (c.getX() + r < origin.getX() || c.getX() - r > origin.getX() + 15
                    || c.getZ() + r < origin.getZ() || c.getZ() - r > origin.getZ() + 15)
                continue;
            long s = mix(seed ^ site.id().hashCode() ^ mix(origin.getX() * 31L + origin.getZ() * 17L));
            RandomSource random = new XoroshiroRandomSource(s, mix(s));
            if (random.nextFloat() >= SEEP_CHANCE)
                continue;
            var indicator = BuiltInRegistries.BLOCK.getOptional(site.basin().indicator().orElse(FBlocks.SEEP.getId()));
            if (indicator.isEmpty())
                continue;
            int x = origin.getX() + random.nextInt(16), z = origin.getZ() + random.nextInt(16);
            double dx = x - c.getX(), dz = z - c.getZ();
            if (dx * dx + dz * dz > r * r * 0.64)
                continue;
            // A patch is the seed block and up to two neighbours, a few blocks wide.
            BlockPos at = new BlockPos(x, 0, z);
            for (int i = 0; i < 3; i++) {
                if (i > 0) {
                    Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    at = new BlockPos(at.getX() + d.getStepX(), 0, at.getZ() + d.getStepZ());
                }
                if (at.getX() < origin.getX() || at.getX() > origin.getX() + 15
                        || at.getZ() < origin.getZ() || at.getZ() > origin.getZ() + 15)
                    continue;
                placed |= seep(level, indicator.get().defaultBlockState(), site, at.getX(), at.getZ());
            }
        }
        return placed;
    }

    private static boolean seep(WorldGenLevel level, BlockState state, Site site, int x, int z) {
        if (site.basin().biomes().isPresent()
                && !level.getBiome(new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z))
                        .is(site.basin().biomes().get()))
            return false;
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        BlockPos ground = new BlockPos(x, top, z), at = ground.above();
        BlockState below = level.getBlockState(ground), here = level.getBlockState(at);
        return level.ensureCanWrite(at) && here.isAir()
                && (below.is(BlockTags.DIRT) || below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.SAND))
                && level.setBlock(at, state, 2);
    }
}
