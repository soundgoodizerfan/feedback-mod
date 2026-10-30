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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own mob effects, the ones potion nodes name ({@code "effect": "feedback:<id>"}). A new effect is one {@link #effect} line here, a
 * lang key {@code effect.feedback.<id>}, and, if it does anything, a handler that reads {@code player.hasEffect(FEffects.X)} (as
 * {@code potion/SecondWind} does for stamina) -- the effect itself is a plain marker with no behaviour of its own.
 */
public final class FEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Feedback.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> SECOND_WIND = effect("second_wind", MobEffectCategory.BENEFICIAL, 0xF0D62C);
    /** The payback for {@link #SECOND_WIND}: stamina does not refill while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> WINDED = effect("winded", MobEffectCategory.HARMFUL, 0x8A7F3A);

    /** Slow falling plus light feet: no sinking in powder snow. Applies vanilla slow falling alongside ({@code potion/Featherstep}). */
    public static final DeferredHolder<MobEffect, MobEffect> FEATHERSTEP = effect("featherstep", MobEffectCategory.BENEFICIAL, 0xE6EFF7);
    /** Fire resistance with its trade: cold bites harder while it lasts ({@code potion/HeatWard}). */
    public static final DeferredHolder<MobEffect, MobEffect> HEAT_WARD = effect("heat_ward", MobEffectCategory.BENEFICIAL, 0xE49B0F);

    /** Guns: spread and camera kick fall to a fraction ({@code potion/SteadyHand}). */
    public static final DeferredHolder<MobEffect, MobEffect> STEADY_HAND = effect("steady_hand", MobEffectCategory.BENEFICIAL, 0x2AB0A8);
    /** Guns: reloads take less time ({@code potion/Quickdraw}); its crash is vanilla mining fatigue. */
    public static final DeferredHolder<MobEffect, MobEffect> QUICKDRAW = effect("quickdraw", MobEffectCategory.BENEFICIAL, 0xD8B02C);

    /** Cast techniques cost a fraction of their stamina and the body does not heal ({@code potion/Adrenaline}). */
    public static final DeferredHolder<MobEffect, MobEffect> ADRENALINE = effect("adrenaline", MobEffectCategory.NEUTRAL, 0xE0502C);

    /** Poison passes at a fraction while it lasts; breathing comes harder ({@code potion/Ward}). */
    public static final DeferredHolder<MobEffect, MobEffect> ANTIVENOM = effect("antivenom", MobEffectCategory.BENEFICIAL, 0x7A4A2A);

    /** Shock passes at a fraction; held metal warms ({@code potion/StaticGround}). */
    public static final DeferredHolder<MobEffect, MobEffect> STATIC_GROUND = effect("static_ground", MobEffectCategory.BENEFICIAL, 0x9AA6B8);

    /** Radiation dose passes at a fraction; healing runs slower ({@code potion/RadiationWard}). */
    public static final DeferredHolder<MobEffect, MobEffect> RADIATION_WARD = effect("radiation_ward", MobEffectCategory.BENEFICIAL, 0x9A7A5A);

    /** Readouts resolve at the truth tier while it lasts ({@code instrument/Instruments.reading}); stamina regen runs slower ({@code potion/SecondWind}). */
    public static final DeferredHolder<MobEffect, MobEffect> ASSAY = effect("assay", MobEffectCategory.NEUTRAL, 0x8A3FC0);

    /** Laced dishes and aftertastes are noticed at a lower threshold; plain food fills less ({@code potion/FinePalate}). */
    public static final DeferredHolder<MobEffect, MobEffect> FINE_PALATE = effect("fine_palate", MobEffectCategory.BENEFICIAL, 0x8C6A3A);

    /** Hot blocks near the drinker and hot pieces in their pack cool faster; frost builds faster in powder snow ({@code potion/Quench}). */
    public static final DeferredHolder<MobEffect, MobEffect> QUENCH = effect("quench", MobEffectCategory.BENEFICIAL, 0x7FB8C9);

    /**
     * Bare skin holds hotter pieces ({@code core/hazard/Protection.handlesUpToTu}); the trade is a dulled hand: slower swings and slower digging
     * (tiny attribute modifiers, so every hand tool slows with no code of its own).
     */
    public static final DeferredHolder<MobEffect, MobEffect> SMELTERS_BLOOD = EFFECTS.register("smelters_blood", () ->
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xB5562B) {}
                    .addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "smelters_blood_swing"),
                            -PotionTuning.SMELTERS_BLOOD_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "smelters_blood_dig"),
                            -PotionTuning.SMELTERS_BLOOD_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private FEffects() {}

    private static DeferredHolder<MobEffect, MobEffect> effect(String id, MobEffectCategory category, int color) {
        return EFFECTS.register(id, () -> new MobEffect(category, color) {});
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
