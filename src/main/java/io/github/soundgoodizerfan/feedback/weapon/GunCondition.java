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
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A gun's two bits of runtime state besides its rounds ({@code docs/feedback_guns_spec.md} sections
 * 3.4 and 6): the barrel's temperature and the bore's wear.
 *
 * <p>Temperature is {@link ItemHeat}'s stamp and tick, unchanged -- a hot gun cools in a holster,
 * a chest or an unloaded chunk because nothing has to tick it -- and cools at the gun's own rate
 * ({@link GunItem#coolingRate}, mass over leak). It is stamped here directly rather than through
 * {@link ItemHeat#set}, which drops anything under the warm threshold: a pistol's few Tu per shot
 * would otherwise never accumulate from cold.
 *
 * <p>Wear is a number on the stack, and what it does is multiply the spec sheet ({@link
 * GunFigures#aged}). It rises with use (a tool wears with use, spec 6) and faster when hot.
 */
public final class GunCondition {

    private GunCondition() {
    }

    public static Tu temperature(ItemStack gun, Level level) {
        return ItemHeat.get(gun, level);
    }

    /** The barrel's temperature as a fraction of {@code ignitionTu}, counted up from the room. */
    public static float heatFraction(ItemStack gun, Level level, float ignitionTu) {
        float ambient = AmbientTable.at(level).value();
        return Math.max(0f, (temperature(gun, level).value() - ambient) / Math.max(1f, ignitionTu - ambient));
    }

    public static float wear(ItemStack gun) {
        return gun.getOrDefault(FWeapons.GUN_WEAR.get(), 0f);
    }

    /** 1 as built, falling with wear to a floor above zero. */
    public static float condition(ItemStack gun) {
        return Math.max(FTuning.GUN_CONDITION_FLOOR, 1f - wear(gun));
    }

    /** {@code base} as this gun is now: worn and hot. */
    public static GunFigures effective(GunFigures base, ItemStack gun, Level level, float ignitionTu) {
        return base.aged(condition(gun), heatFraction(gun, level, ignitionTu));
    }

    /**
     * Book a shot: the barrel takes its heat over its thermal mass, and the bore its wear, both
     * larger the more work the shot did, and the wear larger again for a hot barrel.
     *
     * @param f    the figures the shot was fired at.
     * @param heat the barrel's heat fraction when it was fired.
     */
    public static void afterShot(ItemStack gun, GunFigures f, Level level, float heat) {
        float t = temperature(gun, level).value() + f.muzzleEnergy() * FTuning.GUN_HEAT_PER_WORK * f.heatFactor()
                / f.thermalMass().workPerTu();
        gun.set(FDataComponents.TEMPERATURE.get(), t);
        gun.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
        gun.set(FWeapons.GUN_WEAR.get(), wear(gun) + f.muzzleEnergy() * FTuning.GUN_WEAR_PER_WORK * (1 + Math.max(0f, heat)));
    }

    /**
     * Whether a coil's winding is above its quench temperature: the superconductor has reverted
     * and carries nothing until it has cooled (the redstone torch's failure mode, spec 3.7). A
     * physical impossibility, so a hard gate with a cause; a chamber has no such limit.
     */
    public static boolean quenched(ItemStack gun, Level level, GunSpec.Load load) {
        return load.coil() && temperature(gun, level).value() >= load.ignitionTu();
    }

    /** A word for how hot the barrel is. A live reading is an instrument's to give; this is what a hand can tell. */
    public static String feel(float heat) {
        return heat < FEEL_EDGES[0] ? "cool" : heat < FEEL_EDGES[1] ? "warm" : heat < FEEL_EDGES[2] ? "hot" : "smoking";
    }

    /** Where {@link #feel}'s words change, as a share of the way from the room to cook-off. */
    public static final float[] FEEL_EDGES = {0.25f, 0.6f, 0.9f};
}
