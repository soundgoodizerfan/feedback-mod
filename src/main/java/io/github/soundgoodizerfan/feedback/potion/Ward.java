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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.registry.FEffects;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * The ward schema. A ward is one effect that names a hazard class and the fraction of it that still gets through; never immunity. Every ward also
 * carries its mandatory trade, a factor on stamina regen (the one meter, touched only in rate), so no ward is simply best.
 *
 * <p>A hazard asks {@link #passes}; the stamina code asks {@link #regenFactor}. A new ward is one {@link Entry} line plus its effect in
 * {@link FEffects}; a new hazard class is one {@link Hazard} constant and a {@code passes} call where its damage or dose lands.
 */
public final class Ward {

    /** What a ward guards against. */
    public enum Hazard {
        /** Poison, wherever it comes from: fumes, food toxin, stings. Hooked at the poison tick ({@code WardEvents}). */
        POISON,
        HEAT_COLD, RADIATION, SHOCK, FOOD
    }

    /** One ward: its effect, the class it guards, the fraction of that class passing, and the stamina regen factor it costs. */
    public record Entry(Supplier<? extends Holder<MobEffect>> effect, Hazard guards, float pass, float regen) {}

    public static final List<Entry> ENTRIES = List.of(
            new Entry(() -> FEffects.ANTIVENOM, Hazard.POISON, PotionTuning.ANTIVENOM_PASS, PotionTuning.ANTIVENOM_REGEN),
            // Trade is not stamina: held metal warms (potion/StaticGround), so regen stays 1.
            new Entry(() -> FEffects.STATIC_GROUND, Hazard.SHOCK, PotionTuning.STATIC_GROUND_PASS, 1f),
            // Trade is healing, not stamina (potion/RadiationWard): the scavenger spends the body's repair.
            new Entry(() -> FEffects.RADIATION_WARD, Hazard.RADIATION, PotionTuning.RADIATION_WARD_PASS, 1f));

    private Ward() {}

    /** The fraction of {@code guards} that still reaches {@code who}: 1 unprotected, the product over every active ward of the class. */
    public static float passes(LivingEntity who, Hazard guards) {
        float pass = 1f;
        for (Entry e : ENTRIES)
            if (e.guards() == guards && who.hasEffect(e.effect().get()))
                pass *= e.pass();
        return pass;
    }

    /** Stamina regen multiplier from every active ward's trade (1 with none). */
    public static float regenFactor(LivingEntity who) {
        float f = 1f;
        for (Entry e : ENTRIES)
            if (who.hasEffect(e.effect().get()))
                f *= e.regen();
        return f;
    }
}
