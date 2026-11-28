// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * A perk that does something to a creature a part of its material strikes: the same effect is the held weapon's blow, the plate's answer
 * to whoever strikes its wearer ({@code striker} is then the wearer), and a machine head's strike ({@code striker} is {@code null}).
 * An effect that needs a striker (a pull, a reading) does nothing for a machine.
 */
public final class StrikePerk implements ModularTrait {

    /** What the perk does to {@code target}; {@code striker} is whoever carries the part, or null for a machine. */
    @FunctionalInterface
    public interface Effect {
        void hit(ServerLevel level, LivingEntity target, LivingEntity striker);
    }

    private final Effect effect;

    public StrikePerk(Effect effect) {
        this.effect = effect;
    }

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (attacker.level() instanceof ServerLevel level)
            effect.hit(level, event.getEntity(), attacker);
    }

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        // Only a blow struck in person: a plate does not reach out along an arrow's path.
        if (event.getSource().getEntity() instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
                && attacker != wearer && wearer.level() instanceof ServerLevel level)
            effect.hit(level, attacker, wearer);
    }

    @Override
    public void onMachineHit(ServerLevel level, LivingEntity target, Params params) {
        effect.hit(level, target, null);
    }
}
