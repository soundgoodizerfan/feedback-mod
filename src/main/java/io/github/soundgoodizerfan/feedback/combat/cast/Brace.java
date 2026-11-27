// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Brace, always on once owned (a stance, not a cast): while crouched, knockback cannot move you and every blow taken is held by your mass (armour counts) against the attacker's; the held share goes into the attacker as stagger and the held weapon wears for it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Brace {

    private Brace() {
    }

    /** Mass (Mu) each point of worn armour toughness adds to what holds a blow, with Plate up ([OPEN]). */
    private static final float PLATE_UP_MU_PER_TOUGHNESS = 1.5f;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onBraced(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !braced(p)
                || !(event.getSource().getEntity() instanceof LivingEntity foe) || foe.distanceToSqr(p) > 36)
            return;
        float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = BodyMass.of(foe) + BodyMass.armour(foe);
        // Plate up: worn armour toughness holds as well as its mass.
        if (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "plate_up"))
            mine += (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) * PLATE_UP_MU_PER_TOUGHNESS;
        float held = event.getAmount() * Mth.clamp(mine / (mine + theirs), 0.3f, 0.8f);        event.setAmount(event.getAmount() - held);
        stagger(p, foe, new Work(held * ParryTuning.WORK_PER_DAMAGE));
        ItemStack weapon = p.getMainHandItem();
        if (weapon.isDamageableItem())
            weapon.hurtAndBreak(Math.max(1, (int) (held / 4)), p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        ServerLevel lv = p.serverLevel();
        Vec3 clash = p.position().add(0, 1.2, 0).add(foe.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(0.8));
        CastFx.shards(lv, clash, 0xFFFFFF, 1.1f, 6);
        CastFx.pulse(lv, clash, foe.position().subtract(p.position()).multiply(1, 0, 1), CastFx.STEEL, 1.2f, 7);
        CastFx.sound(lv, clash, FSounds.CAST_GUARD.get(), 1f, 0.8f);
    }

    /** Braced is crouching with the skill: the player's own stance, so there is no window or key. */
    private static boolean braced(ServerPlayer p) {
        return p.isShiftKeyDown() && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "brace");
    }

    /** A braced fighter cannot be shoved. */
    @SubscribeEvent
    public static void onKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && braced(p))
            event.setCanceled(true);
    }
}
