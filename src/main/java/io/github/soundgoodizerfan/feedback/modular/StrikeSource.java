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
