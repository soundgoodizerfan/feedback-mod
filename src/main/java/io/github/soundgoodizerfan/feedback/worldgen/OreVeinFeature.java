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

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places the deposits in {@link OreVeinTable} (mechanics §9.3), GregTech style: ore exists only
 * inside a vein. The world is cut into regions of {@value #REGION_CHUNKS} by {@value #REGION_CHUNKS}
 * chunks; each region holds {@value #SLOTS} vein slots whose type, centre and height are drawn from
 * the world seed and the slot alone, so every chunk that a vein touches computes the same vein and
 * fills its own part of it. A vein is a flat ellipsoid; each block in it becomes ore with the
 * deposit's density, thinning toward the rim, where the block is the deposit's host (stone hosts
 * give {@code ore}, deepslate hosts {@code deepslate_ore}). Nothing is placed outside a vein.
 * <p>
 * {@code origin} is the chunk's corner (the placed feature has no modifiers). A vein reaches at most
 * {@value OreVein#MAX_RADIUS} blocks from its centre, less than a region, so the 3 by 3 regions
 * around the chunk's own cover every vein that can touch it.
 */
public class OreVeinFeature extends Feature<NoneFeatureConfiguration> {

    static final int REGION_CHUNKS = 3, REGION = REGION_CHUNKS * 16;
    /** Veins per region; Invented, with each file's {@code weight} the whole of the abundance tuning. */
    static final int SLOTS = 12;

    public OreVeinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        var dimension = level.getLevel().dimension().location();
        List<OreVein> veins = OreVeinTable.get().entries().stream().filter(v -> v.dimensions().contains(dimension)).toList();
        int totalWeight = veins.stream().mapToInt(OreVein::weight).sum();
        if (totalWeight <= 0)
            return false;
        long seed = level.getSeed();
        int regionX = Math.floorDiv(origin.getX(), REGION), regionZ = Math.floorDiv(origin.getZ(), REGION);

        boolean placed = false;
        for (int rx = regionX - 1; rx <= regionX + 1; rx++)
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++)
                for (int slot = 0; slot < SLOTS; slot++) {
                    long s = mix(seed ^ mix(rx * 341873128712L + rz * 132897987541L + slot * 0x9E3779B97F4A7C15L));
                    RandomSource random = new XoroshiroRandomSource(s, mix(s));
                    OreVein vein = pick(veins, random.nextInt(totalWeight));
                    int span = vein.maxY() - vein.minY() + 1;
                    int cx = rx * REGION + random.nextInt(REGION), cz = rz * REGION + random.nextInt(REGION);
                    int cy = vein.minY() + (vein.triangle()
                            ? (random.nextInt(span) + random.nextInt(span)) / 2
                            : random.nextInt(span));
                    double radiusX = vein.radius() * (0.8 + 0.4 * random.nextDouble());
                    double radiusZ = vein.radius() * (0.8 + 0.4 * random.nextDouble());
                    if (cx + radiusX < origin.getX() || cx - radiusX > origin.getX() + 15
                            || cz + radiusZ < origin.getZ() || cz - radiusZ > origin.getZ() + 15)
                        continue;
                    BlockPos centre = new BlockPos(cx, cy, cz);
                    // The centre may lie in a chunk that is not loaded for this one; the biome is read at the
                    // nearest column of this chunk, so a vein is cut at a biome edge chunk by chunk.
                    if (vein.biomes().isPresent() && !level.getBiome(new BlockPos(
                            Math.max(origin.getX(), Math.min(origin.getX() + 15, cx)), cy,
                            Math.max(origin.getZ(), Math.min(origin.getZ() + 15, cz)))).is(vein.biomes().get()))
                        continue;
                    placed |= fill(level, s, vein, centre, radiusX, radiusZ, origin);
                    if (!vein.indicators().isEmpty())
                        outcrop(level, random, vein, centre, origin);
                }
        return placed;
    }

    private record Stratum(BlockState ore, BlockState deepslateOre, float density) {}

    private static Stratum stratum(net.minecraft.resources.ResourceLocation ore, net.minecraft.resources.ResourceLocation deepslateOre,
                                   float density) {
        if (!BuiltInRegistries.BLOCK.containsKey(ore) || !BuiltInRegistries.BLOCK.containsKey(deepslateOre))
            return null;
        return new Stratum(BuiltInRegistries.BLOCK.get(ore).defaultBlockState(),
                BuiltInRegistries.BLOCK.get(deepslateOre).defaultBlockState(), density);
    }

    private static OreVein pick(List<OreVein> veins, int roll) {
        for (OreVein vein : veins) {
            roll -= vein.weight();
            if (roll < 0)
                return vein;
        }
        return veins.get(veins.size() - 1);
    }

    /** This chunk's share of one vein; every block's fate is a hash of the vein and the position, never the feature's random. */
    private static boolean fill(WorldGenLevel level, long veinSeed, OreVein vein, BlockPos centre, double radiusX,
                                double radiusZ, BlockPos origin) {
        Stratum core = stratum(vein.ore(), vein.deepslateOre(), vein.density());
        if (core == null)
            return false;
        Stratum cap = vein.layers().cap().map(z -> stratum(z.ore(), z.deepslateOre(), z.density())).orElse(core);
        Stratum rim = vein.layers().rim().map(z -> stratum(z.ore(), z.deepslateOre(), z.density())).orElse(core);
        List<Stratum> scatter = vein.layers().scatter().stream().map(z -> stratum(z.ore(), z.deepslateOre(), z.density()))
                .filter(java.util.Objects::nonNull).toList();
        if (cap == null) cap = core;
        if (rim == null) rim = core;
        int x0 = Math.max(origin.getX(), (int) Math.floor(centre.getX() - radiusX)),
                x1 = Math.min(origin.getX() + 15, (int) Math.ceil(centre.getX() + radiusX)),
                z0 = Math.max(origin.getZ(), (int) Math.floor(centre.getZ() - radiusZ)),
                z1 = Math.min(origin.getZ() + 15, (int) Math.ceil(centre.getZ() + radiusZ));
        double thickness = vein.thickness();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = centre.getY() - vein.thickness(); y <= centre.getY() + vein.thickness(); y++) {
                    double dx = (x - centre.getX()) / radiusX, dz = (z - centre.getZ()) / radiusZ,
                            dy = (y - centre.getY()) / thickness;
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist > 1.0)
                        continue;
                    pos.set(x, y, z);
                    if (!level.ensureCanWrite(pos))
                        continue;
                    BlockState existing = level.getBlockState(pos);
                    boolean stone = existing.is(vein.host());
                    if (!stone && !existing.is(vein.deepslateHost()))
                        continue;
                    long h = mix(veinSeed ^ pos.asLong());
                    Stratum layer = dy > 0.35 ? cap : dist > 0.55 ? rim : core;
                    double fade = 1.0 - 0.5 * dist;
                    BlockState state = null;
                    if ((h >>> 40) / (double) (1 << 24) < layer.density * fade)
                        state = stone ? layer.ore : layer.deepslateOre;
                    for (int i = 0; state == null && i < scatter.size(); i++)
                        if ((mix(h + i + 1) >>> 40) / (double) (1 << 24) < scatter.get(i).density * fade)
                            state = stone ? scatter.get(i).ore : scatter.get(i).deepslateOre;
                    if (state == null)
                        continue;
                    if (vein.airDiscard() > 0f && (h & 0xFFFFFF) / (double) (1 << 24) < vein.airDiscard()
                            && touchesAir(level, pos))
                        continue;
                    placed |= level.setBlock(pos, state, 2);
                }
        return placed;
    }

    /** Blocks that stain the ground itself where a vein lies near: they replace the dirt rather than stand on it. */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> STAINS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "ore_stains"));

    /**
     * Up to three signs over a vein that comes near enough to the surface, scattered over its footprint; each
     * spot draws one of the deposit's indicators. A loose rock is a single heap; a small flower is planted in
     * a patch round the spot where it can stand; a stain (a block in {@code feedback:ore_stains}) replaces the
     * dirt in a wider patch. Every draw from the vein's own random is made whether or not it is used, and the
     * patch cells come from a hash of the spot, so every chunk agrees on the spots and places those in its own
     * columns.
     */
    private static void outcrop(WorldGenLevel level, RandomSource random, OreVein vein, BlockPos centre, BlockPos origin) {
        for (int i = 0; i < 3; i++) {
            int x = centre.getX() + random.nextInt(2 * vein.radius() + 1) - vein.radius(),
                    z = centre.getZ() + random.nextInt(2 * vein.radius() + 1) - vein.radius();
            var id = vein.indicators().get(random.nextInt(vein.indicators().size()));
            long patch = random.nextLong();
            var indicator = BuiltInRegistries.BLOCK.getOptional(id);
            if (indicator.isEmpty())
                continue;
            BlockState state = indicator.get().defaultBlockState();
            boolean stain = state.is(STAINS), flower = state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS);
            int reach = stain || flower ? 2 : 0;
            int filled = stain ? 180 : 90; // of 256 cells, a stain being the denser
            for (int dx = -reach; dx <= reach; dx++)
                for (int dz = -reach; dz <= reach; dz++)
                    if (reach == 0 || dx * dx + dz * dz <= 5 && (mix(patch ^ (dx * 31L + dz * 131L + 4096)) & 255) < filled)
                        lay(level, vein, centre, origin, state, stain, flower, x + dx, z + dz);
        }
    }

    private static void lay(WorldGenLevel level, OreVein vein, BlockPos centre, BlockPos origin, BlockState state,
                            boolean stain, boolean flower, int x, int z) {
        if (x < origin.getX() || x > origin.getX() + 15 || z < origin.getZ() || z > origin.getZ() + 15)
            return;
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        if (level.dimensionType().hasCeiling()) {
            // The Nether's heightmap is its bedrock roof: take the first cavern floor above the vein instead.
            top = Integer.MAX_VALUE;
            for (int y = centre.getY(); y <= centre.getY() + vein.indicatorDepth(); y++)
                if (level.getBlockState(new BlockPos(x, y, z)).is(vein.host()) && level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
                    top = y;
                    break;
                }
        }
        if (top - centre.getY() > vein.indicatorDepth())
            return;
        BlockPos ground = new BlockPos(x, top, z), at = ground.above();
        BlockState below = level.getBlockState(ground), here = level.getBlockState(at);
        if (stain) {
            if (level.ensureCanWrite(ground) && below.is(net.minecraft.tags.BlockTags.DIRT))
                level.setBlock(ground, state, 2);
            return;
        }
        // The rock is a heap, not a block: it sits on the surface, in the air cell above the dirt or stone.
        boolean footing = flower ? state.canSurvive(level, at)
                : below.is(net.minecraft.tags.BlockTags.DIRT) || below.is(vein.host())
                        || below.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || below.is(net.minecraft.tags.BlockTags.SAND);
        if (level.ensureCanWrite(at) && (here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty()) && footing)
            level.setBlock(at, state, 2);
    }

    private static boolean touchesAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values())
            if (level.getBlockState(pos.relative(direction)).isAir())
                return true;
        return false;
    }
}
