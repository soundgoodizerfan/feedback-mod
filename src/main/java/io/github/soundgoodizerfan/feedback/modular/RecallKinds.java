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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The runtime kinds of the Recall effect ({@code docs/feedback_enchant_roster.md}). {@code recall}: a thrown weapon
 * whose head or tip is attuned comes back as Loyalty does, but only if that part is magnetic -- its material has a
 * Curie temperature -- and is not heated past it. {@code anchor} (the mirror): the struck target is planted.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RecallKinds {

    /** The return rate a recall gives a thrown weapon, before {@link Throwing} scales it by mass. [OPEN] until played. */
    private static final float RETURN = 0.6f;
    /** Ticks a struck target is held per level of anchor. [OPEN] until played. */
    private static final int ANCHOR_TICKS = 40;
    /** Slowness amplitude that stops a walker (past -100% speed). [OPEN] until played. */
    private static final int ANCHOR_SLOW = 6;

    private RecallKinds() {
    }

    static void register() {
    }

    /** The return rate attunement gives {@code weapon} when thrown: 0 unless a recalling part is magnetic and below its Curie point. */
    static float returns(ItemStack weapon, Level level) {
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(weapon))
            if (a.kind().equals("recall") && a.material().separation().curieTemperature()
                    .filter(curie -> ItemHeat.get(weapon, level).value() <= curie.value()).isPresent())
                return RETURN;
        return 0f;
    }

    @SubscribeEvent
    public static void onStruck(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getDirectEntity() instanceof ThrownWeapon thrown))
            return;
        int level = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(thrown.stack()))
            if (a.kind().equals("anchor"))
                level = Math.max(level, a.level());
        if (level > 0)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ANCHOR_TICKS * level, ANCHOR_SLOW, false, false));
    }
}
