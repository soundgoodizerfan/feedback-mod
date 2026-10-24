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
import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The runtime kinds of the Wager effect ({@code docs/feedback_enchant_roster.md} 25), a gimmick with no mirror.
 * {@code gamble} on a head, blade or tip: a coin flip per blow, double damage or the target is healed for the blow
 * instead. {@code gamble_ward} on a plate or studs: a coin flip per hit taken (once, however many parts carry it),
 * double damage or nothing. Level does not move the odds. No wear of its own.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WagerKinds {

    /** Chance the coin lands in the wearer's or wielder's favour for a blow (double) and for a hit taken (nullified). [OPEN] until played. */
    private static final float WIN_BLOW = 0.5f, WIN_HIT = 0.5f;
    /** Damage multiplier on a win for the wielder, and on a loss for the wearer. [OPEN] until played. */
    private static final float DOUBLE = 2f;

    private WagerKinds() {
    }

    public static void register() {
        AttunementKinds.onStrike("gamble", (event, a) -> {
            if (event.getEntity().level().random.nextFloat() < WIN_BLOW) {
                event.setAmount(event.getAmount() * DOUBLE);
            } else {
                event.getEntity().heal(event.getAmount());
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || event.getAmount() <= 0f)
            return;
        for (ItemStack stack : wearer.getArmorSlots())
            if (TideKinds.level(stack, "gamble_ward") > 0) {
                if (wearer.getRandom().nextFloat() < WIN_HIT)
                    event.setCanceled(true);
                else
                    event.setAmount(event.getAmount() * DOUBLE);
                return;
            }
    }
}
