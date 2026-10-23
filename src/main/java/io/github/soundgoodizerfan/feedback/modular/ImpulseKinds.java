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

import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The runtime kinds of the Impulse effect ({@code docs/feedback_enchant_roster.md}). Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ImpulseKinds {

    /** Knockback strength per level, on a body of {@link #REFERENCE_MASS}. [OPEN] until played. */
    private static final float KNOCKBACK_PER_LEVEL = 0.5f;
    /** Mass (Mu) at and below which a target is thrown at full strength; heavier ones scale down. [OPEN] until played. */
    private static final float REFERENCE_MASS = 80f;
    /** Strength of the kick a recoil part gives the wielder, per level. [OPEN] until played. */
    private static final float RECOIL_PER_LEVEL = 0.4f;
    /** Speed a kickback stock throws the shooter back with, per level, and the target forward with. [OPEN] until played */
    private static final float KICKBACK_PER_LEVEL = 0.6f;
    private static final String KICK_TAG = "feedback_kickback";
    /** Share of the damage taken a reflecting piece returns, per level. [OPEN] until played. */
    private static final float REFLECT_PER_LEVEL = 0.1f;
    /** Share of a blast or fall one level of damping soaks up. [OPEN] until played. */
    private static final float DAMP_PER_LEVEL = 0.1f;
    /** Most the whole worn set can damp. [OPEN] until played. */
    private static final float DAMP_CAP = 0.7f;

    private ImpulseKinds() {
    }

    static void register() {
        onStrike("shove", (event, a) -> {
            LivingEntity target = event.getEntity();
            Entity attacker = event.getSource().getEntity();
            if (attacker == null)
                return;
            float scale = Math.min(1f, REFERENCE_MASS / Math.max(1f, BodyMass.of(target)));
            target.knockback(a.level() * KNOCKBACK_PER_LEVEL * scale,
                    attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        });
        // Impulse-: the blow draws the target in, as shove drives it off.
        onStrike("pull", (event, a) -> {
            LivingEntity target = event.getEntity();
            Entity attacker = event.getSource().getEntity();
            if (attacker == null)
                return;
            float scale = Math.min(1f, REFERENCE_MASS / Math.max(1f, BodyMass.of(target)));
            target.knockback(a.level() * KNOCKBACK_PER_LEVEL * scale,
                    target.getX() - attacker.getX(), target.getZ() - attacker.getZ());
        });
        onStrike("recoil", (event, a) -> {
            if (event.getSource().getEntity() instanceof LivingEntity attacker)
                attacker.knockback(a.level() * RECOIL_PER_LEVEL,
                        event.getEntity().getX() - attacker.getX(), event.getEntity().getZ() - attacker.getZ());
        });
    }

    /**
     * The force of nature: a crossbow's recoiling stock (its mirror, drag, is the same coupling drawn together: the shooter is pulled after the shot and the target back toward them) throws the shooter back along the line of the shot as it
     * leaves, and the target it hits is thrown the other way, so shooter and target part. Aiming down is a launch.
     * shortcut: the arrow is marked so only the one shot is handled, but an arrow loaded from a save with no mark
     * and a shooter now holding such a stock would be kicked once more; mark arrows with a spawn tick if it matters.
     */
    @SubscribeEvent
    public static void onFire(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof Player shooter) || arrow.getPersistentData().contains(KICK_TAG))
            return;
        int level = kick(shooter.getMainHandItem()) + kick(shooter.getOffhandItem());
        if (level == 0)
            return;
        shooter.setDeltaMovement(shooter.getDeltaMovement().add(arrow.getDeltaMovement().normalize().scale(-KICKBACK_PER_LEVEL * level)));
        shooter.hurtMarked = true;
        arrow.getPersistentData().putInt(KICK_TAG, level);
    }

    /** A stock's net kick: kickback throws shooter and target apart (positive), drag draws them together (negative). */
    private static int kick(ItemStack stack) {
        return TideKinds.level(stack, "kickback") - TideKinds.level(stack, "drag");
    }

    /** The target of a kicked shot is thrown along the arrow's flight, in proportion to the kick, lighter bodies further. */
    @SubscribeEvent
    public static void onKickHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !arrow.getPersistentData().contains(KICK_TAG))
            return;
        LivingEntity target = event.getEntity();
        float push = arrow.getPersistentData().getInt(KICK_TAG) * KICKBACK_PER_LEVEL * Math.min(1f, REFERENCE_MASS / Math.max(1f, BodyMass.of(target)));
        target.setDeltaMovement(target.getDeltaMovement().add(arrow.getDeltaMovement().normalize().scale(push)));
        target.hurtMarked = true;
    }

    /** Thorns: the wearer's plate returns a share of the blow; a returned blow is never itself returned. */
    @SubscribeEvent
    public static void onReflect(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.THORNS) || !(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;
        LivingEntity wearer = event.getEntity();
        float share = 0f;
        for (ItemStack worn : wearer.getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("reflect"))
                    share += a.level() * REFLECT_PER_LEVEL;
        if (share > 0f)
            attacker.hurt(wearer.damageSources().thorns(wearer), event.getAmount() * share);
    }

    @SubscribeEvent
    public static void onDamp(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_EXPLOSION) && !event.getSource().is(DamageTypeTags.IS_FALL))
            return;
        float share = 0f;
        for (ItemStack worn : event.getEntity().getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                if (a.kind().equals("damp"))
                    share += a.level() * DAMP_PER_LEVEL;
        if (share > 0f)
            event.setAmount(event.getAmount() * (1f - Math.min(DAMP_CAP, share)));
    }
}
