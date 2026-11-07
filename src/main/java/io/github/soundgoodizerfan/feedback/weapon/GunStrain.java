// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.modular.attunement.ImpulseKinds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What a hand can hold against a gun. The view only turns so far, so kick past what it can take is thrown into the arms instead:
 * every shot adds its kick in degrees to the shooter's strain, the strain drains while they are not firing, and past
 * {@link FTuning#GUN_STRAIN_LIMIT_DEG} the gun leaves the hand. Past twice that the body takes it as well. A gun with an
 * impulse grip locks in the hand and lets the shooter be thrown instead.
 */
public final class GunStrain {

    private static final Map<UUID, float[]> STRAIN = new HashMap<>(); // {strain, last tick}

    private GunStrain() {
    }

    /** Strain after one more shot of {@code kickDeg} at {@code tick}: what is left of the last, drained, plus this. */
    private static float add(float strain, long ticks, float kickDeg) {
        return Math.max(0f, strain - FTuning.GUN_STRAIN_DRAIN_DEG_PER_S * ticks / 20f) + kickDeg;
    }

    /** The shots of a steady burst it takes to throw the gun: 1 for one hard shot, 0 if the hand holds it however long it fires. */
    public static int shotsToThrow(float kickDeg, int cycleMs) {
        long ticks = Math.max(1, cycleMs / 50);
        float strain = 0f;
        for (int shots = 1; shots <= 60; shots++) {
            strain = add(strain, shots == 1 ? 1000 : ticks, kickDeg);
            if (strain > FTuning.GUN_STRAIN_LIMIT_DEG)
                return shots;
        }
        return 0;
    }

    /** One more shot from {@code shooter}'s {@code hand}; throws the gun or hurts them when it is too much. */
    public static void shot(ServerPlayer shooter, ItemStack gun, InteractionHand hand, float kickDeg, boolean aimed) {
        float kick = kickDeg * (aimed ? FTuning.GUN_ADS_KICK : 1f);
        long now = shooter.level().getGameTime();
        float[] s = STRAIN.computeIfAbsent(shooter.getUUID(), k -> new float[] {0f, now});
        s[0] = add(s[0], now - (long) s[1], kick);
        s[1] = now;
        float limit = FTuning.GUN_STRAIN_LIMIT_DEG;
        if (s[0] <= limit)
            return;
        float over = (s[0] - limit) / limit;
        s[0] = 0f;
        io.github.soundgoodizerfan.feedback.core.Awards.give(shooter, "combat/newton_was_right");
        if (ImpulseKinds.recoilLevel(gun) > 0) {
            // The grip holds; the shooter goes instead, back along the line of the shot.
            Vec3 back = shooter.getLookAngle().scale(-Math.min(over, 2f) * FTuning.GUN_STRAIN_THROW_SPEED);
            shooter.setDeltaMovement(shooter.getDeltaMovement().add(back));
            shooter.hurtMarked = true;
        } else {
            shooter.setItemInHand(hand, ItemStack.EMPTY);
            shooter.drop(gun, false);
        }
        if (over > 1f)
            shooter.hurt(Hazards.source(shooter.level(), Hazards.GUN_RECOIL),
                    Math.min((over - 1f) * FTuning.GUN_STRAIN_DAMAGE, FTuning.GUN_STRAIN_DAMAGE_MAX));
    }
}
