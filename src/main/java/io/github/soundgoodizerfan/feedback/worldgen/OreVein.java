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
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

// Design from GregTech CEu Modern's GTOreDefinition, trimmed; see THIRD-PARTY-LICENSES.md.
/**
 * One deposit: where an ore occurs and what a vein of it looks like (mechanics §9.2). Loaded from
 * {@code data/<ns>/ore_vein/*.json} by {@link OreVeinTable}; placed by {@link OreVeinFeature}.
 *
 * <p>Every number in a deposit file is Invented (placeholder, philosophy §19), though the vanilla
 * deposits' files start from vanilla's own height window and a weight that keeps vanilla's relative abundance.
 *
 * <p>This table is the single source of truth for where and how often: the one placed feature
 * ({@code worldgen/placed_feature/ore_vein.json}) has no placement modifiers, and
 * {@link OreVeinFeature} fills every region with veins drawn by {@code weight}, each at its own
 * height. Several files may share an ore (a coal seam near the surface and another deep down).
 *
 * <p><b>Adding an ore</b> (mechanics §9.6):
 * <ol>
 * <li>Register {@code <name>_ore} and {@code deepslate_<name>_ore} blocks in {@code FBlocks}
 * (the {@code ore(...)} helper), their block items in {@code FItems}, and the creative tab entry.</li>
 * <li>Add blockstate, block model and item model, lang names, and a loot table for each block
 * under {@code data/feedback/loot_table/blocks/}.</li>
 * <li>Add both blocks to {@code FormData.BlockTagsGen} (pickaxe, and a {@code needs_*_tool} tag
 * matching the tier), then run {@code runData}; add the vanilla ore tags
 * ({@code minecraft:<name>_ores}, block and item) so other mods and recipes see them.</li>
 * <li>Add {@code data/feedback/ore_vein/<name>.json} (one per height band) and point the
 * material's {@code forms.ore} / {@code forms.deepslate_ore} at the new blocks.</li>
 * <li>To retire a vanilla ore, list its placed features in
 * {@code neoforge/biome_modifier/remove_vanilla_ores.json}.</li>
 * </ol>
 *
 * <p><b>Tag fields are plain ids</b> ({@code "minecraft:is_badlands"}, never {@code "#..."}): the
 * codec is {@code TagKey.codec}, which rejects the hash. <b>One-block ores</b> (the Nether's) name
 * the same block as {@code ore} and {@code deepslate_ore} and set {@code host} to a tag of their
 * stone; the deepslate host never matches there, so the second block is never placed.
 *
 * @param ore          block placed where the host is a stone.
 * @param deepslateOre block placed where the host is a deepslate.
 * @param host         what {@code ore} may replace.
 * @param deepslateHost what {@code deepslateOre} may replace.
 * @param radius       horizontal reach of the vein (blocks), varied a fifth either way per vein.
 * @param thickness    vertical reach of the vein (blocks): veins are flat.
 * @param weight       share of the dimension's vein slots this deposit takes; its rarity.
 * @param density      chance a block in the vein becomes ore at its heart, 0 to 1 (half that at the rim): a lean or rich vein.
 * @param minY         lowest Y a vein may be centred at.
 * @param maxY         highest Y a vein may be centred at.
 * @param triangle     whether Y is drawn triangular (most likely mid-window) rather than uniform.
 * @param airDiscard   chance a block touching air is left unmined stone, 0 to 1: a buried deposit
 *                     shows less in cave walls.
 * @param dimensions   dimensions it occurs in.
 * @param biomes       a biome tag it is limited to; absent means any biome.
 * @param indicator    a block laid on the surface above a vein that comes near enough to it (mechanics
 *                     §9.5): an outcrop, so a vein can be found by walking. Absent means none. Any
 *                     block id, so a pack may name a loose-rock block of its own; the vanilla
 *                     deposits name their own ore.
 * @param layers       see {@link Layers}: a cap, a rim and scattered ores beside the core.
 * @param indicatorDepth how far below the surface (blocks) a vein's centre may be and still show an outcrop.
 */
