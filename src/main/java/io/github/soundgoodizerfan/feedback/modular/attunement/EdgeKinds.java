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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The runtime kinds of the Edge effect. Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class EdgeKinds {

    /** Share of a projectile's damage one level of a part turns aside, per St of the part's hardness. [OPEN] until played. */
    private static final float DEFLECT_PER_HARDNESS = 0.01f;
    /** Most of a projectile the whole worn set can turn aside. [OPEN] until played. */
    private static final float DEFLECT_CAP = 0.7f;

    /** Share of a blow one level of pad turns into stagger instead of damage, and the most that can be. [OPEN] until played. */
    private static final float PAD_CUT_PER_LEVEL = 0.25f, PAD_CUT_CAP = 0.75f;
    /** Ticks of slowness and weakness a padded blow leaves, per level. [OPEN] until played. */
    private static final int PAD_STAGGER_TICKS_PER_LEVEL = 40;

    private EdgeKinds() {
    }

    public static void register() {
        onStrike("extra_damage", (event, a) ->
                event.setAmount(event.getAmount() + a.level() * ModularTuning.PERK_DAMAGE_PER_LEVEL));
        // Edge- spreads the force over more of the target: less damage, and a stagger that lets you take it alive.
        onStrike("pad", (event, a) -> {
            event.setAmount(event.getAmount() * (1f - Math.min(PAD_CUT_CAP, a.level() * PAD_CUT_PER_LEVEL)));
            int ticks = a.level() * PAD_STAGGER_TICKS_PER_LEVEL;
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks));
        });
    }

    /** Hardness against hardness: harder plate turns more of an arrow aside. */
    @SubscribeEvent
    public static void onDeflect(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            return;
        float share = 0f;
        for (ItemStack worn : event.getEntity().getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("deflect"))
                    share += a.level() * a.material().hardness().value() * DEFLECT_PER_HARDNESS;
        if (share > 0f)
            event.setAmount(event.getAmount() * (1f - Math.min(DEFLECT_CAP, share)));
    }
}
