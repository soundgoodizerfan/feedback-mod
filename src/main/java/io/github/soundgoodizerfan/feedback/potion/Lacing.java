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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.item.Meal;
import io.github.soundgoodizerfan.feedback.process.Compound;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A potion laced into a food (potions spec 7, 14.9): a hidden buff riding on a meal or dish stack, plus the potion's taste. The effect
 * reaches the eater through the food buff path ({@code BuffEvents}); the taste is what gives it away.
 *
 * @param effect    the mob effect id.
 * @param amplifier level (0 is I), as the potion gave it when poured.
 * @param duration  ticks this serving carries (the bottle's, shared out over the servings).
 * @param taste     the potion's three taste figures.
 * @param strength  0..1, the potion's potency shared out over the servings: how loud the taste is.
 */
public record Lacing(ResourceLocation effect, int amplifier, int duration, List<Float> taste, float strength) {

    public static final Codec<Lacing> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Lacing::effect),
            Codec.INT.fieldOf("amplifier").forGetter(Lacing::amplifier),
            Codec.INT.fieldOf("duration").forGetter(Lacing::duration),
            Codec.FLOAT.listOf().fieldOf("taste").forGetter(Lacing::taste),
            Codec.FLOAT.fieldOf("strength").forGetter(Lacing::strength)
    ).apply(i, Lacing::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Lacing> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** What a potion makes of itself shared over {@code servings}, or empty for a dud or a flat bottle. */
    public static Optional<Lacing> of(PotionState state, net.minecraft.world.level.Level level, int servings) {
        if (state.dud())
            return Optional.empty();
        PotionFeature node = PotionMap.get().byEffect(state.effect().get());
        int amplifier = state.amplifierNow(level);
        if (node == null || amplifier < 0)
            return Optional.empty();
        int n = Math.max(1, servings);
        return Optional.of(new Lacing(state.effect().get(), amplifier, Math.max(1, state.duration() / n), state.taste(), state.potencyNow(level) / n));
    }

    /** The food's own taste: the mean of its compounds' tastes weighted by concentration (zeros for a food with none). */
    public static float[] foodTaste(Meal meal) {
        float[] sum = new float[3];
        float weight = 0f;
        for (Map.Entry<String, Float> e : meal.compounds().entrySet()) {
            Compound c = CompoundTable.get().find(e.getKey()).orElse(null);
            if (c == null || c.taste().size() < 3)
                continue;
            for (int k = 0; k < 3; k++)
                sum[k] += e.getValue() * c.taste().get(k);
            weight += e.getValue();
        }
        if (weight > 0f)
            for (int k = 0; k < 3; k++)
                sum[k] /= weight;
        return sum;
    }

    private float distanceFrom(float[] other) {
        float d = 0f;
        for (int k = 0; k < 3 && k < taste.size(); k++)
            d += (taste.get(k) - other[k]) * (taste.get(k) - other[k]);
        return (float) Math.sqrt(d);
    }

    /** Whether an eater notices it: loud enough, and far enough from the food's own taste. */
    public boolean detectable(Meal meal, net.minecraft.world.entity.player.Player eater) {
        float keen = FinePalate.keenness(eater);
        return strength >= PotionTuning.TASTE_MIN_STRENGTH * keen && distanceFrom(foodTaste(meal)) > PotionTuning.TASTE_NOTICE_DISTANCE * keen;
    }

    /** The compound class whose taste is nearest the potion's, or empty if none carries a taste. */
    public Optional<String> nearestClass() {
        String best = null;
        float bestD = Float.MAX_VALUE;
        for (Map.Entry<String, Compound> e : CompoundTable.get().all().entrySet()) {
            if (e.getValue().taste().size() < 3)
                continue;
            float d = distanceFrom(new float[] {e.getValue().taste().get(0), e.getValue().taste().get(1), e.getValue().taste().get(2)});
            if (d < bestD) {
                bestD = d;
                best = e.getKey();
            }
        }
        return Optional.ofNullable(best);
    }
}
