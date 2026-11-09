// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.EventTrigger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Advancement witnesses for modular armour: a blow that armour had a say in, and a whole outfit of it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ArmourAdvancements {

    private ArmourAdvancements() {
    }

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                || event.getSource().getEntity() == null || event.getNewDamage() <= 0f)
            return;
        int worn = 0;
        for (EquipmentSlot slot : EquipmentSlot.values())
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && p.getItemBySlot(slot).getItem() instanceof ModularArmor)
                worn++;
        if (worn > 0)
            EventTrigger.fire(p, "progress/combat/armour_worn");
        if (worn == 4)
            EventTrigger.fire(p, "progress/combat/armour_set");
    }
}
