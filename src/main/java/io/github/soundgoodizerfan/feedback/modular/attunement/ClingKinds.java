// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import static io.github.soundgoodizerfan.feedback.modular.attunement.AttunementKinds.onStrike;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Cling effect (friction), every figure from the
 * part material's friction. Weapon: {@code stick}, a landed blow slows the target, longer for a grippier material;
 * {@code slick} (the mirror), the target is knocked further, more for a slipperier one. Boots: {@code worn_grip}
 * brakes a slide on slippery ground when the wearer is not pushing; {@code worn_slick} lets a wearer skate faster
 * on any ground and brakes poorly. Nothing wears. Not built: keeping speed uphill (grip).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ClingKinds {

    /** Friction assumed for a material that states none. [OPEN] until played */
    private static final float DEFAULT_FRICTION = 0.5f;
    /** Slow: ticks per friction per level, cap, and the Slowness amplifier. [OPEN] until played */
    private static final int SLOW_TICKS = 60, SLOW_CAP = 200, SLOW_AMP = 1;
    /** Slick: knockback per level, times (SLICK_REF - friction), floored at zero. [OPEN] until played */
    private static final float SLICK_KNOCKBACK = 0.6f, SLICK_REF = 1.5f;
    /** Ground friction above which a block is slippery (ice is 0.98, stone 0.6). */
    private static final float SLIPPERY = 0.7f;
    /** Grip: share of a slide braked per update, per friction per level, cap. [OPEN] until played */
    private static final float BRAKE = 0.15f, BRAKE_CAP = 0.6f;
    /** Slick soles: speed gain per update per level, horizontal speed it stops at, and the coast gain when not pushing. [OPEN] until played */
    private static final double SKATE_GAIN = 0.05, SKATE_CAP = 0.45, COAST_GAIN = 1.3;
    /** Ticks between boot updates. */
    private static final int PERIOD = 2;

    private ClingKinds() {
    }

    private static float friction(io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.Attunement a) {
        return a.material().mechanical().friction().orElse(DEFAULT_FRICTION);
    }

    public static void register() {
        onStrike("stick", (event, a) -> event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                Math.min(SLOW_CAP, (int) (SLOW_TICKS * friction(a) * a.level())), SLOW_AMP)));
        onStrike("slick", (event, a) -> {
            float kb = SLICK_KNOCKBACK * a.level() * Math.max(0f, SLICK_REF - friction(a));
            var attacker = event.getSource().getEntity();
            LivingEntity victim = event.getEntity();
            if (kb > 0 && attacker != null)
                victim.knockback(kb, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
        });
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % PERIOD != 0 || !p.onGround() || p.getItemBySlot(EquipmentSlot.FEET).isEmpty())
            return;
        boolean pushing = p.zza != 0 || p.xxa != 0;
        var d = p.getDeltaMovement();
        double speed = d.horizontalDistance();
        for (var a : io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.attunements(p.getItemBySlot(EquipmentSlot.FEET))) {
            double k = 1;
            if (a.kind().equals("worn_grip") && !pushing && speed > 0.02
                    && p.level().getBlockState(p.getBlockPosBelowThatAffectsMyMovement()).getFriction(p.level(), p.getBlockPosBelowThatAffectsMyMovement(), p) > SLIPPERY)
                k = 1 - Math.min(BRAKE_CAP, BRAKE * friction(a) * a.level());
            else if (a.kind().equals("worn_slick"))
                k = pushing ? (speed < SKATE_CAP ? 1 + SKATE_GAIN * a.level() : 1) : (speed > 0.02 ? COAST_GAIN : 1);
            if (k != 1) {
                p.setDeltaMovement(d.x * k, d.y, d.z * k);
                p.hurtMarked = true;
                return;
            }
        }
    }
}
