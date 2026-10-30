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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads every {@link PotionFeature} from {@code data/<namespace>/potion_effect/*.json}, keyed by id. Server only: the screen is sent the parts of the map its player may see. */
public final class PotionMap extends SimpleJsonResourceReloadListener {

    private static final PotionMap INSTANCE = new PotionMap();

    private Map<ResourceLocation, PotionFeature> features = Map.of();

    private PotionMap() {
        super(new GsonBuilder().create(), "potion_effect");
    }

    public static PotionMap get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, PotionFeature> loaded = new TreeMap<>();
        json.forEach((id, element) -> PotionFeature.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad potion feature {}: {}", id, error))
                .filter(f -> f.kind() != PotionFeature.Kind.EFFECT || f.effect().isPresent())
                .filter(f -> f.kind() != PotionFeature.Kind.VORTEX || f.destination().isPresent())
                .ifPresent(f -> loaded.put(id, f)));
        features = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} potion map features", features.size());
    }

    /** Every feature on a base's map, in id order. */
    public List<Map.Entry<ResourceLocation, PotionFeature>> on(String base) {
        List<Map.Entry<ResourceLocation, PotionFeature>> out = new ArrayList<>();
        for (var e : features.entrySet())
            if (e.getValue().base().equals(base))
                out.add(e);
        out.sort(Map.Entry.comparingByKey());
        return out;
    }

    /** How far from the centre the base's wall stands: its {@code boundary} row, else the whole plane. */
    public float boundary(String base) {
        for (var e : on(base))
            if (e.getValue().kind() == PotionFeature.Kind.BOUNDARY)
                return e.getValue().radius();
        return 1f;
    }

    public PotionFeature find(ResourceLocation id) {
        return features.get(id);
    }

    /** The effect node that makes {@code effect}, or null. */
    public PotionFeature byEffect(ResourceLocation effect) {
        for (PotionFeature f : features.values())
            if (f.kind() == PotionFeature.Kind.EFFECT && f.effect().get().effect().equals(effect))
                return f;
        return null;
    }
}
