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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The runtime kinds of the Grudge effect ({@code docs/feedback_enchant_roster.md} 18). {@code grudge} (plate, lining,
 * head, blade): damage the wearer takes is added to the Charge meter of the tool in the main hand
 * ({@link ChargeKinds#add}: the same {@code CHARGE_STORED}, the same capacity), so the next armed Charge release spends
 * it. A Charge tool's own grudge part fills its own bar; with no Charge build on the held tool it does nothing.
 * {@code calm} (the mirror, same roles): damage taken bleeds the held bar instead.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GrudgeKinds {

    /** Charge added per point of damage taken, per level of grudge. [OPEN] until played */
    private static final float CHARGE_PER_DAMAGE = 1f;
    /** Charge a level of calm bleeds per hit taken. [OPEN] until played */
    private static final float CALM_PER_LEVEL = 2f;

    private GrudgeKinds() {
    }

    public static void register() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player wearer) || wearer.level().isClientSide
                || event.getNewDamage() <= 0)
            return;
        ItemStack held = wearer.getMainHandItem();
        if (TideKinds.level(held, "build") == 0)
            return;
        int grudge = TideKinds.level(held, "grudge"), calm = TideKinds.level(held, "calm");
        for (ItemStack worn : wearer.getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn)) {
                if (a.kind().equals("grudge"))
                    grudge += a.level();
                else if (a.kind().equals("calm"))
                    calm += a.level();
            }
        float delta = CHARGE_PER_DAMAGE * event.getNewDamage() * grudge - CALM_PER_LEVEL * calm;
        if (delta != 0)
            ChargeKinds.add(held, wearer, delta);
    }
}
