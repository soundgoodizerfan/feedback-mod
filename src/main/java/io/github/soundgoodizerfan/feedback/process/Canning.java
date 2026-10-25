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

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.item.Canned;
import io.github.soundgoodizerfan.feedback.item.Meal;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Jarred food (food spec 8): the jar's eating figures are the contained food's, softened by mush, and the
 * jar is what is left when it is eaten. Nothing here ticks an item; the Pressure Vessel writes progress back.
 */
public final class Canning {

    private Canning() {
    }

    /** Whether this stack can go in a jar: a food a vessel cooks, or a ladled serving. */
    public static boolean fillable(Level level, ItemStack stack) {
        return Cooking.find(level, stack).isPresent() || stack.has(FDataComponents.MEAL.get());
    }

    /** A jar of this one food, not yet sterilised. */
    public static ItemStack fill(ItemStack content) {
        ItemStack jar = new ItemStack(FItems.JARRED_FOOD.get());
        jar.set(FDataComponents.CANNED.get(), new Canned(content.copyWithCount(1), 0f, false, 0f));
        refresh(jar);
        return jar;
    }

    /** Put the contained food's eating figures on the jar, scaled down by mush, converting to an empty jar when eaten. */
    public static void refresh(ItemStack jar) {
        Canned can = jar.get(FDataComponents.CANNED.get());
        if (can == null)
            return;
        FoodProperties base = can.food().get(DataComponents.FOOD);
        if (base == null)
            return;
        float scale = 1f - can.mush() * (1f - FTuning.FOOD_CHARRED_SCALE);
        jar.set(DataComponents.FOOD, new FoodProperties(Math.round(base.nutrition() * scale), base.saturation() * scale,
                base.canAlwaysEat(), base.eatSeconds(), java.util.Optional.of(new ItemStack(FItems.JAR.get())), base.effects()));
    }

    /** Eating an unsealed jar: its food has been spoiling all along, so settle that first. */
    public static void settle(ItemStack jar, Level level) {
        Canned can = jar.get(FDataComponents.CANNED.get());
        if (can == null || can.sealed())
            return;
        ItemStack in = can.food().copy();
        Cooking.settle(in, level, AmbientTable.at(level).value());
        jar.set(FDataComponents.CANNED.get(), new Canned(in, can.sterile(), false, can.mush()));
        refresh(jar);
    }

    /** Close the jar over its food: settle what spoilage it has earned, then kill what is live in it and freeze the clock. */
    public static Canned seal(Canned can, Level level) {
        ItemStack in = can.food().copy();
        Cooking.find(level, in).ifPresentOrElse(entry -> {
            Cooking.settle(in, level, AmbientTable.at(level).value());
            FoodState s = Cooking.state(in, entry);
            FoodState clean = new FoodState(s.cook(), s.moisture(), 0f, s.toxin(), s.charred(), s.rot(), s.stamp(), s.envTu(), s.smoke(), s.salt());
            in.set(FDataComponents.FOOD_STATE.get(), clean);
            Cooking.apply(in, entry, clean);
        }, () -> {
            Meal meal = in.get(FDataComponents.MEAL.get());
            FoodProperties food = in.get(DataComponents.FOOD);
            if (meal == null || food == null)
                return;
            in.set(FDataComponents.MEAL.get(), new Meal(meal.dish(), 0f, meal.compounds(), meal.fat(), meal.fibre(), meal.ingredients()));
            // A serving's live load is only ever the Hunger it bakes in.
            List<FoodProperties.PossibleEffect> effects = food.effects().stream()
                    .filter(e -> !e.effect().getEffect().is(MobEffects.HUNGER)).toList();
            in.set(DataComponents.FOOD, new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(),
                    food.eatSeconds(), food.usingConvertsTo(), effects));
        });
        return new Canned(in, 1f, true, can.mush());
    }
}