public record OreVein(ResourceLocation ore, ResourceLocation deepslateOre, TagKey<Block> host,
                      TagKey<Block> deepslateHost, int radius, int thickness, int weight, float density,
                      int minY, int maxY, boolean triangle, float airDiscard, List<ResourceLocation> dimensions,
                      Optional<TagKey<Biome>> biomes, Optional<ResourceLocation> indicator, int indicatorDepth,
                      Layers layers) {

    /** Widest a vein may be; a region is wider, which is what lets a chunk look only at its neighbouring regions. */
    public static final int MAX_RADIUS = 20;

    private static final TagKey<Block> STONE_HOST = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("stone_ore_replaceables"));
    private static final TagKey<Block> DEEPSLATE_HOST = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("deepslate_ore_replaceables"));

    public static final Codec<OreVein> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("ore").forGetter(OreVein::ore),
            ResourceLocation.CODEC.fieldOf("deepslate_ore").forGetter(OreVein::deepslateOre),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("host", STONE_HOST).forGetter(OreVein::host),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("deepslate_host", DEEPSLATE_HOST)
                    .forGetter(OreVein::deepslateHost),
            Codec.intRange(1, MAX_RADIUS).fieldOf("radius").forGetter(OreVein::radius),
            Codec.intRange(1, 16).fieldOf("thickness").forGetter(OreVein::thickness),
            ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(OreVein::weight),
            Codec.floatRange(0f, 1f).fieldOf("density").forGetter(OreVein::density),
            Codec.INT.fieldOf("min_y").forGetter(OreVein::minY),
            Codec.INT.fieldOf("max_y").forGetter(OreVein::maxY),
            Codec.BOOL.optionalFieldOf("triangle", false).forGetter(OreVein::triangle),
            Codec.floatRange(0f, 1f).optionalFieldOf("air_discard", 0.8f).forGetter(OreVein::airDiscard),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dimension_filter",
                    List.of(ResourceLocation.withDefaultNamespace("overworld"))).forGetter(OreVein::dimensions),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(OreVein::biomes),
            Tail.CODEC.forGetter(v -> new Tail(v.indicator(), v.indicatorDepth(), v.layers()))
    ).apply(instance, (ore, deepslateOre, host, deepslateHost, radius, thickness, weight, density, minY, maxY, triangle,
                       airDiscard, dimensions, biomes, tail) -> new OreVein(ore, deepslateOre, host, deepslateHost, radius,
            thickness, weight, density, minY, maxY, triangle, airDiscard, dimensions, biomes, tail.indicator(),
            tail.indicatorDepth(), tail.layers())));

    /** One more ore in a vein: {@code ore} where the host is a stone, {@code deepslate_ore} where it is a deepslate. */
    public record Zone(ResourceLocation ore, ResourceLocation deepslateOre, float density) {
        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("ore").forGetter(Zone::ore),
                ResourceLocation.CODEC.optionalFieldOf("deepslate_ore").forGetter(z -> Optional.of(z.deepslateOre())),
                Codec.floatRange(0f, 1f).fieldOf("density").forGetter(Zone::density)
        ).apply(instance, (ore, deepslate, density) -> new Zone(ore, deepslate.orElse(ore), density)));
    }

    /**
     * How a vein is laid out. The plain {@code ore} is its <em>core</em>. A {@code cap} replaces it in
     * the top of the vein (an oxidised cap over a deeper body: the weathered ores of a sulphide vein),
     * a {@code rim} in its outer shell (the halo), and each {@code scatter} ore sprinkles through the
     * whole of it at its own density. All optional; a vein with none is one ore throughout.
     */
    public record Layers(Optional<Zone> cap, Optional<Zone> rim, List<Zone> scatter) {
        public static final Layers NONE = new Layers(Optional.empty(), Optional.empty(), List.of());
    }

    /** The keys that did not fit the codec's sixteen. */
    private record Tail(Optional<ResourceLocation> indicator, int indicatorDepth, Layers layers) {
        static final com.mojang.serialization.MapCodec<Tail> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("indicator").forGetter(Tail::indicator),
                Codec.INT.optionalFieldOf("indicator_depth", 24).forGetter(Tail::indicatorDepth),
                Zone.CODEC.optionalFieldOf("cap").forGetter(t -> t.layers().cap()),
                Zone.CODEC.optionalFieldOf("rim").forGetter(t -> t.layers().rim()),
                Zone.CODEC.listOf().optionalFieldOf("scatter", List.of()).forGetter(t -> t.layers().scatter())
        ).apply(instance, (indicator, depth, cap, rim, scatter) -> new Tail(indicator, depth, new Layers(cap, rim, scatter))));
    }
}
