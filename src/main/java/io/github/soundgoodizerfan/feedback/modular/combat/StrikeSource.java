// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A melee blow struck by the <b>off hand</b>. Vanilla's attack is main-hand only, so a damage
 * source that carries no hand means the main hand; the off-hand swing
 * ({@code combat.hit.OffhandStrike}) and the trait secondary hits it spawns wrap their source in
 * this, and every weapon hook asks {@link #weapon} for the stack that dealt the blow rather than
 * reading the main hand. Traits, perks, wear and drops then follow the weapon that struck.
 */
public final class StrikeSource extends DamageSource {

    private final InteractionHand hand;

    private StrikeSource(DamageSource base, InteractionHand hand) {
        super(base.typeHolder(), base.getDirectEntity(), base.getEntity(), base.getSourcePosition());
        this.hand = hand;
    }

    /** {@code base} marked as struck by {@code hand}. */
    public static DamageSource of(DamageSource base, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? base : new StrikeSource(base, hand);
    }

    /** {@code base} marked as struck by whichever hand of {@code attacker} holds {@code weapon}. */
    public static DamageSource of(DamageSource base, LivingEntity attacker, ItemStack weapon) {
        return of(base, hand(attacker, weapon));
    }

    public static InteractionHand hand(DamageSource source) {
        return source instanceof StrikeSource s ? s.hand : InteractionHand.MAIN_HAND;
    }

    /** The hand of {@code attacker} holding {@code weapon}: off hand only if it is that stack and not the main-hand one. */
    public static InteractionHand hand(LivingEntity attacker, ItemStack weapon) {
        return weapon == attacker.getOffhandItem() && weapon != attacker.getMainHandItem()
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** The stack of {@code attacker} that dealt the blow {@code source}. */
    public static ItemStack weapon(LivingEntity attacker, DamageSource source) {
        return attacker.getItemInHand(hand(source));
    }

    /** The slot to report wear against, for {@code hurtAndBreak}. */
    public static EquipmentSlot slot(LivingEntity attacker, ItemStack weapon) {
        return hand(attacker, weapon) == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}
