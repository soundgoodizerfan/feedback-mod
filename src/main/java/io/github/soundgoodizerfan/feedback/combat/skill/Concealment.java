// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Veil's concealed carry: a player holding a weapon that scores on stealth ({@link WeaponProfile#stealth}) is noticed
 * from nearer, in proportion to the share. A held item above the burn line glows and is seen: no help from it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Concealment {

    private Concealment() {
    }

    /** Stealth share a weapon needs to hide, and the most visibility it takes off at a full share ([OPEN]). */
    public static final float MIN_STEALTH = 0.6f, MAX_HIDE = 0.4f;

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "concealed_carry"))
            return;
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty() || ItemHeat.isHot(held, p.level()))
            return;
        float stealth = WeaponProfile.of(held).stealth();
        if (stealth >= MIN_STEALTH)
            event.modifyVisibility(1.0 - MAX_HIDE * stealth);
    }
}
