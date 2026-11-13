// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A gun's two bits of runtime state besides its rounds ({@code docs/specs/feedback_guns_spec.md} sections
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
        return Math.max(GunTuning.GUN_CONDITION_FLOOR, 1f - wear(gun));
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
    public static void afterShot(ItemStack gun, GunFigures f, Level level, float heat, float boreWear) {
        float t = temperature(gun, level).value() + f.muzzleEnergy() * GunTuning.GUN_HEAT_PER_WORK * f.heatFactor()
                * io.github.soundgoodizerfan.feedback.modular.attunement.HeatKinds.gunShotHeat(gun)
                / f.thermalMass().workPerTu();
        gun.set(FDataComponents.TEMPERATURE.get(), t);
        gun.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
        // A receiver the barrel's heat carries past its service temperature softens: each shot wears the gun by the excess.
        float soft = 0f;
        var spec = GunSpec.of(gun);
        if (spec.isPresent()) {
            var rm = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(spec.get().parts().receiver());
            float service = rm == null ? Float.MAX_VALUE : rm.thermal().serviceTemperature().map(v -> v.value()).orElse(Float.MAX_VALUE);
            soft = Math.max(0f, t - service) * GunTuning.GUN_RECEIVER_SOFT_WEAR_PER_TU;
        }
        float worn = soft + f.muzzleEnergy() * GunTuning.GUN_WEAR_PER_WORK * boreWear * (1 + Math.max(0f, heat));
        // An attuned Ward part turns wear away as it does on a tool: n levels leave 1/(n+1) of it.
        gun.set(FWeapons.GUN_WEAR.get(), wear(gun)
                + worn / (1 + io.github.soundgoodizerfan.feedback.modular.attunement.WardKinds.levels(gun)));
    }

    /**
     * Book wear a misuse caused on a gun that has no bore to wear by firing (a spring gun, a pulse gun): turned away by its
     * Ward parts as {@link #afterShot} does, so soak, mend and tithe act on it. Ordinary shots never call this.
     */
    public static void misuse(ItemStack gun, float amount) {
        gun.set(FWeapons.GUN_WEAR.get(), wear(gun)
                + amount / (1 + io.github.soundgoodizerfan.feedback.modular.attunement.WardKinds.levels(gun)));
    }

    /** The condition line a worn gun's tooltip carries, if it is worn. */
    public static java.util.Optional<net.minecraft.network.chat.Component> wearLine(ItemStack gun) {
        return wear(gun) > 0 ? java.util.Optional.of(net.minecraft.network.chat.Component.translatable("gun.feedback.wear",
                Math.round(100 - 100 * condition(gun))).withStyle(net.minecraft.ChatFormatting.YELLOW)) : java.util.Optional.empty();
    }

    /** A word for how hot the barrel is. A live reading is an instrument's to give; this is what a hand can tell. */
    public static String feel(float heat) {
        return heat < FEEL_EDGES[0] ? "cool" : heat < FEEL_EDGES[1] ? "warm" : heat < FEEL_EDGES[2] ? "hot" : "smoking";
    }

    /** Where {@link #feel}'s words change, as a share of the way from the room to cook-off. */
    public static final float[] FEEL_EDGES = {0.25f, 0.6f, 0.9f};
}
