// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * What an armour plate's material does besides stop blows: it turns pierce damage and it carries fire.
 *
 * <h2>Pierce</h2>
 * A projectile ({@code #minecraft:is_projectile}) is cut by the sum of {@link ModularFigures.Fig#PIERCE_RESIST}
 * over the worn modular pieces, each weighted by its slot ({@code PIERCE_WEIGHT_*}), never below 0.
 *
 * <h2>Fire conduction</h2>
 * While the wearer burns or stands in lava, every {@code FIRE_SOAK_INTERVAL} ticks each worn piece with
 * {@link ModularFigures.Fig#FIRE_CONDUCT} stamps its {@link ItemHeat} towards the fire's temperature (the way
 * {@link ShieldHeat} warms a shield), and the wearer takes {@code FIRE_SOAK_PER_CONDUCT} heat damage per second for
 * each point summed: a fire-tagged hot-contact hit, so fire resistance answers it. Nothing runs when the wearer is
 * not alight, and nothing is booked from uptime.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ArmourSplit {

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmourSplit() {
    }

    private static float weight(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> ModularTuning.PIERCE_WEIGHT_CHEST;
            case LEGS -> ModularTuning.PIERCE_WEIGHT_LEGS;
            case HEAD -> ModularTuning.PIERCE_WEIGHT_HEAD;
            default -> ModularTuning.PIERCE_WEIGHT_FEET;
        };
    }

    @SubscribeEvent
    public static void onPierce(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || !event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            return;
        float resist = 0f;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = wearer.getItemBySlot(slot);
            if (!(piece.getItem() instanceof ModularArmor))
                continue;
            ModularFigures f = ModularStacks.figures(piece);
            if (f != null && !ModularStacks.broken(piece))
                resist += weight(slot) * f.get(ModularFigures.Fig.PIERCE_RESIST);
        }
        if (resist > 0f)
            event.setAmount(Math.max(0f, event.getAmount() * (1f - Math.min(1f, resist))));
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity wearer))
            return;
        Level level = wearer.level();
        if (level.isClientSide || wearer.tickCount % ModularTuning.FIRE_SOAK_INTERVAL != 0)
            return;
        boolean lava = wearer.isInLava();
        if (!lava && !wearer.isOnFire())
            return;
        float source = (lava ? FTuning.FIRE_TU_LAVA : FTuning.FIRE_TU_CAMPFIRE).value();
        float conduct = 0f;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = wearer.getItemBySlot(slot);
            if (!(piece.getItem() instanceof ModularArmor))
                continue;
            ModularFigures f = ModularStacks.figures(piece);
            if (f == null || f.get(ModularFigures.Fig.FIRE_CONDUCT) <= 0f)
                continue;
            conduct += f.get(ModularFigures.Fig.FIRE_CONDUCT);
            float now = ItemHeat.get(piece, level).value();
            ShieldHeat.stamp(piece, ShieldHeat.approach(now, source, ShieldHeat.thermalMass(f), ModularTuning.FIRE_SOAK_INTERVAL), level);
        }
        if (conduct > 0f)
            wearer.hurt(Hazards.source(level, Hazards.HOT_CONTACT),
                    conduct * ModularTuning.FIRE_SOAK_PER_CONDUCT * ModularTuning.FIRE_SOAK_INTERVAL / 20f);
    }
}
