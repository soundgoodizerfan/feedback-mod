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
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * What a crop wants of its weather ({@code docs/feedback_modular_foods_spec.md} 2.3): {@code data/<ns>/crop_climate/*.json},
 * each line a block tag with a temperature band, so a modded crop joins by tagging, never by id. A random tick grows the
 * crop only as often as the place suits it: inside the band always, outside it less and less, and under {@code frost}
 * not at all. It halts and never kills, so a crop recovers when it warms; nothing is a gate, it is a rate (outside the
 * band it still grows, at {@code floor}). Bonemeal does not fire the event and is untouched.
 *
 * <p>The weather is the dimension's ambient ({@link AmbientTable}) shifted by the biome's own temperature.
 *
 * <p>shortcut: no heat from a warmed block (a hearth under a planter) and no sky-light curve; add either when a crop
 * needs it.
 */
public final class CropClimate extends SimpleJsonResourceReloadListener {

    /**
     * @param tag       the block tag this line is for
     * @param min       the band's low edge, Tu
     * @param max       the band's high edge, Tu
     * @param tolerance Tu outside the band over which the rate falls from 1 to {@code floor}
     * @param frost     under this Tu growth halts
     * @param floor     the least rate outside the band and above frost
     * @param light     the least brightness (0-15) it grows fully in; darker is {@code floor}
     * @param wet       whether it wants standing water within two blocks (a paddy); without, {@code floor}
     */
    public record Entry(TagKey<Block> tag, float min, float max, float tolerance, float frost, float floor, int light, boolean wet) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.xmap(id -> TagKey.create(Registries.BLOCK, id), TagKey::location).fieldOf("tag").forGetter(Entry::tag),
                Codec.FLOAT.fieldOf("min").forGetter(Entry::min),
                Codec.FLOAT.fieldOf("max").forGetter(Entry::max),
                Codec.floatRange(0.01f, 1000f).optionalFieldOf("tolerance", 15f).forGetter(Entry::tolerance),
                Codec.FLOAT.optionalFieldOf("frost", -1000f).forGetter(Entry::frost),
                Codec.floatRange(0f, 1f).optionalFieldOf("floor", 0.25f).forGetter(Entry::floor),
                Codec.intRange(0, 15).optionalFieldOf("light", 0).forGetter(Entry::light),
                Codec.BOOL.optionalFieldOf("wet", false).forGetter(Entry::wet)
        ).apply(i, Entry::new));
    }

    private static final CropClimate INSTANCE = new CropClimate();

    private List<Entry> entries = List.of();

    private CropClimate() {
        super(new com.google.gson.GsonBuilder().create(), "crop_climate");
    }

    public static CropClimate get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Entry> loaded = new TreeMap<>();
        json.forEach((id, element) -> Entry.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad crop climate {}: {}", id, error))
                .ifPresent(e -> loaded.put(id, e)));
        entries = new ArrayList<>(loaded.values());
        Feedback.LOGGER.info("Loaded {} crop climates", entries.size());
    }

    /** The weather a plant at {@code pos} feels, Tu. */
    public static float weather(Level level, BlockPos pos) {
        return AmbientTable.at(level).value() + (level.getBiome(pos).value().getBaseTemperature() - 0.8f) * FTuning.CROP_TU_PER_CLIMATE;
    }

    /** 0..1: how often a random tick of this crop at {@code pos} may grow it; 1 for a block no line names. */
    public static float rate(Level level, BlockPos pos, BlockState state) {
        for (Entry e : INSTANCE.entries) {
            if (!state.is(e.tag()))
                continue;
            float t = weather(level, pos);
            if (t < e.frost())
                return 0f;
            float off = Math.max(e.min() - t, t - e.max());
            float rate = off <= 0f ? 1f : Mth.lerp(Math.min(1f, off / e.tolerance()), 1f, e.floor());
            boolean dark = level.getRawBrightness(pos, 0) < e.light();
            return dark || e.wet() && !standingWater(level, pos) ? Math.min(rate, e.floor()) : rate;
        }
        return 1f;
    }

    /** Water in a source or flowing block within two blocks sideways of the crop or its soil: what a paddy wants. */
    private static boolean standingWater(Level level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2)))
            if (level.getFluidState(near).is(net.minecraft.tags.FluidTags.WATER))
                return true;
        return false;
    }

    /** Whether a line names this block, so a thermometer has a growth to read on it. */
    public static boolean covers(BlockState state) {
        return INSTANCE.entries.stream().anyMatch(e -> state.is(e.tag()));
    }

    public static void onGrow(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide
                && level.getRandom().nextFloat() >= rate(level, event.getPos(), event.getState()))
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }
}
